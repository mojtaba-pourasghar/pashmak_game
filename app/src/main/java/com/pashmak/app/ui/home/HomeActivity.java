package com.pashmak.app.ui.home;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.pashmak.app.R;
import com.pashmak.app.audio.AudioManifest;
import com.pashmak.app.databinding.ActivityHomeBinding;
import com.pashmak.app.mascot.MascotController;
import com.pashmak.app.mascot.MascotState;
import com.pashmak.app.ui.base.BaseActivity;
import com.pashmak.app.ui.games.GamesActivity;
import com.pashmak.app.ui.gallery.GalleryActivity;
import com.pashmak.app.ui.lullaby.LullabyActivity;
import com.pashmak.app.ui.missions.MissionsActivity;
import com.pashmak.app.ui.parentgate.ParentGateDialog;

/** The hub. The mascot is big here and answers to being poked. */
public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String name = getString(R.string.mascot_name);
        binding.homeSubtitle.setText(getString(R.string.home_subtitle, name));
        binding.homeBtnMissions.setText(getString(R.string.home_missions, name));

        bindStars(binding.homeStars.starsValue);

        binding.homeBtnMissions.setOnClickListener(v -> open(MissionsActivity.class));
        binding.homeBtnGames.setOnClickListener(v -> open(GamesActivity.class));
        binding.homeBtnGallery.setOnClickListener(v -> open(GalleryActivity.class));
        binding.homeBtnLullaby.setOnClickListener(v -> open(LullabyActivity.class));
        binding.homeBtnParents.setOnClickListener(v -> {
            tap();
            ParentGateDialog.show(this);
        });
        binding.homeMascotHolder.setOnClickListener(v -> {
            sounds.play(AudioManifest.SFX_TAP);
            mascot.say(getString(R.string.ms_poke_home), MascotState.TICKLE,
                    MascotController.HOLD_GIGGLE_MS, AudioManifest.VOICE_GIGGLE);
        });

        binding.homeBubble.setOnMuteToggled(() -> mascot.toggleMute());
        mascot.state().observe(this, state -> {
            if (state == null) {
                return;
            }
            // At rest the character waves at the child rather than just standing there.
            binding.homeMascot.setState(
                    state.mood == MascotState.IDLE ? MascotState.WAVE : state.mood);
            binding.homeBubble.bind(state);
        });

        if (!mascot.hasWelcomed()) {
            // Mark it welcomed up front; dock() would cancel the greeting it just started.
            prefs.setWelcomed(true);
            mascot.welcome();
        }
    }

    @Override
    protected void onSpeakingChanged(boolean speaking) {
        if (binding != null) {
            binding.homeMascot.setSpeaking(speaking);
        }
    }
}
