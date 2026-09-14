package ir.brandimo.pashmak.ui.missions;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.databinding.ActivityMissionsBinding;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.ui.livedrawing.LiveDrawingActivity;

/** The 20 collections, with live progress read straight from the captured items. */
public class MissionsActivity extends BaseActivity {

    private ActivityMissionsBinding binding;
    private MissionsViewModel viewModel;
    private MissionAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(MissionsViewModel.class);

        String name = getString(R.string.mascot_name);
        binding.missionsHeader.headerTitle.setText(R.string.missions_title);
        binding.missionsHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        binding.missionsHint.setText(getString(R.string.missions_hint, name));
        bindStars(binding.missionsHeader.headerStarsValue);
        attachCompanion();

        adapter = new MissionAdapter(viewModel.missions(), this::startMission);
        binding.missionsList.setLayoutManager(new GridLayoutManager(
                this, getResources().getInteger(R.integer.missions_span)));
        binding.missionsList.setAdapter(adapter);

        viewModel.progress().observe(this, done ->
                adapter.setProgress(done, viewModel.currentMission(done)));
    }

    private void startMission(ir.brandimo.pashmak.data.catalog.Mission mission, int captured) {
        tap();
        mascot.say(getString(R.string.mission_start_line, mission.title),
                MascotState.TALK, MascotController.HOLD_DEFAULT_MS);
        Intent intent = new Intent(this, LiveDrawingActivity.class);
        intent.putExtra(LiveDrawingActivity.EXTRA_MISSION, mission.index);
        startActivity(intent);
    }
}
