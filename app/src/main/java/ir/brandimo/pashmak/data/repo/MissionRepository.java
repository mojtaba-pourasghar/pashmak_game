package ir.brandimo.pashmak.data.repo;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.brandimo.pashmak.data.db.AppDatabase;
import ir.brandimo.pashmak.data.db.CapturedItem;
import ir.brandimo.pashmak.data.db.CapturedItemDao;
import ir.brandimo.pashmak.data.db.MissionProgress;
import ir.brandimo.pashmak.util.BitmapIO;

/** Owns everything about a mission's captured drawings. */
public final class MissionRepository {

    private static volatile MissionRepository instance;

    private final Context appContext;
    private final CapturedItemDao dao;
    private final LiveData<Map<Integer, Integer>> progress;

    private MissionRepository(Context context) {
        appContext = context.getApplicationContext();
        dao = AppDatabase.get(appContext).capturedItems();
        progress = Transformations.map(dao.observeProgress(), rows -> {
            Map<Integer, Integer> map = new HashMap<>();
            if (rows != null) {
                for (int i = 0; i < rows.size(); i++) {
                    MissionProgress row = rows.get(i);
                    map.put(row.missionIndex, row.done);
                }
            }
            return map;
        });
    }

    public static MissionRepository get(@NonNull Context context) {
        MissionRepository local = instance;
        if (local == null) {
            synchronized (MissionRepository.class) {
                if (instance == null) {
                    instance = new MissionRepository(context);
                }
                local = instance;
            }
        }
        return local;
    }

    /** missionIndex to number of captured items, for the missions list. */
    public LiveData<Map<Integer, Integer>> progress() {
        return progress;
    }

    public LiveData<List<CapturedItem>> observeMission(int missionIndex) {
        return dao.observeForMission(missionIndex);
    }

    public interface SaveCallback {
        void onSaved(@Nullable CapturedItem item, int capturedCount);
    }

    /**
     * Writes the cut-out drawing to disk and records it against the mission slot.
     * Re-capturing an occupied slot replaces it, so a child can redo one item.
     */
    public void saveCapture(int missionIndex, int slotIndex, String label,
                            Bitmap cutout, @NonNull SaveCallback callback) {
        AppExecutors.disk().execute(() -> {
            File target = new File(BitmapIO.dir(appContext, BitmapIO.DIR_ITEMS),
                    "m" + missionIndex + "_s" + slotIndex + ".png");
            String path = BitmapIO.writePng(cutout, target);
            if (path == null) {
                AppExecutors.main(() -> callback.onSaved(null, dao.countForMission(missionIndex)));
                return;
            }
            CapturedItem item = new CapturedItem();
            item.missionIndex = missionIndex;
            item.slotIndex = slotIndex;
            item.itemLabel = label == null ? "" : label;
            item.pngPath = path;
            item.createdAt = System.currentTimeMillis();
            dao.insert(item);
            int count = dao.countForMission(missionIndex);
            AppExecutors.main(() -> callback.onSaved(item, count));
        });
    }

    public void deleteSlot(int missionIndex, int slotIndex, @Nullable Runnable done) {
        AppExecutors.disk().execute(() -> {
            List<CapturedItem> items = dao.forMission(missionIndex);
            for (int i = 0; i < items.size(); i++) {
                CapturedItem item = items.get(i);
                if (item.slotIndex == slotIndex) {
                    BitmapIO.delete(item.pngPath);
                }
            }
            dao.deleteSlot(missionIndex, slotIndex);
            if (done != null) {
                AppExecutors.main(done);
            }
        });
    }

    public void clearMission(int missionIndex, @Nullable Runnable done) {
        AppExecutors.disk().execute(() -> {
            List<CapturedItem> items = dao.forMission(missionIndex);
            for (int i = 0; i < items.size(); i++) {
                BitmapIO.delete(items.get(i).pngPath);
            }
            dao.clearMission(missionIndex);
            if (done != null) {
                AppExecutors.main(done);
            }
        });
    }

    public void countForMission(int missionIndex, @NonNull CountCallback callback) {
        AppExecutors.disk().execute(() -> {
            int count = dao.countForMission(missionIndex);
            AppExecutors.main(() -> callback.onCount(count));
        });
    }

    public interface CountCallback {
        void onCount(int count);
    }
}
