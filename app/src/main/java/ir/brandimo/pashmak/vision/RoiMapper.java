package ir.brandimo.pashmak.vision;

import android.graphics.RectF;

/**
 * Translates the on-screen scanning frame into a rectangle of the captured image.
 *
 * <p>This is the piece most likely to be subtly wrong, and it was: it used to assume
 * the preview was letterboxed (FIT_CENTER) while the layout left PreviewView on its
 * FILL_CENTER default, so the frame mapped onto the wrong part of the photo, the
 * extractor analysed the wrong crop, and every capture failed. Rather than document
 * an assumption and hope the layout keeps it, the caller now passes the scale type
 * the PreviewView actually reports.
 */
public final class RoiMapper {

    /** How the preview fits the image into the view. */
    public enum Fit {
        /** Whole image visible, letterboxed — PreviewView FIT_* scale types. */
        LETTERBOX,
        /** Image fills the view and the overflow is cropped — the FILL_* types. */
        CROP
    }

    private RoiMapper() {
    }

    /**
     * @param roiInView the frame rectangle in view pixels
     * @param viewW     preview view width in pixels
     * @param viewH     preview view height in pixels
     * @param imageW    captured image width, after rotation
     * @param imageH    captured image height, after rotation
     * @param fit       how the preview is fitting that image into the view
     * @return the same region in 0..1 image coordinates, clamped to the image
     */
    public static RectF toNormalized(RectF roiInView, int viewW, int viewH,
                                     int imageW, int imageH, Fit fit) {
        if (roiInView == null || viewW <= 0 || viewH <= 0 || imageW <= 0 || imageH <= 0) {
            return new RectF(0f, 0f, 1f, 1f);
        }
        // Where the image actually sits inside the view. Letterboxing shrinks to the
        // tighter axis and leaves bars; cropping grows to the looser one and spills.
        float byWidth = viewW / (float) imageW;
        float byHeight = viewH / (float) imageH;
        float scale = fit == Fit.CROP
                ? Math.max(byWidth, byHeight) : Math.min(byWidth, byHeight);
        float shownW = imageW * scale;
        float shownH = imageH * scale;
        float offsetX = (viewW - shownW) / 2f;
        float offsetY = (viewH - shownH) / 2f;

        RectF out = new RectF(
                clamp((roiInView.left - offsetX) / shownW),
                clamp((roiInView.top - offsetY) / shownH),
                clamp((roiInView.right - offsetX) / shownW),
                clamp((roiInView.bottom - offsetY) / shownH));
        if (out.width() <= 0.02f || out.height() <= 0.02f) {
            return new RectF(0f, 0f, 1f, 1f);
        }
        return out;
    }

    private static float clamp(float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }
}
