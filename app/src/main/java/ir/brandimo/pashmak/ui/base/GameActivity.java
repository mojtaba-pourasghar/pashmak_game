package ir.brandimo.pashmak.ui.base;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;

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

    /** Correct answer: celebrate and hand out a star. */
    protected void onCorrect() {
        sounds.play(AudioManifest.SFX_MATCH);
        mascot.cheer();
    }

    /** Correct, with this screen's own words — and the clip that reads them. */
    protected void onCorrect(String line, @NonNull String clip) {
        sounds.play(AudioManifest.SFX_MATCH);
        mascot.addStars(1);
        mascot.say(line, MascotState.CHEER, MascotController.HOLD_CHEER_MS, clip);
    }

    /** Wrong answer: never a scolding, always a nudge. */
    protected void onWrong() {
        mascot.encourage();
    }

    protected void onWrong(String line, @NonNull String clip) {
        sounds.play(AudioManifest.SFX_WRONG);
        mascot.say(line, MascotState.ENCOURAGE, MascotController.HOLD_ENCOURAGE_MS, clip);
    }
}
