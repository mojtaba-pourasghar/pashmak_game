package ir.brandimo.pashmak.mascot;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ir.brandimo.pashmak.audio.AudioManifest;
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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<MascotUiState> state = new MutableLiveData<>();

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

    public MascotDialogues dialogues() {
        return dialogues;
    }

    /** Speak a line, with a pose and a hold before returning to idle. */
    public void say(String text, @Nullable MascotState mood, long holdMs) {
        say(text, mood, holdMs, null);
    }

    public void say(String text, @Nullable MascotState mood, long holdMs, @Nullable String audio) {
        cancelPending();
        String body = text == null ? "" : text;
        MascotState pose = mood == null ? MascotState.TALK : mood;
        pendingText = body;
        typedChars = 0;
        state.setValue(new MascotUiState(pose, body, "", !body.isEmpty(), prefs.isMuted()));
        if (!body.isEmpty()) {
            startTypewriter();
        }
        if (audio != null) {
            voice.speak(audio, this::idle);
        }
        long hold = Math.max(HOLD_MIN_MS, holdMs <= 0 ? HOLD_DEFAULT_MS : holdMs);
        dismiss = this::idle;
        handler.postDelayed(dismiss, hold);
    }

    public void speak(@NonNull MascotLine line, long holdMs) {
        say(line.text, line.mood, holdMs, line.audio);
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
        say(dialogues.help(key, args), MascotState.TALK, HOLD_DEFAULT_MS);
    }

    public void welcome() {
        MascotLine line = dialogues.welcome();
        say(line.text, MascotState.ENTER, HOLD_WELCOME_MS);
        handler.postDelayed(() -> say(line.text, line.mood, HOLD_WELCOME_MS, line.audio),
                DOCK_TRANSITION_MS);
    }

    public void idle() {
        cancelPending();
        state.setValue(MascotUiState.idle(prefs.isMuted()));
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
            voice.stopMusic();
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
                }
            }
        };
        handler.postDelayed(typer, TYPEWRITER_MS);
    }

    private void cancelPending() {
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
