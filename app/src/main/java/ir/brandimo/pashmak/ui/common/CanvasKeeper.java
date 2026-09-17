package ir.brandimo.pashmak.ui.common;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

/**
 * Holds a drawing across a screen rotation.
 *
 * <p>{@link PaintCanvasView} paints into a bitmap the size of the view rather than
 * keeping a list of strokes, which makes it fast and makes an eraser trivial — but
 * it also means the drawing cannot travel in a Bundle: a full-screen ARGB_8888
 * bitmap is megabytes and the system would kill the transaction. A ViewModel is not
 * destroyed and rebuilt with the Activity, so the bitmap simply waits here while the
 * screen is torn down and put back the other way up.
 *
 * <p>A child who turns the tablet mid-drawing should find their drawing still there.
 */
public class CanvasKeeper extends ViewModel {

    @Nullable
    private Bitmap held;

    /** Takes ownership of a drawing. Passing null clears whatever was held. */
    public void hold(@Nullable Bitmap bitmap) {
        if (held != null && held != bitmap && !held.isRecycled()) {
            held.recycle();
        }
        held = bitmap;
    }

    /** Gives the drawing back, and lets go of it, so it is only ever handed over once. */
    @Nullable
    public Bitmap take() {
        Bitmap out = held;
        held = null;
        return out == null || out.isRecycled() ? null : out;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // The screen is finished with for good, not merely being rebuilt.
        if (held != null && !held.isRecycled()) {
            held.recycle();
        }
        held = null;
    }
}
