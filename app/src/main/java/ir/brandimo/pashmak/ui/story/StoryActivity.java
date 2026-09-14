package ir.brandimo.pashmak.ui.story;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.catalog.StoryCatalog;
import ir.brandimo.pashmak.databinding.ActivityStoryBinding;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.GameActivity;

/** A short story the child can poke at: every prop answers back. */
public class StoryActivity extends GameActivity {

    private ActivityStoryBinding binding;
    private int scene;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.storyHeader.headerTitle.setText(R.string.story_title);
        binding.storyHeader.headerTitle.setTextColor(
                ContextCompat.getColor(this, R.color.gold_ink));
        binding.storyHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        bindStars(binding.storyHeader.headerStarsValue);
        attachCompanion();

        binding.storyScene.setOnPropTapped(prop -> {
            sounds.play(AudioManifest.SFX_WHOOSH);
            mascot.say(prop.line, MascotState.TALK, MascotController.HOLD_MIN_MS);
        });
        binding.storyNext.setOnClickListener(v -> {
            tap();
            scene++;
            render();
        });

        render();
    }

    private void render() {
        binding.storyLine.setText(StoryCatalog.line(this, scene));
        binding.storyScene.setScene(StoryCatalog.scene(scene));
    }
}
