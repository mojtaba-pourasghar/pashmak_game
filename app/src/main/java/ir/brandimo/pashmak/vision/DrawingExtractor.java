package ir.brandimo.pashmak.vision;

import android.graphics.Bitmap;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Lifts a child's marker drawing off the paper it was drawn on.
 *
 * The photo is taken on a desk under whatever light the room happens to have, so
 * a single global threshold is useless — one corner of the page is always darker
 * than the other. Instead every pixel is compared against the average of its own
 * neighbourhood (Bradley-Roth), which is invariant to a smooth lighting gradient,
 * with a second absolute-contrast test that stops blank paper texture from being
 * amplified into speckle.
 *
 * Colour is judged on an "ink" channel of 255 - min(R,G,B) rather than luma, so a
 * yellow or light-green marker survives as strongly as a black one.
 *
 * Pure Java on int arrays: no OpenCV, no RenderScript, nothing to ship but code.
 */
public final class DrawingExtractor {

    /** Work resolution. Keeps the integral image inside int range and the pass fast. */
    private static final int MAX_EDGE = 1200;
    /** Neighbourhood radius as a fraction of the shorter side. */
    private static final float WINDOW_FRACTION = 0.125f;
    /** Bradley-Roth relative margin. */
    private static final float RELATIVE_MARGIN = 0.15f;
    /** Minimum absolute lift above the local mean, out of 255. */
    private static final int ABSOLUTE_MARGIN = 18;
    /** Components smaller than this share of the region are noise. */
    private static final float MIN_AREA_FRACTION = 0.0008f;
    /** A component this wide or tall that is mostly empty is the paper edge. */
    private static final float EDGE_SPAN_FRACTION = 0.92f;
    private static final float EDGE_FILL_RATIO = 0.15f;
    /** Alpha ramp width above the threshold, for anti-aliased strokes. */
    private static final float ALPHA_RAMP = 40f;
    private static final int CROP_PADDING = 8;

    public interface ProgressListener {
        void onProgress(int percent);
    }

    private DrawingExtractor() {
    }

    /**
     * @param source  the captured frame, already rotated upright
     * @param roi     region of interest in normalised 0..1 coordinates
     */
    @NonNull
    public static ExtractionResult extract(@Nullable Bitmap source, @Nullable RectF roi,
                                           @Nullable ProgressListener listener) {
        if (source == null || source.isRecycled()) {
            return ExtractionResult.empty();
        }
        Bitmap region = cropAndScale(source, roi);
        if (region == null) {
            return ExtractionResult.empty();
        }
        report(listener, 10);

        final int w = region.getWidth();
        final int h = region.getHeight();
        final int n = w * h;
        int[] pixels = new int[n];
        region.getPixels(pixels, 0, w, 0, 0, w, h);
        if (region != source) {
            region.recycle();
        }

        int[] ink = inkChannel(pixels);
        report(listener, 25);

        int[] integral = integralImage(ink, w, h);
        report(listener, 40);

        byte[] mask = threshold(ink, integral, w, h);
        report(listener, 55);

        mask = morphClose(mask, w, h);
        mask = morphOpen(mask, w, h);
        report(listener, 70);

        boolean[] keep = selectComponents(mask, w, h);
        report(listener, 85);

        ExtractionResult result = compose(pixels, ink, integral, keep, w, h);
        report(listener, 100);
        return result;
    }

    // ------------------------------------------------------------------ stages

    @Nullable
    private static Bitmap cropAndScale(Bitmap source, @Nullable RectF roi) {
        int sw = source.getWidth();
        int sh = source.getHeight();
        int left = 0;
        int top = 0;
        int width = sw;
        int height = sh;
        if (roi != null && roi.width() > 0f && roi.height() > 0f) {
            left = clamp(Math.round(roi.left * sw), 0, sw - 1);
            top = clamp(Math.round(roi.top * sh), 0, sh - 1);
            width = clamp(Math.round(roi.width() * sw), 1, sw - left);
            height = clamp(Math.round(roi.height() * sh), 1, sh - top);
        }
        if (width < 16 || height < 16) {
            return null;
        }
        Bitmap cropped;
        try {
            cropped = Bitmap.createBitmap(source, left, top, width, height);
        } catch (Exception e) {
            return null;
        }
        int longest = Math.max(width, height);
        if (longest <= MAX_EDGE) {
            return cropped;
        }
        float scale = MAX_EDGE / (float) longest;
        int targetW = Math.max(1, Math.round(width * scale));
        int targetH = Math.max(1, Math.round(height * scale));
        try {
            Bitmap scaled = Bitmap.createScaledBitmap(cropped, targetW, targetH, true);
            if (scaled != cropped) {
                cropped.recycle();
            }
            return scaled;
        } catch (Exception e) {
            return cropped;
        }
    }

    /** 255 - min(R,G,B): high for any coloured mark, near zero for white paper. */
    private static int[] inkChannel(int[] pixels) {
        int[] ink = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            int min = r < g ? (r < b ? r : b) : (g < b ? g : b);
            ink[i] = 255 - min;
        }
        return ink;
    }

    private static int[] integralImage(int[] ink, int w, int h) {
        int stride = w + 1;
        int[] integral = new int[stride * (h + 1)];
        for (int y = 0; y < h; y++) {
            int rowSum = 0;
            int rowBase = y * w;
            int outBase = (y + 1) * stride;
            int prevBase = y * stride;
            for (int x = 0; x < w; x++) {
                rowSum += ink[rowBase + x];
                integral[outBase + x + 1] = integral[prevBase + x + 1] + rowSum;
            }
        }
        return integral;
    }

    private static int boxSum(int[] integral, int stride, int x0, int y0, int x1, int y1) {
        return integral[(y1 + 1) * stride + (x1 + 1)]
                - integral[y0 * stride + (x1 + 1)]
                - integral[(y1 + 1) * stride + x0]
                + integral[y0 * stride + x0];
    }

    private static byte[] threshold(int[] ink, int[] integral, int w, int h) {
        int stride = w + 1;
        int radius = Math.max(8, Math.round(Math.min(w, h) * WINDOW_FRACTION));
        byte[] mask = new byte[w * h];
        for (int y = 0; y < h; y++) {
            int y0 = Math.max(0, y - radius);
            int y1 = Math.min(h - 1, y + radius);
            for (int x = 0; x < w; x++) {
                int x0 = Math.max(0, x - radius);
                int x1 = Math.min(w - 1, x + radius);
                int count = (x1 - x0 + 1) * (y1 - y0 + 1);
                int sum = boxSum(integral, stride, x0, y0, x1, y1);
                int value = ink[y * w + x];
                float mean = sum / (float) count;
                boolean relative = value > mean * (1f + RELATIVE_MARGIN);
                boolean absolute = value - mean >= ABSOLUTE_MARGIN;
                mask[y * w + x] = (byte) ((relative && absolute) ? 1 : 0);
            }
        }
        return mask;
    }

    /** Dilate then erode: bridges the gaps a dry marker leaves in a stroke. */
    private static byte[] morphClose(byte[] mask, int w, int h) {
        return erode(dilate(mask, w, h), w, h);
    }

    /** Erode then dilate: removes lone speckles without thinning real strokes. */
    private static byte[] morphOpen(byte[] mask, int w, int h) {
        return dilate(erode(mask, w, h), w, h);
    }

    private static byte[] dilate(byte[] mask, int w, int h) {
        byte[] out = new byte[mask.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                byte value = 0;
                for (int dy = -1; dy <= 1 && value == 0; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= h) {
                        continue;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= w) {
                            continue;
                        }
                        if (mask[ny * w + nx] != 0) {
                            value = 1;
                            break;
                        }
                    }
                }
                out[y * w + x] = value;
            }
        }
        return out;
    }

    private static byte[] erode(byte[] mask, int w, int h) {
        byte[] out = new byte[mask.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                byte value = 1;
                for (int dy = -1; dy <= 1 && value == 1; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= h) {
                        value = 0;
                        break;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= w || mask[ny * w + nx] == 0) {
                            value = 0;
                            break;
                        }
                    }
                }
                out[y * w + x] = value;
            }
        }
        return out;
    }

    /**
     * Labels connected ink and keeps only the plausible drawing parts. The flood
     * fill is iterative on purpose: a recursive one overflows the stack on a
     * full-page scribble.
     */
    private static boolean[] selectComponents(byte[] mask, int w, int h) {
        int n = w * h;
        int[] labels = new int[n];
        int[] stack = new int[n];
        boolean[] keepPixel = new boolean[n];
        int minArea = Math.max(12, Math.round(n * MIN_AREA_FRACTION));
        int edgeSpanW = Math.round(w * EDGE_SPAN_FRACTION);
        int edgeSpanH = Math.round(h * EDGE_SPAN_FRACTION);

        int label = 0;
        for (int start = 0; start < n; start++) {
            if (mask[start] == 0 || labels[start] != 0) {
                continue;
            }
            label++;
            int top = 0;
            stack[top++] = start;
            labels[start] = label;

            int area = 0;
            int minX = w;
            int minY = h;
            int maxX = -1;
            int maxY = -1;
            int[] members = new int[16];
            int memberCount = 0;

            while (top > 0) {
                int index = stack[--top];
                int x = index % w;
                int y = index / w;
                area++;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
                if (memberCount == members.length) {
                    int[] grown = new int[members.length * 2];
                    System.arraycopy(members, 0, grown, 0, members.length);
                    members = grown;
                }
                members[memberCount++] = index;

                for (int dy = -1; dy <= 1; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= h) {
                        continue;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= w) {
                            continue;
                        }
                        int neighbour = ny * w + nx;
                        if (mask[neighbour] != 0 && labels[neighbour] == 0) {
                            labels[neighbour] = label;
                            stack[top++] = neighbour;
                        }
                    }
                }
            }
            int boxW = maxX - minX + 1;
            int boxH = maxY - minY + 1;
            float fillRatio = area / (float) Math.max(1, boxW * boxH);
            boolean tooSmall = area < minArea;
            boolean frameLike = (boxW >= edgeSpanW || boxH >= edgeSpanH)
                    && fillRatio < EDGE_FILL_RATIO;
            if (tooSmall || frameLike) {
                continue;
            }
            for (int i = 0; i < memberCount; i++) {
                keepPixel[members[i]] = true;
            }
        }
        return keepPixel;
    }

    private static ExtractionResult compose(int[] pixels, int[] ink, int[] integral,
                                            boolean[] keep, int w, int h) {
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        int kept = 0;
        for (int i = 0; i < keep.length; i++) {
            if (!keep[i]) {
                continue;
            }
            kept++;
            int x = i % w;
            int y = i / w;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        if (kept == 0 || maxX < minX || maxY < minY) {
            return ExtractionResult.empty();
        }

        minX = Math.max(0, minX - CROP_PADDING);
        minY = Math.max(0, minY - CROP_PADDING);
        maxX = Math.min(w - 1, maxX + CROP_PADDING);
        maxY = Math.min(h - 1, maxY + CROP_PADDING);

        int outW = maxX - minX + 1;
        int outH = maxY - minY + 1;
        int[] out = new int[outW * outH];
        int stride = w + 1;
        int radius = Math.max(8, Math.round(Math.min(w, h) * WINDOW_FRACTION));

        for (int y = minY; y <= maxY; y++) {
            int y0 = Math.max(0, y - radius);
            int y1 = Math.min(h - 1, y + radius);
            for (int x = minX; x <= maxX; x++) {
                int index = y * w + x;
                int outIndex = (y - minY) * outW + (x - minX);
                if (!keep[index]) {
                    out[outIndex] = 0;
                    continue;
                }
                int x0 = Math.max(0, x - radius);
                int x1 = Math.min(w - 1, x + radius);
                int count = (x1 - x0 + 1) * (y1 - y0 + 1);
                float mean = boxSum(integral, stride, x0, y0, x1, y1) / (float) count;
                // Soft alpha keeps stroke edges smooth instead of fax-like.
                float lift = ink[index] - mean - 8f;
                int alpha = clamp(Math.round(lift * 255f / ALPHA_RAMP), 0, 255);
                if (alpha == 0) {
                    out[outIndex] = 0;
                    continue;
                }
                out[outIndex] = (alpha << 24) | (vivid(pixels[index]) & 0x00FFFFFF);
            }
        }

        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(out, outW, outH, Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            return ExtractionResult.empty();
        }
        return ExtractionResult.ok(bitmap, kept / (float) (w * h));
    }

    /** Pushes saturation up and lifts very dark pencil, so faint work still reads. */
    private static int vivid(int pixel) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        int luma = (r * 77 + g * 151 + b * 28) >> 8;
        r = clamp(Math.round(luma + (r - luma) * 1.25f), 0, 255);
        g = clamp(Math.round(luma + (g - luma) * 1.25f), 0, 255);
        b = clamp(Math.round(luma + (b - luma) * 1.25f), 0, 255);
        int brightest = Math.max(r, Math.max(g, b));
        if (brightest < 60 && brightest > 0) {
            float boost = 60f / brightest;
            r = clamp(Math.round(r * boost), 0, 255);
            g = clamp(Math.round(g * boost), 0, 255);
            b = clamp(Math.round(b * boost), 0, 255);
        }
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static void report(@Nullable ProgressListener listener, int percent) {
        if (listener != null) {
            listener.onProgress(percent);
        }
    }
}
