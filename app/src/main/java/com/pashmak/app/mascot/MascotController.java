package com.pashmak.app.mascot;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.pashmak.app.audio.AudioManifest;
import com.pashmak.app.audio.MusicEngine;
import com.pashmak.app.audio.SoundBank;
import com.pashmak.app.audio.VoicePlayer;
import com.pashmak.app.data.prefs.GamePrefs;

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
    /**
     * A breath between the last syllable and the mascot dropping back to idle, so a
     * line never ends on the same tick it stops sounding.
     */
    public static final long VOICE_TAIL_MS = 260L;
    /**
     * The slowest the read-along is allowed to crawl. Measured over the whole voice
     * set: the slowest clip spends 308ms on a character, so anything under that would
     * make the words overtake the voice on at least one line.
     */
    private static final long TYPEWRITER_MAX_MS = 320L;

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
     *
     * <p>Returns how many milliseconds this line occupies — the recording's own
     * length when there is one — so a caller that wants to move on afterwards can
     * wait for that instead of typing a number and cutting him off.
     */
    public long say(String text, @Nullable MascotState mood, long holdMs,
                    @NonNull String audio) {
        return speakInternal(text, mood, holdMs, audio, null);
    }

    /**
     * As {@link #say}, but tells the caller when the line has actually finished
     * sounding. The told tales use it to turn the page on the voice rather than on a
     * stopwatch — though they keep a stopwatch too, because on a device with no
     * Persian voice nothing sounds and nothing would ever finish.
     */
    public long say(String text, @Nullable MascotState mood, long holdMs,
                    @NonNull String audio, @Nullable Runnable whenSpoken) {
        return speakInternal(text, mood, holdMs, audio, whenSpoken);
    }

    private long speakInternal(String text, @Nullable MascotState mood, long holdMs,
                               @Nullable String audio, @Nullable Runnable whenSpoken) {
        cancelPending();
        String body = text == null ? "" : text;
        MascotState pose = mood == null ? MascotState.TALK : mood;
        pendingText = body;
        typedChars = 0;
        state.setValue(new MascotUiState(pose, body, "", !body.isEmpty(), prefs.isMuted()));
        // Pass the words as well as the clip name: a recording wins if one exists,
        // otherwise the device speaks the line itself.
        boolean sounding = voice.speak(audio, body, () -> {
            idle();
            if (whenSpoken != null) {
                whenSpoken.run();
            }
        });
        // How long this line lasts. A recording knows exactly; the device's own voice
        // does not say, so only then is the word count used.
        long voiced = 0L;
        if (sounding) {
            long clip = voice.soundingMs();
            voiced = clip > 0 ? clip : spokenMs(body);
        }
        if (!body.isEmpty()) {
            setSpeaking(true);
            // When the line is actually being said, the words appear at the speed it
            // is said at, so a child follows along instead of reading it and then
            // waiting. In silence the typewriter keeps its own brisk pace.
            startTypewriter(voiced > 0 ? pace(body, voiced) : TYPEWRITER_MS);
        }
        long hold = Math.max(HOLD_MIN_MS, holdMs <= 0 ? HOLD_DEFAULT_MS : holdMs);
        if (voiced > 0) {
            // The hold is a fallback for a line nothing says out loud. Letting it fire
            // while Pashmak is still talking would cut him off mid-sentence, which is
            // exactly what happened to every line in the app: the holds were set for a
            // 110-words-a-minute reading and the voice reads at 87, so the longest
            // passages were losing their last six seconds.
            hold = Math.max(hold, voiced + VOICE_TAIL_MS);
        }
        dismiss = this::idle;
        handler.postDelayed(dismiss, hold);
        return hold;
    }

    public long speak(@NonNull MascotLine line, long holdMs) {
        return speakInternal(line.text, line.mood, holdMs, line.audio, null);
    }

    /**
     * Celebrates and awards a star, exactly as the prototype's cheer() does. Returns
     * how long the praise lasts, as {@link #say} does.
     */
    public long cheer() {
        return cheer(1);
    }

    public long cheer(int stars) {
        if (stars != 0) {
            addStars(stars);
        }
        return speak(dialogues.pick(MascotDialogues.BUCKET_SUCCESS), HOLD_CHEER_MS);
    }

    /** Warm, never punishing — the design is explicit about this. */
    public long encourage() {
        sounds.play(AudioManifest.SFX_WRONG);
        return speak(dialogues.pick(MascotDialogues.BUCKET_ENCOURAGE), HOLD_ENCOURAGE_MS);
    }

    public long poke() {
        return speak(dialogues.pick(MascotDialogues.BUCKET_POKE), HOLD_GIGGLE_MS);
    }

    public long help(String key, String... args) {
        return speak(dialogues.help(key, args), HOLD_DEFAULT_MS);
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

    /**
     * Roughly how long a line takes to say aloud, used only when nothing can be asked
     * for the real figure — the device's own voice. 87 words a minute, measured across
     * all 1,136 recordings rather than guessed; the 110 that used to be here is a
     * newsreader's pace and it was clipping the end off seven lines in ten.
     */
    private static long spokenMs(String text) {
        int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        return (long) (words / 87f * 60000f) + 800L;
    }

    /**
     * Milliseconds per character, so the words land with the voice. The step comes
     * from the line's real length divided by its letters, which is the whole point —
     * the ceiling is only there so a mis-tagged clip of several minutes cannot leave
     * the read-along frozen on the first word.
     */
    private static long pace(String text, long voicedMs) {
        long step = voicedMs / Math.max(1, text.length());
        return Math.max(TYPEWRITER_MS, Math.min(step, TYPEWRITER_MAX_MS));
    }

    private void startTypewriter(final long step) {
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
                    handler.postDelayed(this, step);
                } else if (!voice.isSpeaking()) {
                    setSpeaking(false);
                }
            }
        };
        handler.postDelayed(typer, step);
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
