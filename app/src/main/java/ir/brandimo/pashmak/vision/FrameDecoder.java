package ir.brandimo.pashmak.vision;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/** Turns a CameraX capture into an upright bitmap the extractor can work on. */
public final class FrameDecoder {

    /**
     * Longest edge to decode to. The capture runs in MAXIMIZE_QUALITY mode, so on a
     * 12MP camera the full frame is 4000x3000 — 48MB as ARGB_8888, in one allocation,
     * which is enough to throw OutOfMemoryError on a modest tablet. The whole photo
     * would then be lost silently. The extractor works at 1200px anyway and only ever
     * sees the cropped scanning frame, so decoding beyond this buys nothing.
     */
    private static final int TARGET_EDGE = 2000;

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
            options.inSampleSize = sampleSize(bytes);

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

    /** Reads the JPEG header only, then picks the power of two that fits TARGET_EDGE. */
    private static int sampleSize(byte[] bytes) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        int sample = 1;
        while (longest / sample > TARGET_EDGE) {
            sample *= 2;
        }
        return sample;
    }
}
