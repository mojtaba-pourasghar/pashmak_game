package ir.brandimo.pashmak.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Bitmaps live on disk; the database only ever stores their paths. */
public final class BitmapIO {

    private static final String TAG = "BitmapIO";

    public static final String DIR_ITEMS = "items";
    public static final String DIR_SCENES = "scenes";

    private BitmapIO() {
    }

    public static File dir(Context context, String name) {
        File dir = new File(context.getFilesDir(), name);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "could not create " + dir);
        }
        return dir;
    }

    public static File newFile(Context context, String dirName, String prefix) {
        return new File(dir(context, dirName), prefix + "_" + System.currentTimeMillis() + ".png");
    }

    /** Writes a PNG and returns its absolute path, or null when the write fails. */
    public static String writePng(Bitmap bitmap, File target) {
        if (bitmap == null || target == null) {
            return null;
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(target);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            return target.getAbsolutePath();
        } catch (IOException e) {
            Log.w(TAG, "png write failed", e);
            return null;
        } finally {
            close(out);
        }
    }

    public static Bitmap read(String path) {
        if (path == null) {
            return null;
        }
        try {
            return BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            Log.w(TAG, "decode failed: " + path, e);
            return null;
        }
    }

    /** Decodes downsampled, so thumbnails never pull a full-size bitmap into memory. */
    public static Bitmap readSampled(String path, int maxEdge) {
        if (path == null) {
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            int sample = 1;
            while (maxEdge > 0 && longest / sample > maxEdge) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            return BitmapFactory.decodeFile(path, opts);
        } catch (Exception e) {
            Log.w(TAG, "sampled decode failed: " + path, e);
            return null;
        }
    }

    public static void delete(String path) {
        if (path == null) {
            return;
        }
        File file = new File(path);
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "could not delete " + path);
        }
    }

    private static void close(FileOutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
    }
}
