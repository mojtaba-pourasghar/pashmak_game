package ir.brandimo.pashmak.ui.bubbles;

import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.catalog.TraceCatalog;
import ir.brandimo.pashmak.data.prefs.GamePrefs;
import ir.brandimo.pashmak.databinding.ActivityBubblePopBinding;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.GameActivity;
import ir.brandimo.pashmak.util.FaNum;

/** Pop the bubbles, collect the stars. Difficulty sets how fast they climb. */
public class BubblePopActivity extends GameActivity {

    private static final int CHEER_EVERY = 4;

    private ActivityBubblePopBinding binding;
    private int score;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBubblePopBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bubbleBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        attachCompanion();

        binding.bubbleField.setLabels(bubbleLabels());
        binding.bubbleField.setSpeedScale(speedForDifficulty());
        binding.bubbleField.setOnBubblePopped(this::onPopped);
        renderScore();
    }

    private String[] bubbleLabels() {
        List<String> labels = new ArrayList<>();
        String[] letters = TraceCatalog.letters(this);
        for (int i = 0; i < letters.length && i < 12; i++) {
            labels.add(letters[i]);
        }
        String[] digits = TraceCatalog.digits(this);
        for (int i = 1; i < digits.length; i++) {
            labels.add(digits[i]);
        }
        return labels.toArray(new String[0]);
    }

    private float speedForDifficulty() {
        switch (prefs.difficulty()) {
            case GamePrefs.DIFFICULTY_EASY:
                return 0.7f;
            case GamePrefs.DIFFICULTY_HARD:
                return 1.5f;
            case GamePrefs.DIFFICULTY_MEDIUM:
            default:
                return 1f;
        }
    }

    private void onPopped(String label) {
        score++;
        sounds.play(AudioManifest.SFX_POP, 0.9f + (score % 5) * 0.05f);
        mascot.addStars(1);
        renderScore();
        if (score % CHEER_EVERY == 0) {
            mascot.say(getString(R.string.bubbles_pop_line), MascotState.CHEER,
                    MascotController.HOLD_MIN_MS, AudioManifest.VOICE_BUBBLE_POP);
        }
    }

    private void renderScore() {
        binding.bubbleScore.setText(FaNum.of(score));
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bubbleField.start();
    }

    @Override
    protected void onPause() {
        binding.bubbleField.stop();
        super.onPause();
    }
}
