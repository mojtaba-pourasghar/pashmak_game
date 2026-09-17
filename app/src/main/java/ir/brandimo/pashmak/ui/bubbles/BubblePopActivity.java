package ir.brandimo.pashmak.ui.bubbles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.catalog.BubbleRound;
import ir.brandimo.pashmak.data.catalog.BubbleRoundCatalog;
import ir.brandimo.pashmak.data.catalog.TraceCatalog;
import ir.brandimo.pashmak.data.prefs.GamePrefs;
import ir.brandimo.pashmak.databinding.ActivityBubblePopBinding;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.GameActivity;
import ir.brandimo.pashmak.util.FaNum;

/**
 * Pop the bubbles — but only the ones the round asks for. Each round names a rule
 * ("the letter چ", "any number") and popping something else is a shrug, not a
 * punishment: the bubble goes, Pashmak says try again, and nothing is lost.
 */
public class BubblePopActivity extends GameActivity {

    private ActivityBubblePopBinding binding;
    private final Set<String> digitSet = new HashSet<>();

    private static final String STATE_SEED = "round_seed";
    private static final String STATE_ROUND = "round_index";
    private static final String STATE_POPPED = "round_popped";
    private static final String STATE_SCORE = "score";

    private long seed;
    private List<BubbleRound> rounds;
    private int roundIndex;
    private int poppedThisRound;
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

        digitSet.addAll(Arrays.asList(TraceCatalog.digits(this)));
        binding.bubbleField.setLabels(bubbleLabels());
        binding.bubbleField.setSpeedScale(speedForDifficulty());
        binding.bubbleField.setOnBubblePopped(this::onPopped);

        seed = savedInstanceState == null
                ? new java.util.Random().nextLong()
                : savedInstanceState.getLong(STATE_SEED);
        rounds = BubbleRoundCatalog.session(this, seed);
        if (savedInstanceState == null) {
            startRound(0);
        } else {
            // Rotating is not a reason to lose a game. The rules come back from the
            // seed and the progress through them from the bundle.
            score = savedInstanceState.getInt(STATE_SCORE);
            startRound(savedInstanceState.getInt(STATE_ROUND));
            poppedThisRound = savedInstanceState.getInt(STATE_POPPED);
            renderBanner();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SEED, seed);
        outState.putInt(STATE_ROUND, roundIndex);
        outState.putInt(STATE_POPPED, poppedThisRound);
        outState.putInt(STATE_SCORE, score);
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

    private BubbleRound round() {
        return rounds.get(Math.max(0, Math.min(roundIndex, rounds.size() - 1)));
    }

    private void startRound(int index) {
        roundIndex = index % rounds.size();
        poppedThisRound = 0;
        BubbleRound round = round();
        // Salt the field with what we are asking for; a single letter among thirty
        // is a waiting game, not a looking game.
        boolean single = round.kind == BubbleRound.Kind.LETTER
                || round.kind == BubbleRound.Kind.DIGIT;
        binding.bubbleField.setFavoured(single ? round.target : null, 0.34f);
        binding.bubbleField.reset();
        renderBanner();
        mascot.say(BubbleRoundCatalog.prompt(this, round), MascotState.TALK,
                MascotController.HOLD_MIN_MS, AudioManifest.VOICE_BUBBLE_GOAL);
    }

    private void onPopped(String label, float x, float y) {
        BubbleRound round = round();
        boolean isDigit = digitSet.contains(label);
        if (!round.accepts(label, isDigit)) {
            sounds.play(AudioManifest.SFX_WRONG);
            mascot.say(getString(R.string.bubbles_not_that,
                            BubbleRoundCatalog.prompt(this, round)),
                    MascotState.ENCOURAGE, MascotController.HOLD_MIN_MS,
                    AudioManifest.VOICE_BUBBLE_WRONG);
            return;
        }

        score++;
        poppedThisRound++;
        sounds.play(AudioManifest.SFX_POP, 0.9f + (score % 5) * 0.05f);
        mascot.addStars(1);
        renderBanner();

        if (poppedThisRound >= round.goal) {
            finishRound();
        }
    }

    private void finishRound() {
        sounds.play(AudioManifest.SFX_FANFARE);
        mascot.say(getString(R.string.bubbles_round_done), MascotState.CHEER,
                MascotController.HOLD_CHEER_MS, AudioManifest.VOICE_BUBBLE_ROUND);
        binding.getRoot().postDelayed(() -> startRound(roundIndex + 1), 1600L);
    }

    private void renderBanner() {
        binding.bubbleScore.setText(FaNum.of(score));
        binding.bubbleBanner.setText(
                BubbleRoundCatalog.banner(this, round(), poppedThisRound));
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
