package ir.brandimo.pashmak.ui.common;

import android.os.Bundle;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.databinding.ActivityStagePickerBinding;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.BaseActivity;

/**
 * Every game is entered the same way the missions are: a list of named stages the
 * child picks from, showing what is finished and what is still locked.
 */
public abstract class StagePickerActivity extends BaseActivity
        implements StageAdapter.OnStageClick {

    protected ActivityStagePickerBinding binding;
    private StageAdapter adapter;

    protected abstract String screenTitle();

    protected abstract String screenHint();

    @ColorRes
    protected abstract int accentColor();

    protected abstract List<Stage> buildStages();

    /** Called for an unlocked stage; a locked one is explained instead. */
    protected abstract void onStageChosen(Stage stage);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStagePickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.pickerHeader.headerTitle.setText(screenTitle());
        binding.pickerHeader.headerTitle.setTextColor(
                ContextCompat.getColor(this, accentColor()));
        binding.pickerHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        binding.pickerHint.setText(screenHint());
        bindStars(binding.pickerHeader.headerStarsValue);
        attachCompanion();

        adapter = new StageAdapter(accentColor(), this);
        binding.pickerList.setLayoutManager(new GridLayoutManager(
                this, getResources().getInteger(R.integer.missions_span)));
        binding.pickerList.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Progress may have changed while a stage was being played.
        adapter.submit(buildStages());
    }

    @Override
    public void onStageClick(Stage stage) {
        tap();
        if (stage.locked) {
            mascot.say(getString(R.string.stage_locked), MascotState.TALK,
                    MascotController.HOLD_MIN_MS);
            return;
        }
        onStageChosen(stage);
    }
}
