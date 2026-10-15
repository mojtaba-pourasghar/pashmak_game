package com.pashmak.app.ui.tale;

import android.content.Intent;

import androidx.annotation.ColorRes;

import java.util.ArrayList;
import java.util.List;

import com.pashmak.app.R;
import com.pashmak.app.data.catalog.Tale;
import com.pashmak.app.data.catalog.TaleCatalog;
import com.pashmak.app.ui.common.Stage;
import com.pashmak.app.ui.common.StagePickerActivity;
import com.pashmak.app.util.FaNum;

/**
 * Forty told tales. Nothing is locked here — this is the section a tired child
 * reaches for, and having to earn your bedtime story would be a strange rule.
 */
public class TalePickerActivity extends StagePickerActivity {

    @Override
    protected String screenTitle() {
        return getString(R.string.game_tales_title);
    }

    @Override
    protected String screenHint() {
        return getString(R.string.tales_hint);
    }

    @Override
    @ColorRes
    protected int accentColor() {
        return R.color.purple;
    }

    @Override
    protected List<Stage> buildStages() {
        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < TaleCatalog.count(); i++) {
            Tale tale = TaleCatalog.tale(i);
            long minutes = Math.max(1L, Math.round(tale.durationMs() / 60_000d));
            stages.add(new Stage(i, tale.title,
                    getString(R.string.tale_minutes, FaNum.of(minutes)), "",
                    prefs.isTaleDone(tale.id), false, tale.badge, null));
        }
        return stages;
    }

    @Override
    protected void onStageChosen(Stage stage) {
        Intent intent = new Intent(this, TaleActivity.class);
        intent.putExtra(TaleActivity.EXTRA_TALE, stage.index);
        startActivity(intent);
    }
}
