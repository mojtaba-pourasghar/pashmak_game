package ir.brandimo.pashmak.vision;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

/** What came back from a scan: the cut-out drawing, or why there wasn't one. */
public final class ExtractionResult {

    public final boolean success;
    @Nullable
    public final Bitmap cutout;
    /** Share of the region that turned out to be ink — a sanity signal for the UI. */
    public final float inkRatio;

    private ExtractionResult(boolean success, @Nullable Bitmap cutout, float inkRatio) {
        this.success = success;
        this.cutout = cutout;
        this.inkRatio = inkRatio;
    }

    static ExtractionResult ok(Bitmap cutout, float inkRatio) {
        return new ExtractionResult(true, cutout, inkRatio);
    }

    static ExtractionResult empty() {
        return new ExtractionResult(false, null, 0f);
    }
}
