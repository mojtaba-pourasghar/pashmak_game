package ir.brandimo.pashmak.ui.coloring;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ir.brandimo.pashmak.data.catalog.ColorPack;
import ir.brandimo.pashmak.data.catalog.ColorRegion;
import ir.brandimo.pashmak.data.catalog.ColoringCatalog;
import ir.brandimo.pashmak.data.catalog.ColoringPage;
import ir.brandimo.pashmak.data.catalog.Palette;

/** Tracks which page is open and what has been coloured on it. */
public class ColoringViewModel extends ViewModel {

    private final Map<String, Map<String, Integer>> fillsByPage = new HashMap<>();
    /** Pages whose stars have already been handed out. */
    private final Set<String> awarded = new HashSet<>();
    private final MutableLiveData<Integer> packIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> pageIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> selectedColor = new MutableLiveData<>(Palette.RED);
    private final MutableLiveData<Map<String, Integer>> fills =
            new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Boolean> pageComplete = new MutableLiveData<>(false);

    public LiveData<Integer> packIndex() {
        return packIndex;
    }

    public LiveData<Integer> pageIndex() {
        return pageIndex;
    }

    public LiveData<Integer> selectedColor() {
        return selectedColor;
    }

    public LiveData<Map<String, Integer>> fills() {
        return fills;
    }

    public LiveData<Boolean> pageComplete() {
        return pageComplete;
    }

    public int packPosition() {
        Integer value = packIndex.getValue();
        return value == null ? 0 : value;
    }

    public int pagePosition() {
        Integer value = pageIndex.getValue();
        return value == null ? 0 : value;
    }

    public ColorPack pack() {
        return ColoringCatalog.pack(packPosition());
    }

    public ColoringPage page() {
        return pack().page(pagePosition());
    }

    /** Stable key for the open page, also used for saved progress. */
    public String pageKey() {
        return pack().id + "/" + pagePosition();
    }

    public void selectPack(int index) {
        packIndex.setValue(index);
        selectPage(0);
    }

    public void selectPage(int index) {
        pageIndex.setValue(index);
        fills.setValue(new HashMap<>(fillsFor(pageKey())));
        pageComplete.setValue(isComplete());
    }

    public void selectColor(int color) {
        selectedColor.setValue(color);
    }

    /** Returns true when the tapped region got its correct colour. */
    public boolean paint(ColorRegion region) {
        Integer color = selectedColor.getValue();
        int chosen = color == null ? Palette.RED : color;
        Map<String, Integer> current = fillsFor(pageKey());
        current.put(region.id, chosen);
        fills.setValue(new HashMap<>(current));
        pageComplete.setValue(isComplete());
        return region.targetColor == chosen;
    }

    /**
     * True the first time a page is finished and false ever after.
     *
     * <p>This lives here rather than in the Activity because turning the screen
     * builds a new Activity, which re-observes the LiveData and is handed the
     * "page complete" it was already showing — so a flag held up there paid the
     * stars out again on every rotation. A ViewModel outlives that rebuild.
     */
    public boolean claimAward() {
        return awarded.add(pageKey());
    }

    public void clearPage() {
        // Clearing genuinely starts the page over, so it can be earned again.
        awarded.remove(pageKey());
        fillsByPage.put(pageKey(), new HashMap<>());
        fills.setValue(new HashMap<>());
        pageComplete.setValue(false);
    }

    public int correctCount() {
        Map<String, Integer> current = fillsFor(pageKey());
        ColoringPage page = page();
        int correct = 0;
        for (int i = 0; i < page.regions.size(); i++) {
            ColorRegion region = page.regions.get(i);
            Integer filled = current.get(region.id);
            if (filled != null && filled == region.targetColor) {
                correct++;
            }
        }
        return correct;
    }

    private boolean isComplete() {
        return correctCount() == page().size();
    }

    private Map<String, Integer> fillsFor(String key) {
        Map<String, Integer> map = fillsByPage.get(key);
        if (map == null) {
            map = new HashMap<>();
            fillsByPage.put(key, map);
        }
        return map;
    }
}
