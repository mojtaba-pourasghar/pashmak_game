package com.pashmak.app.ui.livedrawing;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import com.pashmak.app.data.catalog.Mission;
import com.pashmak.app.data.catalog.MissionCatalog;
import com.pashmak.app.data.db.CapturedItem;
import com.pashmak.app.data.repo.AppExecutors;
import com.pashmak.app.data.repo.MissionRepository;
import com.pashmak.app.vision.DrawingExtractor;
import com.pashmak.app.vision.ExtractionResult;

/** Drives one mission session: brief, scan, extract, celebrate. */
public class LiveDrawingViewModel extends AndroidViewModel {

    /**
     * The room is the hub: every item card in it opens the camera for that item,
     * and a finished scan comes straight back to the room to land in its place.
     */
    public enum Step {
        BRIEF, CAMERA, PROCESSING
    }

    private final MissionRepository repository;
    private final MutableLiveData<Step> step = new MutableLiveData<>(Step.BRIEF);
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> extractionFailed = new MutableLiveData<>(false);

    private Mission mission;
    /**
     * How many items had been captured when the finished mission was last celebrated.
     *
     * <p>It lives here and not in the Activity because the Activity is rebuilt on a
     * rotation with its fields back at their defaults, and the completed mission it
     * re-renders would then be celebrated — stars and all — on every turn of the tablet.
     */
    private int celebratedForCount = -1;
    private LiveData<List<CapturedItem>> items;
    private boolean extracting;
    /**
     * The item card the child tapped, i.e. the slot the next photo is for. Kept
     * here so a rotation in the middle of a scan still files it under the right item.
     */
    private int targetSlot = -1;

    public LiveDrawingViewModel(@NonNull Application application) {
        super(application);
        repository = MissionRepository.get(application);
    }

    public void start(int missionIndex) {
        if (mission != null) {
            return;
        }
        mission = MissionCatalog.get(getApplication(), missionIndex);
        items = repository.observeMission(missionIndex);
    }

    public Mission mission() {
        return mission;
    }

    public LiveData<List<CapturedItem>> items() {
        return items;
    }

    public LiveData<Step> step() {
        return step;
    }

    public LiveData<Integer> progress() {
        return progress;
    }

    public LiveData<Boolean> extractionFailed() {
        return extractionFailed;
    }

    public void goTo(Step next) {
        step.setValue(next);
    }

    public void setTargetSlot(int slot) {
        targetSlot = slot;
    }

    /** The slot the camera is shooting for, or -1 when no card has been tapped yet. */
    public int targetSlot() {
        return targetSlot;
    }

    public int capturedCount(@Nullable List<CapturedItem> current) {
        return current == null ? 0 : current.size();
    }

    public String currentItem(@Nullable List<CapturedItem> current) {
        return mission == null ? "" : mission.currentItem(capturedCount(current));
    }

    public boolean isComplete(@Nullable List<CapturedItem> current) {
        return mission != null && mission.isComplete(capturedCount(current));
    }

    /**
     * Runs the real extraction off the main thread and reports honest progress —
     * the bar reflects the pipeline rather than a timer.
     */
    /** The photo never even decoded; show the retry panel rather than failing quietly. */
    public void reportCaptureFailed() {
        extracting = false;
        progress.setValue(0);
        step.setValue(Step.PROCESSING);
        extractionFailed.setValue(true);
    }

    public void processCapture(Bitmap frame, RectF roi, int slotIndex,
                               @NonNull ExtractionCallback callback) {
        if (extracting) {
            return;
        }
        extracting = true;
        extractionFailed.setValue(false);
        progress.setValue(0);
        step.setValue(Step.PROCESSING);

        AppExecutors.imaging().execute(() -> {
            ExtractionResult result;
            try {
                result = DrawingExtractor.extract(frame, roi,
                        percent -> progress.postValue(percent));
            } catch (RuntimeException | OutOfMemoryError trouble) {
                // The extractor works on a full-resolution photo and can run out
                // of heap on a cheap tablet, and arithmetic on an odd frame can
                // throw. Either way the flag has to come back down: it is what
                // stops two scans at once, and left raised it turns every later
                // scan into a silent no-op — the child presses the shutter, the
                // bar never moves, the retry button goes back to the camera and
                // the next capture is dropped on the floor. The screen is stuck
                // for the rest of the session and nothing in the log says why.
                AppExecutors.main(() -> {
                    extracting = false;
                    extractionFailed.setValue(true);
                });
                return;
            } finally {
                if (!frame.isRecycled()) {
                    frame.recycle();
                }
            }
            if (!result.success || result.cutout == null) {
                AppExecutors.main(() -> {
                    extracting = false;
                    extractionFailed.setValue(true);
                });
                return;
            }
            String label = mission == null ? "" : mission.itemAt(slotIndex);
            AppExecutors.main(() -> repository.saveCapture(
                    mission == null ? 0 : mission.index, slotIndex, label, result.cutout,
                    (item, count) -> {
                        extracting = false;
                        if (item == null) {
                            extractionFailed.setValue(true);
                            return;
                        }
                        // Back to the room, where the new drawing lands in its place
                        // (replacing the old one if this slot had been shot before).
                        step.setValue(Step.BRIEF);
                        callback.onCaptured(label, slotIndex, count);
                    }));
        });
    }

    public interface ExtractionCallback {
        void onCaptured(String label, int slotIndex, int capturedCount);
    }

    /** True the first time a finished mission is celebrated at this item count. */
    public boolean claimCelebration(int capturedCount) {
        if (celebratedForCount == capturedCount) {
            return false;
        }
        celebratedForCount = capturedCount;
        return true;
    }

    public void clearMission() {
        if (mission != null) {
            repository.clearMission(mission.index, null);
        }
    }
}
