package ir.brandimo.pashmak.ui.coloring;

import android.content.Intent;

import androidx.annotation.ColorRes;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.ColorPack;
import ir.brandimo.pashmak.data.catalog.ColoringCatalog;
import ir.brandimo.pashmak.data.catalog.ColoringPage;
import ir.brandimo.pashmak.ui.common.Stage;
import ir.brandimo.pashmak.ui.common.StagePickerActivity;
import ir.brandimo.pashmak.util.FaNum;

/** All sixty coloring pictures, named, grouped by pack, unlocked pack by pack. */
public class ColoringPickerActivity extends StagePickerActivity {

    @Override
    protected String screenTitle() {
        return getString(R.string.game_paint_title);
    }

    @Override
    protected String screenHint() {
        return getString(R.string.stage_pick_hint);
    }

    @Override
    @ColorRes
    protected int accentColor() {
        return R.color.purple;
    }

    @Override
    protected List<Stage> buildStages() {
        List<Stage> stages = new ArrayList<>();
        int number = 0;
        for (int packIndex = 0; packIndex < ColoringCatalog.packCount(); packIndex++) {
            ColorPack pack = ColoringCatalog.pack(packIndex);
            boolean packLocked = !isPackUnlocked(packIndex);
            for (int pageIndex = 0; pageIndex < pack.size(); pageIndex++) {
                ColoringPage page = pack.page(pageIndex);
                boolean done = prefs.isColoringDone(pack.id + "/" + pageIndex);
                number++;
                stages.add(new Stage(packIndex * 100 + pageIndex, page.name, pack.name,
                        "", done, packLocked, 0, FaNum.of(number)));
            }
        }
        return stages;
    }

    /** A pack opens once every picture in the one before it is finished. */
    private boolean isPackUnlocked(int index) {
        if (index == 0) {
            return true;
        }
        ColorPack previous = ColoringCatalog.pack(index - 1);
        return prefs.coloringDoneInPack(previous.id, previous.size()) >= previous.size();
    }

    @Override
    protected void onStageChosen(Stage stage) {
        Intent intent = new Intent(this, ColoringActivity.class);
        intent.putExtra(ColoringActivity.EXTRA_PACK, stage.index / 100);
        intent.putExtra(ColoringActivity.EXTRA_PAGE, stage.index % 100);
        startActivity(intent);
    }
}
