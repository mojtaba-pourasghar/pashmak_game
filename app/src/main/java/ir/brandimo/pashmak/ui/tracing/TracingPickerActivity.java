package ir.brandimo.pashmak.ui.tracing;

import android.content.Intent;

import androidx.annotation.ColorRes;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.TraceCatalog;
import ir.brandimo.pashmak.ui.common.Stage;
import ir.brandimo.pashmak.ui.common.StagePickerActivity;
import ir.brandimo.pashmak.util.FaNum;

/** One stage per glyph: every Persian letter, then the digits. */
public class TracingPickerActivity extends StagePickerActivity {

    /** Letters occupy the low indices, digits follow. */
    static final int DIGIT_OFFSET = 1000;

    @Override
    protected String screenTitle() {
        return getString(R.string.game_trace_title);
    }

    @Override
    protected String screenHint() {
        return getString(R.string.stage_pick_hint);
    }

    @Override
    @ColorRes
    protected int accentColor() {
        return R.color.green;
    }

    @Override
    protected List<Stage> buildStages() {
        List<Stage> stages = new ArrayList<>();
        String[] letters = TraceCatalog.letters(this);
        for (int i = 0; i < letters.length; i++) {
            stages.add(new Stage(i,
                    getString(R.string.stage_letter, letters[i]),
                    getString(R.string.stage_letters_group), "",
                    prefs.isTraceDone(letters[i]), false, 0, letters[i]));
        }
        String[] digits = TraceCatalog.digits(this);
        for (int i = 0; i < digits.length; i++) {
            stages.add(new Stage(DIGIT_OFFSET + i,
                    getString(R.string.stage_digit, FaNum.of(i)),
                    getString(R.string.stage_digits_group), "",
                    prefs.isTraceDone(digits[i]), false, 0, digits[i]));
        }
        return stages;
    }

    @Override
    protected void onStageChosen(Stage stage) {
        Intent intent = new Intent(this, TracingActivity.class);
        intent.putExtra(TracingActivity.EXTRA_DIGITS, stage.index >= DIGIT_OFFSET);
        intent.putExtra(TracingActivity.EXTRA_INDEX, stage.index % DIGIT_OFFSET);
        startActivity(intent);
    }
}
