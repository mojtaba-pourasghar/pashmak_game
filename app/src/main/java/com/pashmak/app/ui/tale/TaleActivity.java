package com.pashmak.app.ui.tale;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.pashmak.app.R;
import com.pashmak.app.audio.AudioManifest;
import com.pashmak.app.data.catalog.Tale;
import com.pashmak.app.data.catalog.TaleCatalog;
import com.pashmak.app.databinding.ActivityTaleBinding;
import com.pashmak.app.mascot.MascotController;
import com.pashmak.app.mascot.MascotState;
import com.pashmak.app.ui.base.GameActivity;
import com.pashmak.app.util.FaNum;

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
    private static final String STATE_TALE = "tale_index";
    private static final String STATE_MOMENT = "moment_index";
    private static final String STATE_PLAYING = "playing";
    /** The breath between one moment's last word and the next one starting. */
    private static final long PAGE_PAUSE_MS = 500L;

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
            int next = (taleIndex + 1) % TaleCatalog.count();
            if (!stageOpen(next)) {
                pendingNextTale = true;
                offerFullVersion();
                return;
            }
            openTale(next);
        });

        if (savedInstanceState == null) {
            openTale(getIntent().getIntExtra(EXTRA_TALE, 0));
        } else {
            // Come back to the same passage of the same tale, paused if it was
            // paused. A bedtime story that restarts because the child rolled over
            // with the tablet is worse than no bedtime story.
            openTale(savedInstanceState.getInt(STATE_TALE));
            goTo(savedInstanceState.getInt(STATE_MOMENT));
            setPlaying(savedInstanceState.getBoolean(STATE_PLAYING, true));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TALE, taleIndex);
        outState.putInt(STATE_MOMENT, momentIndex);
        outState.putBoolean(STATE_PLAYING, playing);
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
        binding.taleScene.setActor(tale.actorFor(momentIndex),
                tale.motionFor(momentIndex));
        binding.taleProgress.setText(getString(R.string.tale_progress,
                FaNum.of(momentIndex + 1), FaNum.of(tale.size())));
        renderPlayButton();
        if (playing) {
            speakMoment(moment);
        }
    }

    private void speakMoment(Tale.Moment moment) {
        final int mine = ++token;
        // say() hands back the moment's real length — the recording's own, when there
        // is one — so the page turns after the last word instead of over it. The text
        // estimate in the catalogue is only the floor for a device that says nothing.
        long said = mascot.say(moment.text, MascotState.TALK, moment.durationMs(),
                AudioManifest.taleMoment(tale.id, momentIndex),
                () -> {
                    // The voice got there first; drop the stopwatch.
                    if (mine == token && playing) {
                        binding.getRoot().removeCallbacks(advance);
                        binding.getRoot().postDelayed(advance, PAGE_PAUSE_MS);
                    }
                });
        binding.getRoot().postDelayed(advance, said + PAGE_PAUSE_MS);
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

    /** "Next tale" was tapped on a locked one; it opens once the purchase lands. */
    private boolean pendingNextTale;

    @Override
    protected void onFullVersionBought() {
        if (pendingNextTale) {
            pendingNextTale = false;
            openTale(taleIndex + 1);
        }
    }

    @Override
    protected void onPause() {
        cancelPending();
        mascot.idle();
        super.onPause();
    }
}
