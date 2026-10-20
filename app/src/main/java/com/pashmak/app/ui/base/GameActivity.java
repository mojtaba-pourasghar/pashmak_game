package com.pashmak.app.ui.base;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pashmak.app.audio.AudioManifest;
import com.pashmak.app.mascot.MascotController;
import com.pashmak.app.mascot.MascotState;

/** A mini-game screen: companion docked, play music running, mascot reacting. */
public abstract class GameActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String musicTrack() {
        return AudioManifest.BGM_PLAY;
    }

    /**
     * Correct answer: celebrate and hand out a star. Returns how long the praise
     * lasts, so a screen that moves on afterwards can wait for it rather than for a
     * number somebody typed.
     */
    protected long onCorrect() {
        sounds.play(AudioManifest.SFX_MATCH);
        return mascot.cheer();
    }

    /** Correct, with this screen's own words — and the clip that reads them. */
    protected long onCorrect(String line, @NonNull String clip) {
        sounds.play(AudioManifest.SFX_MATCH);
        mascot.addStars(1);
        return mascot.say(line, MascotState.CHEER, MascotController.HOLD_CHEER_MS, clip);
    }

    /** Wrong answer: never a scolding, always a nudge. */
    protected long onWrong() {
        return mascot.encourage();
    }

    protected long onWrong(String line, @NonNull String clip) {
        sounds.play(AudioManifest.SFX_WRONG);
        return mascot.say(line, MascotState.ENCOURAGE,
                MascotController.HOLD_ENCOURAGE_MS, clip);
    }
}
