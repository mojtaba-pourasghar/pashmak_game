package ir.brandimo.pashmak.ui.livedrawing;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ir.brandimo.pashmak.data.catalog.Mission;
import ir.brandimo.pashmak.data.catalog.MissionCatalog;
import ir.brandimo.pashmak.data.db.CapturedItem;
import ir.brandimo.pashmak.data.repo.AppExecutors;
import ir.brandimo.pashmak.data.repo.MissionRepository;
import ir.brandimo.pashmak.vision.DrawingExtractor;
import ir.brandimo.pashmak.vision.ExtractionResult;

/** Drives one mission session: brief, scan, extract, celebrate. */
public class LiveDrawingViewModel extends AndroidViewModel {

    public enum Step {
        BRIEF, CAMERA, PROCESSING, ALIVE
    }

    private final MissionRepository repository;
    private final MutableLiveData<Step> step = new MutableLiveData<>(Step.BRIEF);
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> extractionFailed = new MutableLiveData<>(false);

    private Mission mission;
    /**
     * How many items had been captured when the finished scene was last saved.
     *
     * <p>It lives here and not in the Activity because the Activity is rebuilt on a
     * rotation with its fields back at their defaults, and the completed mission it
     * re-renders would then be celebrated and written to the gallery all over again
     * — one duplicate entry per turn of the tablet.
     */
    private int sceneSavedForCount = -1;
    private LiveData<List<CapturedItem>> items;
    private boolean extracting;

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
            ExtractionResult result = DrawingExtractor.extract(frame, roi,
                    percent -> progress.postValue(percent));
            if (!frame.isRecycled()) {
                frame.recycle();
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
                        step.setValue(Step.ALIVE);
                        callback.onCaptured(label, count);
                    }));
        });
    }

    public interface ExtractionCallback {
        void onCaptured(String label, int capturedCount);
    }

    public void retakeSlot(int slotIndex) {
        if (mission != null) {
            repository.deleteSlot(mission.index, slotIndex, null);
        }
    }

    /** True the first time a finished mission is saved at this item count. */
    public boolean claimSceneSave(int capturedCount) {
        if (sceneSavedForCount == capturedCount) {
            return false;
        }
        sceneSavedForCount = capturedCount;
        return true;
    }

    public void clearMission() {
        if (mission != null) {
            repository.clearMission(mission.index, null);
        }
    }
}
