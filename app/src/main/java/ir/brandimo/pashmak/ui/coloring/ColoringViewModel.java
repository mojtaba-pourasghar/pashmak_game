package ir.brandimo.pashmak.ui.coloring;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

import ir.brandimo.pashmak.data.catalog.ColorRegion;
import ir.brandimo.pashmak.data.catalog.ColoringCatalog;
import ir.brandimo.pashmak.data.catalog.ColoringPage;
import ir.brandimo.pashmak.data.catalog.Palette;

public class ColoringViewModel extends ViewModel {

    private final Map<Integer, Map<String, Integer>> fillsByPage = new HashMap<>();
    private final MutableLiveData<Integer> pageIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> selectedColor = new MutableLiveData<>(Palette.RED);
    private final MutableLiveData<Map<String, Integer>> fills = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Boolean> pageComplete = new MutableLiveData<>(false);

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

    public ColoringPage page() {
        Integer index = pageIndex.getValue();
        return ColoringCatalog.get(index == null ? 0 : index);
    }

    public void selectPage(int index) {
        pageIndex.setValue(index);
        fills.setValue(fillsFor(index));
        pageComplete.setValue(isComplete(index));
    }

    public void selectColor(int color) {
        selectedColor.setValue(color);
    }

    /** Returns true when the tapped region got its correct color. */
    public boolean paint(ColorRegion region) {
        Integer index = pageIndex.getValue();
        int page = index == null ? 0 : index;
        Integer color = selectedColor.getValue();
        int chosen = color == null ? Palette.RED : color;

        Map<String, Integer> current = fillsFor(page);
        current.put(region.id, chosen);
        fills.setValue(new HashMap<>(current));
        pageComplete.setValue(isComplete(page));
        return region.targetColor == chosen;
    }

    public void clearPage() {
        Integer index = pageIndex.getValue();
        int page = index == null ? 0 : index;
        fillsByPage.put(page, new HashMap<>());
        fills.setValue(new HashMap<>());
        pageComplete.setValue(false);
    }

    public int correctCount() {
        Integer index = pageIndex.getValue();
        int page = index == null ? 0 : index;
        Map<String, Integer> current = fillsFor(page);
        ColoringPage coloringPage = ColoringCatalog.get(page);
        int correct = 0;
        for (int i = 0; i < coloringPage.regions.size(); i++) {
            ColorRegion region = coloringPage.regions.get(i);
            Integer filled = current.get(region.id);
            if (filled != null && filled == region.targetColor) {
                correct++;
            }
        }
        return correct;
    }

    private boolean isComplete(int page) {
        ColoringPage coloringPage = ColoringCatalog.get(page);
        Map<String, Integer> current = fillsFor(page);
        for (int i = 0; i < coloringPage.regions.size(); i++) {
            ColorRegion region = coloringPage.regions.get(i);
            Integer filled = current.get(region.id);
            if (filled == null || filled != region.targetColor) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Integer> fillsFor(int page) {
        Map<String, Integer> map = fillsByPage.get(page);
        if (map == null) {
            map = new HashMap<>();
            fillsByPage.put(page, map);
        }
        return map;
    }
}
