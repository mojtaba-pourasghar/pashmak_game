package ir.brandimo.pashmak.ui.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioInventory;
import ir.brandimo.pashmak.audio.SpeechEngine;
import ir.brandimo.pashmak.audio.VoiceInstaller;
import ir.brandimo.pashmak.data.prefs.GamePrefs;
import ir.brandimo.pashmak.databinding.ActivitySettingsBinding;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.util.FaNum;

/** Behind the parent gate: difficulty, audio and a way to reset the score. */
public class SettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;
    /** Held as a field so it can be taken off again in onPause. */
    private final SpeechEngine.StatusListener voiceWatcher =
            status -> renderVoice();

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
        binding.settingsVoiceFix.setOnClickListener(v -> fetchVoice());
        renderVoice();
    }

    /**
     * What the parent is told about the voice, and what they can do about it.
     *
     * <p>The rule here is that nothing on this screen is a wall. The app works with
     * no voice at all — every line is on screen and a recording in res/raw always
     * wins over synthesis — so a missing voice is an offer, never a warning, and
     * when there is nothing a parent could usefully do the block is hidden rather
     * than left sitting there saying something broken.
     */
    private void renderVoice() {
        SpeechEngine speech = SpeechEngine.get(this);
        SpeechEngine.Status status = speech.status();
        String line = null;
        Integer action = null;

        switch (status) {
            case READY:
                String engine = engineLabel(speech.voiceEngine());
                line = getString(R.string.settings_voice_ready)
                        + (engine == null ? ""
                           : " " + getString(R.string.settings_voice_engine_named, engine));
                break;
            case NEEDS_DATA:
                // An engine that knows Persian and only wants the pack. One tap.
                line = getString(R.string.settings_voice_needs_data);
                action = R.string.settings_voice_download;
                break;
            case NO_PERSIAN:
                line = getString(R.string.settings_voice_no_persian);
                action = voiceAlreadyInstalled() != null
                        ? R.string.settings_voice_installed_open
                        : R.string.settings_voice_fix;
                break;
            case UNAVAILABLE:
                // No engine at all. Saying so helps nobody and reads as a fault, so
                // the whole block goes away and the app carries on with its text.
                break;
            case STARTING:
            default:
                line = getString(R.string.settings_voice_starting);
                break;
        }

        binding.settingsVoice.setVisibility(line == null ? View.GONE : View.VISIBLE);
        if (line != null) {
            binding.settingsVoice.setText(line);
        }
        binding.settingsVoiceFix.setVisibility(action == null ? View.GONE : View.VISIBLE);
        if (action != null) {
            binding.settingsVoiceFix.setText(action);
        }
    }

    /** An engine we know about that is already on the device, or null. */
    @Nullable
    private String voiceAlreadyInstalled() {
        if (VoiceInstaller.isInstalled(this, VoiceInstaller.ESPEAK)) {
            return VoiceInstaller.ESPEAK;
        }
        if (VoiceInstaller.isInstalled(this, VoiceInstaller.RHVOICE)) {
            return VoiceInstaller.RHVOICE;
        }
        return null;
    }

    /** A name a parent would recognise, for the engine doing the talking. */
    @Nullable
    private String engineLabel(@Nullable String pkg) {
        if (pkg == null) {
            return null;
        }
        try {
            PackageManager packages = getPackageManager();
            return packages.getApplicationLabel(
                    packages.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    /**
     * Gets a Persian voice onto the device, in the order that actually helps.
     *
     * <p>If an engine is already installed, it is opened — an engine whose Persian
     * data has not been downloaded looks exactly like an engine with no Persian, and
     * the fix is on its own screen. Otherwise the store: Bazaar first, because that
     * is the one an Iranian family is likely to have, then Google Play, then the
     * plain web page.
     */
    private void fetchVoice() {
        tap();
        // Cheapest errand first: an engine that already knows Persian and only wants
        // the language pack. That is a download inside an app the device already has.
        Intent download = SpeechEngine.get(this).voiceDataIntent();
        if (download != null && start(download)) {
            return;
        }
        String installed = voiceAlreadyInstalled();
        if (installed != null && VoiceInstaller.launch(this, installed)) {
            return;
        }
        if (VoiceInstaller.open(this, VoiceInstaller.ESPEAK) == null) {
            Toast.makeText(this, R.string.settings_voice_no_store,
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Opens another app's screen, answering whether it went anywhere. */
    private boolean start(@NonNull Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from the store, or from the engine's own screen, is exactly
        // when a voice may have appeared — so look again rather than trusting what
        // was found when this screen was first opened.
        SpeechEngine speech = SpeechEngine.get(this);
        speech.watch(voiceWatcher);
        if (speech.status() != SpeechEngine.Status.READY) {
            speech.restart();
        }
        renderVoice();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SpeechEngine.get(this).unwatch(voiceWatcher);
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
