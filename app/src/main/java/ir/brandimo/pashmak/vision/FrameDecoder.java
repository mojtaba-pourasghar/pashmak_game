package ir.brandimo.pashmak.vision;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/** Turns a CameraX capture into an upright bitmap the extractor can work on. */
public final class FrameDecoder {

    private FrameDecoder() {
    }

    @Nullable
    public static Bitmap toUprightBitmap(@Nullable ImageProxy proxy) {
        if (proxy == null) {
            return null;
        }
        try {
            ByteBuffer buffer = proxy.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (decoded == null) {
                return null;
            }
            int rotation = proxy.getImageInfo().getRotationDegrees();
            if (rotation == 0) {
                return decoded;
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(
                    decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);
            if (rotated != decoded) {
                decoded.recycle();
            }
            return rotated;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }
}
