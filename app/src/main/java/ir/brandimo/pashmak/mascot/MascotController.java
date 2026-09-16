package ir.brandimo.pashmak.mascot;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.audio.MusicEngine;
import ir.brandimo.pashmak.audio.SoundBank;
import ir.brandimo.pashmak.audio.VoicePlayer;
import ir.brandimo.pashmak.data.prefs.GamePrefs;

/**
 * The single place that decides what the mascot is doing. Screens call cheer(),
 * encourage(), say() and so on; everything else — the typewriter, the hold timer,
 * the voice clip, the star award — happens here, mirroring the design's
 * MascotController so behavior stays identical to the approved prototype.
 */
public final class MascotController {

    /* Timings from MascotConfig.js. */
    public static final long TYPEWRITER_MS = 26L;
    public static final long HOLD_DEFAULT_MS = 4600L;
    public static final long HOLD_MIN_MS = 2200L;
    public static final long DOCK_TRANSITION_MS = 620L;
    public static final long HOLD_CHEER_MS = 3200L;
    public static final long HOLD_ENCOURAGE_MS = 3000L;
    public static final long HOLD_GIGGLE_MS = 2200L;
    public static final long HOLD_WELCOME_MS = 9000L;

    private static volatile MascotController instance;

    private final Context appContext;
    private final GamePrefs prefs;
    private final MascotDialogues dialogues;
    private final VoicePlayer voice;
    private final SoundBank sounds;
    private final MusicEngine music;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<MascotUiState> state = new MutableLiveData<>();
    /** True while words are actually appearing, which is what drives the lips. */
    private final MutableLiveData<Boolean> speaking = new MutableLiveData<>(false);

    private Runnable typer;
    private Runnable dismiss;
    private String pendingText = "";
    private int typedChars;

    private MascotController(Context context) {
        appContext = context.getApplicationContext();
        prefs = GamePrefs.get(appContext);
        dialogues = new MascotDialogues(appContext);
        voice = VoicePlayer.get(appContext);
        sounds = SoundBank.get(appContext);
        music = MusicEngine.get(appContext);
        state.setValue(MascotUiState.idle(prefs.isMuted()));
    }

    public static MascotController get(@NonNull Context context) {
        MascotController local = instance;
        if (local == null) {
            synchronized (MascotController.class) {
                if (instance == null) {
                    instance = new MascotController(context);
                }
                local = instance;
            }
        }
        return local;
    }

    public LiveData<MascotUiState> state() {
        return state;
    }

    /** Observed by every MascotView so its mouth moves in step with the text. */
    public LiveData<Boolean> speaking() {
        return speaking;
    }

    public MascotDialogues dialogues() {
        return dialogues;
    }

    /**
     * Speak a line, with a pose, a hold before returning to idle, and the clip to
     * play. The clip is not optional: every line in the app names one, so a new
     * screen cannot quietly ship a mouth moving over silence. Pass a name from
     * {@link AudioManifest}; if the recording is not in res/raw yet the words
     * still appear and nothing sounds.
     */
    public void say(String text, @Nullable MascotState mood, long holdMs,
                    @NonNull String audio) {
        speakInternal(text, mood, holdMs, audio, null);
    }

    /**
     * As {@link #say}, but tells the caller when the line has actually finished
     * sounding. The told tales use it to turn the page on the voice rather than on a
     * stopwatch — though they keep a stopwatch too, because on a device with no
     * Persian voice nothing sounds and nothing would ever finish.
     */
    public void say(String text, @Nullable MascotState mood, long holdMs,
                    @NonNull String audio, @Nullable Runnable whenSpoken) {
        speakInternal(text, mood, holdMs, audio, whenSpoken);
    }

    private void speakInternal(String text, @Nullable MascotState mood, long holdMs,
                               @Nullable String audio, @Nullable Runnable whenSpoken) {
        cancelPending();
        String body = text == null ? "" : text;
        MascotState pose = mood == null ? MascotState.TALK : mood;
        pendingText = body;
        typedChars = 0;
        state.setValue(new MascotUiState(pose, body, "", !body.isEmpty(), prefs.isMuted()));
        if (!body.isEmpty()) {
            setSpeaking(true);
            startTypewriter();
        }
        // Pass the words as well as the clip name: a recording wins if one exists,
        // otherwise the device speaks the line itself.
        if (voice.speak(audio, body, () -> {
            idle();
            if (whenSpoken != null) {
                whenSpoken.run();
            }
        })) {
            // Something is actually sounding, so keep the mouth going until it ends
            // rather than stopping when the text has finished appearing.
            setSpeaking(true);
        }
        long hold = Math.max(HOLD_MIN_MS, holdMs <= 0 ? HOLD_DEFAULT_MS : holdMs);
        dismiss = this::idle;
        handler.postDelayed(dismiss, hold);
    }

    public void speak(@NonNull MascotLine line, long holdMs) {
        speakInternal(line.text, line.mood, holdMs, line.audio, null);
    }

    /** Celebrates and awards a star, exactly as the prototype's cheer() does. */
    public void cheer() {
        cheer(1);
    }

    public void cheer(int stars) {
        if (stars != 0) {
            addStars(stars);
        }
        speak(dialogues.pick(MascotDialogues.BUCKET_SUCCESS), HOLD_CHEER_MS);
    }

    /** Warm, never punishing — the design is explicit about this. */
    public void encourage() {
        sounds.play(AudioManifest.SFX_WRONG);
        speak(dialogues.pick(MascotDialogues.BUCKET_ENCOURAGE), HOLD_ENCOURAGE_MS);
    }

    public void poke() {
        speak(dialogues.pick(MascotDialogues.BUCKET_POKE), HOLD_GIGGLE_MS);
    }

    public void help(String key, String... args) {
        speak(dialogues.help(key, args), HOLD_DEFAULT_MS);
    }

    public void welcome() {
        MascotLine line = dialogues.welcome();
        // The entrance pose first, silent; the clip plays once he has landed.
        speakInternal(line.text, MascotState.ENTER, HOLD_WELCOME_MS, null, null);
        handler.postDelayed(() -> speakInternal(line.text, line.mood, HOLD_WELCOME_MS,
                line.audio, null), DOCK_TRANSITION_MS);
    }

    public void idle() {
        cancelPending();
        setSpeaking(false);
        state.setValue(MascotUiState.idle(prefs.isMuted()));
    }

    /** One place to flip the flag, so music ducking can never drift out of step. */
    private void setSpeaking(boolean value) {
        if (Boolean.valueOf(value).equals(speaking.getValue())) {
            return;
        }
        speaking.setValue(value);
        music.setDucked(value);
    }

    public void dock() {
        prefs.setWelcomed(true);
        idle();
    }

    public boolean hasWelcomed() {
        return prefs.hasWelcomed();
    }

    public void addStars(int delta) {
        prefs.addStars(delta);
        if (delta > 0) {
            sounds.play(AudioManifest.SFX_STAR);
        }
    }

    public int stars() {
        return prefs.stars();
    }

    public LiveData<Integer> starsLive() {
        return prefs.starsLive();
    }

    public boolean isMuted() {
        return prefs.isMuted();
    }

    public void toggleMute() {
        setMuted(!prefs.isMuted());
    }

    public void setMuted(boolean muted) {
        prefs.setMuted(muted);
        if (muted) {
            voice.stopVoice();
            music.stop();
        }
        MascotUiState current = state.getValue();
        state.setValue(current == null
                ? MascotUiState.idle(muted)
                : current.withMuted(muted));
    }

    private void startTypewriter() {
        typer = new Runnable() {
            @Override
            public void run() {
                typedChars++;
                MascotUiState current = state.getValue();
                if (current == null) {
                    return;
                }
                int end = Math.min(typedChars, pendingText.length());
                state.setValue(current.withTyped(pendingText.substring(0, end)));
                if (end < pendingText.length()) {
                    handler.postDelayed(this, TYPEWRITER_MS);
                } else if (!voice.isSpeaking()) {
                    setSpeaking(false);
                }
            }
        };
        handler.postDelayed(typer, TYPEWRITER_MS);
    }

    private void cancelPending() {
        voice.stopVoice();
        if (typer != null) {
            handler.removeCallbacks(typer);
            typer = null;
        }
        if (dismiss != null) {
            handler.removeCallbacks(dismiss);
            dismiss = null;
        }
    }
}
