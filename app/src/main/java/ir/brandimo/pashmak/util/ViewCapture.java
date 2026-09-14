package ir.brandimo.pashmak.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import androidx.annotation.Nullable;

/** Renders a live view into a bitmap, for saving a finished scene to the gallery. */
public final class ViewCapture {

    private ViewCapture() {
    }

    @Nullable
    public static Bitmap of(@Nullable View view, int backgroundColor) {
        if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) {
            return null;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(
                    view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            if (backgroundColor != Color.TRANSPARENT) {
                canvas.drawColor(backgroundColor);
            }
            view.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError | Exception e) {
            return null;
        }
    }
}
