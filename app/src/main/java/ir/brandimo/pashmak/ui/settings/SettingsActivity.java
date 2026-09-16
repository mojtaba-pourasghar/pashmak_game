package ir.brandimo.pashmak.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioInventory;
import ir.brandimo.pashmak.audio.SpeechEngine;
import ir.brandimo.pashmak.data.prefs.GamePrefs;
import ir.brandimo.pashmak.databinding.ActivitySettingsBinding;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.util.FaNum;

/** Behind the parent gate: difficulty, audio and a way to reset the score. */
public class SettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.settingsHeader.headerTitle.setText(R.string.settings_title);
        binding.settingsHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        bindStars(binding.settingsHeader.headerStarsValue);

        binding.settingsSoundSub.setText(
                getString(R.string.settings_sound_sub, getString(R.string.mascot_name)));

        binding.settingsEasy.setOnClickListener(v -> setDifficulty(GamePrefs.DIFFICULTY_EASY));
        binding.settingsMedium.setOnClickListener(v -> setDifficulty(GamePrefs.DIFFICULTY_MEDIUM));
        binding.settingsHard.setOnClickListener(v -> setDifficulty(GamePrefs.DIFFICULTY_HARD));

        binding.settingsSoundSwitch.setChecked(prefs.soundEnabled() && !prefs.isMuted());
        binding.settingsSoundSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.setSoundEnabled(checked);
            mascot.setMuted(!checked);
        });

        binding.settingsMusicSwitch.setChecked(prefs.musicEnabled());
        binding.settingsMusicSwitch.setOnCheckedChangeListener(
                (button, checked) -> prefs.setMusicEnabled(checked));

        binding.settingsResetStars.setOnClickListener(v -> {
            tap();
            prefs.resetStars();
            Toast.makeText(this, R.string.settings_stars_reset, Toast.LENGTH_SHORT).show();
        });

        renderDifficulty();
        renderAudioInventory();
        renderVoice();
    }

    /**
     * Pashmak speaks through the device, and most Android devices cannot speak
     * Persian — Google's engine does not ship it. If this one cannot, say so here
     * rather than leaving a grown-up wondering why the character is mute.
     */
    private void renderVoice() {
        SpeechEngine.Status status = SpeechEngine.get(this).status();
        int line;
        switch (status) {
            case READY:
                line = R.string.settings_voice_ready;
                break;
            case NO_PERSIAN:
                line = R.string.settings_voice_no_persian;
                break;
            case UNAVAILABLE:
                line = R.string.settings_voice_unavailable;
                break;
            case STARTING:
            default:
                line = R.string.settings_voice_starting;
                break;
        }
        binding.settingsVoice.setText(line);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The engine may have finished starting up since the screen opened.
        renderVoice();
    }

    /**
     * Whoever drops an audio file in has no way to tell from inside the app whether
     * it landed under the right name, so say what was found. The music is reported
     * first and on its own: those three files are the only ones a parent is really
     * expected to supply, and a tally of hundreds of optional voice clips would
     * otherwise read as though something were broken when nothing is.
     */
    private void renderAudioInventory() {
        AudioInventory.Report report = AudioInventory.scan(this);
        String music = report.musicFound == report.musicTotal
                ? getString(R.string.settings_audio_music_all)
                : getString(R.string.settings_audio_music_some,
                        FaNum.of(report.musicFound), FaNum.of(report.musicTotal));
        String voices = report.found == report.total
                ? getString(R.string.settings_audio_all, FaNum.of(report.total))
                : getString(R.string.settings_audio_some,
                        FaNum.of(report.found), FaNum.of(report.total),
                        TextUtils.join("، ", report.missingSample));
        binding.settingsAudio.setText(music + "\n" + voices);
    }

    private void setDifficulty(int level) {
        tap();
        prefs.setDifficulty(level);
        renderDifficulty();
    }

    private void renderDifficulty() {
        int level = prefs.difficulty();
        binding.settingsEasy.setSelected(level == GamePrefs.DIFFICULTY_EASY);
        binding.settingsMedium.setSelected(level == GamePrefs.DIFFICULTY_MEDIUM);
        binding.settingsHard.setSelected(level == GamePrefs.DIFFICULTY_HARD);

        int active = androidx.core.content.ContextCompat.getColor(this, R.color.white);
        int idle = androidx.core.content.ContextCompat.getColor(this, R.color.ink_secondary);
        binding.settingsEasy.setTextColor(
                level == GamePrefs.DIFFICULTY_EASY ? active : idle);
        binding.settingsMedium.setTextColor(
                level == GamePrefs.DIFFICULTY_MEDIUM ? active : idle);
        binding.settingsHard.setTextColor(
                level == GamePrefs.DIFFICULTY_HARD ? active : idle);
    }
}
