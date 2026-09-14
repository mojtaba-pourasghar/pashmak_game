package ir.brandimo.pashmak.ui.home;

import android.os.Bundle;

import androidx.annotation.Nullable;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.databinding.ActivityHomeBinding;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.ui.games.GamesActivity;
import ir.brandimo.pashmak.ui.gallery.GalleryActivity;
import ir.brandimo.pashmak.ui.missions.MissionsActivity;
import ir.brandimo.pashmak.ui.parentgate.ParentGateDialog;

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
