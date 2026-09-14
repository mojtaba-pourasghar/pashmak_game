package ir.brandimo.pashmak.ui.base;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.audio.MusicEngine;
import ir.brandimo.pashmak.audio.SoundBank;
import ir.brandimo.pashmak.data.prefs.GamePrefs;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotView;
import ir.brandimo.pashmak.mascot.SpeechBubbleView;
import ir.brandimo.pashmak.util.FaNum;
import ir.brandimo.pashmak.util.LocaleUtil;

/**
 * Shared plumbing for every screen: the docked companion, the star counter and
 * an immersive, kid-proof window.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected MascotController mascot;
    protected GamePrefs prefs;
    protected SoundBank sounds;

    private boolean observingSpeech;

    @Nullable
    private MascotView dockMascot;
    @Nullable
    private SpeechBubbleView dockBubble;

    /** The app is Persian-only; it must not inherit the device's language. */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtil.persian(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        mascot = MascotController.get(this);
        prefs = GamePrefs.get(this);
        sounds = SoundBank.get(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registered here, not in onCreate: LiveData delivers immediately, and the
        // subclass's view binding does not exist yet during super.onCreate().
        if (!observingSpeech) {
            observingSpeech = true;
            mascot.speaking().observe(this, speaking -> {
                boolean value = Boolean.TRUE.equals(speaking);
                if (dockMascot != null) {
                    dockMascot.setSpeaking(value);
                }
                onSpeakingChanged(value);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        goImmersive();
        MusicEngine.get(this).play(musicTrack());
    }

    /** Which loop this screen wants; play games music over the menu theme. */
    protected String musicTrack() {
        return AudioManifest.BGM_MENU;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            goImmersive();
        }
    }

    /** Small hands rest on the edges of a tablet; keep the system bars out of the way. */
    private void goImmersive() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Wires the corner companion if the layout includes one. The mascot itself is
     * decorative and non-clickable so it can never steal a tap from the game.
     */
    protected void attachCompanion() {
        dockMascot = findViewById(R.id.dock_mascot);
        dockBubble = findViewById(R.id.dock_bubble);
        if (dockBubble != null) {
            dockBubble.setOnMuteToggled(() -> mascot.toggleMute());
        }
        mascot.state().observe(this, state -> {
            if (state == null) {
                return;
            }
            if (dockMascot != null) {
                dockMascot.setState(state.mood);
            }
            if (dockBubble != null) {
                dockBubble.bind(state);
            }
        });
    }

    /**
     * Shrinks the docked companion, for screens whose side column is already full
     * of tools. An include can only resize its own root, so this is done in code.
     */
    protected void shrinkCompanion() {
        if (dockMascot == null) {
            return;
        }
        ViewGroup.LayoutParams params = dockMascot.getLayoutParams();
        if (params != null) {
            params.height = getResources().getDimensionPixelSize(R.dimen.mascot_dock_small);
            dockMascot.setLayoutParams(params);
        }
    }

    /** Binds a star counter that follows the shared total. */
    protected void bindStars(@Nullable TextView view) {
        if (view == null) {
            return;
        }
        prefs.starsLive().observe(this, stars ->
                view.setText(FaNum.of(stars == null ? 0 : stars)));
    }

    /** Screens with their own large mascot override this to sync its mouth too. */
    protected void onSpeakingChanged(boolean speaking) {
    }

    protected void tap() {
        sounds.play(AudioManifest.SFX_TAP);
    }

    protected void open(Class<?> activity) {
        tap();
        startActivity(new Intent(this, activity));
    }

    protected void openAndFinish(Class<?> activity) {
        tap();
        startActivity(new Intent(this, activity));
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
