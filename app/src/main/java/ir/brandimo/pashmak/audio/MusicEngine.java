package ir.brandimo.pashmak.audio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.brandimo.pashmak.data.prefs.GamePrefs;

/**
 * Wordless background music.
 *
 * If a real recording has been dropped into res/raw it is used. If not — which is
 * the state the project ships in — a soft pentatonic loop is synthesised here, so
 * the game has music today and silently upgrades the day someone adds a file.
 *
 * Either source ducks to a whisper while Pashmak is speaking, so his voice and the
 * speech bubble always come first.
 */
public final class MusicEngine {

    private static final float FULL_VOLUME = 0.32f;
    private static final float DUCKED_VOLUME = 0.08f;
    private static final long RAMP_MS = 280L;

    /* Synthesis settings. */
    private static final int SAMPLE_RATE = 44100;
    private static final float NOTE_SECONDS = 0.52f;
    /** C major pentatonic — no semitone clashes, so any order sounds gentle. */
    private static final float[] SCALE = {
            261.63f, 293.66f, 329.63f, 392.00f, 440.00f,
            523.25f, 587.33f, 659.25f
    };
    /** A fixed, hand-picked phrase; random notes wander and get tiring. */
    private static final int[] PHRASE = {
            0, 2, 4, 2, 3, 5, 4, 2,
            1, 3, 5, 3, 4, 6, 5, 3,
            0, 2, 4, 5, 7, 5, 4, 2,
            3, 1, 2, 0, 2, 4, 2, 0
    };

    private static volatile MusicEngine instance;

    private final Context appContext;
    private final GamePrefs prefs;

    @Nullable
    private MediaPlayer player;
    @Nullable
    private AudioTrack track;
    @Nullable
    private ValueAnimator ramp;

    private String currentTrack;
    private float volume = FULL_VOLUME;
    private boolean ducked;

    private MusicEngine(Context context) {
        appContext = context.getApplicationContext();
        prefs = GamePrefs.get(appContext);
    }

    public static MusicEngine get(@NonNull Context context) {
        MusicEngine local = instance;
        if (local == null) {
            synchronized (MusicEngine.class) {
                if (instance == null) {
                    instance = new MusicEngine(context);
                }
                local = instance;
            }
        }
        return local;
    }

    /** Starts, or keeps playing, the named loop. Safe to call on every resume. */
    public void play(String name) {
        if (prefs.isMuted() || !prefs.musicEnabled()) {
            stop();
            return;
        }
        if (isPlaying() && name != null && name.equals(currentTrack)) {
            return;
        }
        stop();
        currentTrack = name;
        if (!playFromResources(name)) {
            playSynthesised();
        }
        applyVolume(ducked ? DUCKED_VOLUME : FULL_VOLUME);
    }

    private boolean isPlaying() {
        return player != null || track != null;
    }

    private boolean playFromResources(String name) {
        if (name == null) {
            return false;
        }
        int resId;
        try {
            resId = appContext.getResources().getIdentifier(
                    name, "raw", appContext.getPackageName());
        } catch (Exception e) {
            return false;
        }
        if (resId == 0) {
            return false;
        }
        try {
            player = MediaPlayer.create(appContext, resId);
            if (player == null) {
                return false;
            }
            player.setLooping(true);
            player.start();
            return true;
        } catch (Exception e) {
            releasePlayer();
            return false;
        }
    }

    private void playSynthesised() {
        try {
            short[] samples = renderLoop();
            int bytes = samples.length * 2;
            AudioTrack built;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                built = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(bytes)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
            } else {
                built = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        bytes, AudioTrack.MODE_STATIC);
            }
            built.write(samples, 0, samples.length);
            built.setLoopPoints(0, samples.length, -1);
            built.play();
            track = built;
        } catch (Exception | OutOfMemoryError e) {
            releaseTrack();
        }
    }

    /**
     * Renders one seamless bar-aligned loop: a bell-ish melody over a quiet drone.
     * Each note fades fully before the next, so looping never clicks.
     */
    private short[] renderLoop() {
        int noteSamples = Math.round(NOTE_SECONDS * SAMPLE_RATE);
        int total = noteSamples * PHRASE.length;
        float[] mix = new float[total];

        for (int n = 0; n < PHRASE.length; n++) {
            float freq = SCALE[PHRASE[n] % SCALE.length];
            int start = n * noteSamples;
            for (int i = 0; i < noteSamples; i++) {
                float t = i / (float) SAMPLE_RATE;
                // Quick attack, long exponential tail — a soft mallet, not a beep.
                float envelope = (float) (Math.min(1f, t / 0.015f) * Math.exp(-3.2f * t));
                double phase = 2 * Math.PI * freq * t;
                float value = (float) (Math.sin(phase) + 0.32 * Math.sin(2 * phase)
                        + 0.12 * Math.sin(3 * phase));
                mix[start + i] += 0.16f * envelope * value;
            }
        }

        // A slow drone a fifth apart underneath, to glue the phrase together.
        float droneRoot = SCALE[0] / 2f;
        for (int i = 0; i < total; i++) {
            float t = i / (float) SAMPLE_RATE;
            float swell = 0.5f + 0.5f * (float) Math.sin(2 * Math.PI * t / (total / (float) SAMPLE_RATE));
            mix[i] += 0.05f * swell * (float) Math.sin(2 * Math.PI * droneRoot * t);
            mix[i] += 0.035f * swell * (float) Math.sin(2 * Math.PI * droneRoot * 1.5f * t);
        }

        short[] out = new short[total];
        for (int i = 0; i < total; i++) {
            float value = mix[i];
            if (value > 1f) {
                value = 1f;
            } else if (value < -1f) {
                value = -1f;
            }
            out[i] = (short) (value * Short.MAX_VALUE);
        }
        return out;
    }

    /** Drops the music under Pashmak's voice, then brings it back afterwards. */
    public void setDucked(boolean value) {
        if (ducked == value) {
            return;
        }
        ducked = value;
        rampTo(value ? DUCKED_VOLUME : FULL_VOLUME);
    }

    private void rampTo(float target) {
        cancelRamp();
        final float from = volume;
        ValueAnimator animator = ValueAnimator.ofFloat(from, target);
        animator.setDuration(RAMP_MS);
        animator.addUpdateListener(value -> applyVolume((Float) value.getAnimatedValue()));
        ramp = animator;
        animator.start();
    }

    private void applyVolume(float value) {
        volume = value;
        try {
            if (player != null) {
                player.setVolume(value, value);
            }
            if (track != null) {
                track.setVolume(value);
            }
        } catch (Exception ignored) {
        }
    }

    public void stop() {
        cancelRamp();
        releasePlayer();
        releaseTrack();
        currentTrack = null;
    }

    private void cancelRamp() {
        if (ramp != null) {
            ramp.cancel();
            ramp = null;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }

    private void releaseTrack() {
        if (track != null) {
            try {
                track.stop();
                track.release();
            } catch (Exception ignored) {
            }
            track = null;
        }
    }
}
