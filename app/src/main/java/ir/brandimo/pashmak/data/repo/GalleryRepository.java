package ir.brandimo.pashmak.data.repo;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.io.File;
import java.util.List;

import ir.brandimo.pashmak.data.db.AppDatabase;
import ir.brandimo.pashmak.data.db.GalleryDao;
import ir.brandimo.pashmak.data.db.GalleryEntry;
import ir.brandimo.pashmak.util.BitmapIO;
import ir.brandimo.pashmak.util.MediaStoreExporter;

/** Saved artwork: mission scenes, free drawings and finished coloring pages. */
public final class GalleryRepository {

    private static volatile GalleryRepository instance;

    private final Context appContext;
    private final GalleryDao dao;

    private GalleryRepository(Context context) {
        appContext = context.getApplicationContext();
        dao = AppDatabase.get(appContext).gallery();
    }

    public static GalleryRepository get(@NonNull Context context) {
        GalleryRepository local = instance;
        if (local == null) {
            synchronized (GalleryRepository.class) {
                if (instance == null) {
                    instance = new GalleryRepository(context);
                }
                local = instance;
            }
        }
        return local;
    }

    public LiveData<List<GalleryEntry>> observeAll() {
        return dao.observeAll();
    }

    public LiveData<Integer> observeCount() {
        return dao.observeCount();
    }

    public interface SaveCallback {
        void onSaved(boolean success);
    }

    public void save(int kind, int missionIndex, String title, Bitmap bitmap,
                     @Nullable SaveCallback callback) {
        AppExecutors.disk().execute(() -> {
            File target = BitmapIO.newFile(appContext, BitmapIO.DIR_SCENES, "art");
            String path = BitmapIO.writePng(bitmap, target);
            boolean ok = path != null;
            if (ok) {
                GalleryEntry entry = new GalleryEntry();
                entry.kind = kind;
                entry.missionIndex = missionIndex;
                entry.title = title == null ? "" : title;
                entry.filePath = path;
                entry.createdAt = System.currentTimeMillis();
                dao.insert(entry);
            }
            if (callback != null) {
                AppExecutors.main(() -> callback.onSaved(ok));
            }
        });
    }

    public void delete(long id, @Nullable Runnable done) {
        AppExecutors.disk().execute(() -> {
            GalleryEntry entry = dao.byId(id);
            if (entry != null) {
                BitmapIO.delete(entry.filePath);
            }
            dao.deleteById(id);
            if (done != null) {
                AppExecutors.main(done);
            }
        });
    }

    public interface ExportCallback {
        void onExported(@Nullable Uri uri);
    }

    public void exportToDeviceGallery(long id, @NonNull ExportCallback callback) {
        AppExecutors.disk().execute(() -> {
            GalleryEntry entry = dao.byId(id);
            Uri uri = null;
            if (entry != null) {
                Bitmap bitmap = BitmapIO.read(entry.filePath);
                if (bitmap != null) {
                    uri = MediaStoreExporter.export(appContext, bitmap,
                            "pashmak_" + entry.createdAt);
                    bitmap.recycle();
                }
            }
            Uri result = uri;
            AppExecutors.main(() -> callback.onExported(result));
        });
    }
}
