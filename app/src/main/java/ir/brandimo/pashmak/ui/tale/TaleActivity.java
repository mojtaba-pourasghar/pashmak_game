package ir.brandimo.pashmak.ui.tale;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.catalog.Tale;
import ir.brandimo.pashmak.data.catalog.TaleCatalog;
import ir.brandimo.pashmak.databinding.ActivityTaleBinding;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.GameActivity;
import ir.brandimo.pashmak.util.FaNum;

/**
 * Pashmak tells a tale. The child does nothing but listen and watch the place
 * change, which is the whole point of this section.
 *
 * <p>Turning the page is driven from two directions: the voice finishing, and a
 * timer set from the passage's length. Both are needed — most Android devices have
 * no Persian voice, so on those nothing ever "finishes" and only the timer moves the
 * story on; where there is a voice, it usually lands first and the timer is dropped.
 */
public class TaleActivity extends GameActivity {

    public static final String EXTRA_TALE = "tale_index";

    private ActivityTaleBinding binding;
    private Tale tale;
    private int taleIndex;
    private int momentIndex;
    private boolean playing = true;

    /** Guards against a late voice callback turning two pages at once. */
    private int token;

    private final Runnable advance = this::nextMoment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.taleHeader.headerTitle.setTextColor(
                ContextCompat.getColor(this, R.color.gold_ink));
        binding.taleHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        bindStars(binding.taleHeader.headerStarsValue);
        attachCompanion();

        binding.talePlay.setOnClickListener(v -> {
            tap();
            setPlaying(!playing);
        });
        binding.taleNext.setOnClickListener(v -> {
            tap();
            goTo(momentIndex + 1);
        });
        binding.talePrev.setOnClickListener(v -> {
            tap();
            goTo(momentIndex - 1);
        });
        binding.taleNextTale.setOnClickListener(v -> {
            tap();
            openTale(taleIndex + 1);
        });

        openTale(getIntent().getIntExtra(EXTRA_TALE, 0));
    }

    /** The passage is printed in full on screen, so a bubble would say it twice. */
    @Override
    protected boolean showsBubble() {
        return false;
    }

    @Override
    protected String musicTrack() {
        return AudioManifest.BGM_STORY;
    }

    private void openTale(int index) {
        taleIndex = Math.max(0, index) % TaleCatalog.count();
        tale = TaleCatalog.tale(taleIndex);
        binding.taleHeader.headerTitle.setText(tale.title);
        momentIndex = 0;
        playing = true;
        binding.taleScene.reset(tale.sceneFor(0));
        renderMoment();
    }

    private void goTo(int index) {
        if (index < 0) {
            index = 0;
        }
        if (index >= tale.size()) {
            finishTale();
            return;
        }
        momentIndex = index;
        renderMoment();
    }

    private void nextMoment() {
        goTo(momentIndex + 1);
    }

    private void renderMoment() {
        cancelPending();
        Tale.Moment moment = tale.moment(momentIndex);
        binding.taleLine.setText(moment.text);
        binding.taleScene.show(tale.sceneFor(momentIndex));
        binding.taleProgress.setText(getString(R.string.tale_progress,
                FaNum.of(momentIndex + 1), FaNum.of(tale.size())));
        renderPlayButton();
        if (playing) {
            speakMoment(moment);
        }
    }

    private void speakMoment(Tale.Moment moment) {
        final int mine = ++token;
        long hold = moment.durationMs();
        mascot.say(moment.text, MascotState.TALK, hold,
                AudioManifest.taleMoment(tale.id, momentIndex),
                () -> {
                    // The voice got there first; drop the stopwatch.
                    if (mine == token && playing) {
                        binding.getRoot().removeCallbacks(advance);
                        binding.getRoot().postDelayed(advance, 500L);
                    }
                });
        binding.getRoot().postDelayed(advance, hold);
    }

    private void setPlaying(boolean value) {
        playing = value;
        if (playing) {
            renderMoment();
        } else {
            cancelPending();
            mascot.idle();
            renderPlayButton();
        }
    }

    private void renderPlayButton() {
        binding.talePlay.setImageResource(
                playing ? R.drawable.ic_pause : R.drawable.ic_play);
        binding.talePlay.setContentDescription(
                getString(playing ? R.string.tale_pause : R.string.tale_play));
    }

    private void cancelPending() {
        token++;
        binding.getRoot().removeCallbacks(advance);
    }

    private void finishTale() {
        cancelPending();
        playing = false;
        renderPlayButton();
        prefs.setTaleDone(tale.id);
        mascot.addStars(2);
        mascot.say(getString(R.string.tale_finished), MascotState.CHEER,
                MascotController.HOLD_CHEER_MS, AudioManifest.VOICE_TALE_DONE);
    }

    @Override
    protected void onPause() {
        cancelPending();
        mascot.idle();
        super.onPause();
    }
}
