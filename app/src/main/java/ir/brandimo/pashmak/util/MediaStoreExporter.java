package ir.brandimo.pashmak.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Saves finished artwork into the device gallery under Pictures/PashmakGame.
 * API 29+ goes through MediaStore; older releases write the public directory
 * directly and ask the media scanner to index it.
 */
public final class MediaStoreExporter {

    private static final String TAG = "MediaStoreExporter";
    public static final String ALBUM = "PashmakGame";

    private MediaStoreExporter() {
    }

    public static Uri export(Context context, Bitmap bitmap, String displayName) {
        if (context == null || bitmap == null) {
            return null;
        }
        String fileName = displayName.endsWith(".png") ? displayName : displayName + ".png";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return exportViaMediaStore(context, bitmap, fileName);
        }
        return exportLegacy(context, bitmap, fileName);
    }

    private static Uri exportViaMediaStore(Context context, Bitmap bitmap, String fileName) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + ALBUM);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri uri = null;
        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return null;
            }
            OutputStream out = resolver.openOutputStream(uri);
            if (out == null) {
                resolver.delete(uri, null, null);
                return null;
            }
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } finally {
                out.close();
            }
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
            return uri;
        } catch (Exception e) {
            Log.w(TAG, "MediaStore export failed", e);
            if (uri != null) {
                try {
                    resolver.delete(uri, null, null);
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    private static Uri exportLegacy(Context context, Bitmap bitmap, String fileName) {
        try {
            File album = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    ALBUM);
            if (!album.exists() && !album.mkdirs()) {
                return null;
            }
            File target = new File(album, fileName);
            FileOutputStream out = new FileOutputStream(target);
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
            } finally {
                out.close();
            }
            MediaScannerConnection.scanFile(context,
                    new String[]{target.getAbsolutePath()}, new String[]{"image/png"}, null);
            return Uri.fromFile(target);
        } catch (Exception e) {
            Log.w(TAG, "legacy export failed", e);
            return null;
        }
    }
}
