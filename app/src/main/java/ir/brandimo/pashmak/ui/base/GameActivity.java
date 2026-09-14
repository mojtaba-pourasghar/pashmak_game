package ir.brandimo.pashmak.ui.base;

import android.os.Bundle;

import androidx.annotation.Nullable;

import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.audio.VoicePlayer;

/** A mini-game screen: companion docked, play music running, mascot reacting. */
public abstract class GameActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoicePlayer.get(this).startMusic(AudioManifest.BGM_PLAY);
    }

    @Override
    protected void onPause() {
        VoicePlayer.get(this).stopMusic();
        super.onPause();
    }

    /** Correct answer: celebrate and hand out a star. */
    protected void onCorrect() {
        sounds.play(AudioManifest.SFX_MATCH);
        mascot.cheer();
    }

    protected void onCorrect(String line) {
        sounds.play(AudioManifest.SFX_MATCH);
        mascot.addStars(1);
        mascot.say(line, ir.brandimo.pashmak.mascot.MascotState.CHEER,
                ir.brandimo.pashmak.mascot.MascotController.HOLD_CHEER_MS);
    }

    /** Wrong answer: never a scolding, always a nudge. */
    protected void onWrong() {
        mascot.encourage();
    }

    protected void onWrong(String line) {
        sounds.play(AudioManifest.SFX_WRONG);
        mascot.say(line, ir.brandimo.pashmak.mascot.MascotState.ENCOURAGE,
                ir.brandimo.pashmak.mascot.MascotController.HOLD_ENCOURAGE_MS);
    }
}
