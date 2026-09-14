package ir.brandimo.pashmak.vision;

import android.graphics.RectF;

/**
 * Translates the on-screen scanning frame into a rectangle of the captured image.
 *
 * This is the piece most likely to be subtly wrong, so it assumes one specific
 * arrangement and the camera screen is configured to match it: the preview uses
 * FIT_CENTER, so the whole frame is visible inside the view with letterboxing,
 * and preview and capture share an aspect ratio.
 */
public final class RoiMapper {

    private RoiMapper() {
    }

    /**
     * @param roiInView the frame rectangle in view pixels
     * @param viewW     preview view width in pixels
     * @param viewH     preview view height in pixels
     * @param imageW    captured image width, after rotation
     * @param imageH    captured image height, after rotation
     * @return the same region in 0..1 image coordinates, clamped to the image
     */
    public static RectF toNormalized(RectF roiInView, int viewW, int viewH,
                                     int imageW, int imageH) {
        if (roiInView == null || viewW <= 0 || viewH <= 0 || imageW <= 0 || imageH <= 0) {
            return new RectF(0f, 0f, 1f, 1f);
        }
        // Where the letterboxed image actually sits inside the view.
        float scale = Math.min(viewW / (float) imageW, viewH / (float) imageH);
        float shownW = imageW * scale;
        float shownH = imageH * scale;
        float offsetX = (viewW - shownW) / 2f;
        float offsetY = (viewH - shownH) / 2f;

        float left = (roiInView.left - offsetX) / shownW;
        float top = (roiInView.top - offsetY) / shownH;
        float right = (roiInView.right - offsetX) / shownW;
        float bottom = (roiInView.bottom - offsetY) / shownH;

        RectF out = new RectF(
                clamp(left), clamp(top), clamp(right), clamp(bottom));
        if (out.width() <= 0.02f || out.height() <= 0.02f) {
            return new RectF(0f, 0f, 1f, 1f);
        }
        return out;
    }

    private static float clamp(float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }
}
