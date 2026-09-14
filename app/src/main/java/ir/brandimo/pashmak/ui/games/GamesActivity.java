package ir.brandimo.pashmak.ui.games;

import android.os.Bundle;

import androidx.annotation.Nullable;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.databinding.ActivityGamesBinding;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.ui.bubbles.BubblePopActivity;
import ir.brandimo.pashmak.ui.coloring.ColoringActivity;
import ir.brandimo.pashmak.ui.freedraw.FreeDrawActivity;
import ir.brandimo.pashmak.ui.memory.MemoryActivity;
import ir.brandimo.pashmak.ui.missions.MissionsActivity;
import ir.brandimo.pashmak.ui.parentgate.ParentGateDialog;
import ir.brandimo.pashmak.ui.story.StoryActivity;
import ir.brandimo.pashmak.ui.tracing.TracingActivity;

/** The games menu. Everything the child can play, one tap away. */
public class GamesActivity extends BaseActivity {

    private ActivityGamesBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGamesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.gamesHeader.headerTitle.setText(R.string.games_title);
        binding.gamesHeader.headerTitle.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, R.color.red));
        binding.gamesHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        bindStars(binding.gamesHeader.headerStarsValue);
        attachCompanion();

        binding.gamesLiveSub.setText(
                getString(R.string.game_live_sub, getString(R.string.mascot_name)));

        binding.gamesLive.setOnClickListener(v -> open(MissionsActivity.class));
        binding.gamesPaint.setOnClickListener(v -> open(ColoringActivity.class));
        binding.gamesFreedraw.setOnClickListener(v -> open(FreeDrawActivity.class));
        binding.gamesTrace.setOnClickListener(v -> open(TracingActivity.class));
        binding.gamesBubbles.setOnClickListener(v -> open(BubblePopActivity.class));
        binding.gamesMemory.setOnClickListener(v -> open(MemoryActivity.class));
        binding.gamesStory.setOnClickListener(v -> open(StoryActivity.class));
        binding.gamesSettings.setOnClickListener(v -> {
            tap();
            ParentGateDialog.show(this);
        });
    }
}
