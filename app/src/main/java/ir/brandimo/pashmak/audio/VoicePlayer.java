package ir.brandimo.pashmak.audio;

import android.content.Context;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.brandimo.pashmak.data.prefs.GamePrefs;

/**
 * Mascot voice lines and background music. Mirrors the prototype's behavior of
 * failing silently when a clip has not been recorded yet.
 */
public final class VoicePlayer {

    private static volatile VoicePlayer instance;

    private final Context appContext;
    private final GamePrefs prefs;

    private MediaPlayer voice;
    private MediaPlayer music;
    private String musicName;

    private VoicePlayer(Context context) {
        appContext = context.getApplicationContext();
        prefs = GamePrefs.get(appContext);
    }

    public static VoicePlayer get(@NonNull Context context) {
        VoicePlayer local = instance;
        if (local == null) {
            synchronized (VoicePlayer.class) {
                if (instance == null) {
                    instance = new VoicePlayer(context);
                }
                local = instance;
            }
        }
        return local;
    }

    public interface CompletionCallback {
        void onVoiceFinished();
    }

    /** Returns true when a clip actually started, so callers can adjust timing. */
    public boolean speak(@Nullable String name, @Nullable CompletionCallback callback) {
        stopVoice();
        if (name == null || prefs.isMuted() || !prefs.soundEnabled()) {
            return false;
        }
        int resId = resolve(name);
        if (resId == 0) {
            return false;
        }
        try {
            voice = MediaPlayer.create(appContext, resId);
            if (voice == null) {
                return false;
            }
            voice.setOnCompletionListener(mp -> {
                releaseVoice();
                if (callback != null) {
                    callback.onVoiceFinished();
                }
            });
            voice.start();
            return true;
        } catch (Exception e) {
            releaseVoice();
            return false;
        }
    }

    public void stopVoice() {
        if (voice != null) {
            try {
                if (voice.isPlaying()) {
                    voice.stop();
                }
            } catch (Exception ignored) {
            }
            releaseVoice();
        }
    }

    private void releaseVoice() {
        if (voice != null) {
            try {
                voice.release();
            } catch (Exception ignored) {
            }
            voice = null;
        }
    }

    public void startMusic(String name) {
        if (name == null || prefs.isMuted() || !prefs.musicEnabled()) {
            return;
        }
        if (music != null && name.equals(musicName)) {
            return;
        }
        stopMusic();
        int resId = resolve(name);
        if (resId == 0) {
            return;
        }
        try {
            music = MediaPlayer.create(appContext, resId);
            if (music == null) {
                return;
            }
            music.setLooping(true);
            music.setVolume(0.35f, 0.35f);
            music.start();
            musicName = name;
        } catch (Exception e) {
            stopMusic();
        }
    }

    public void stopMusic() {
        if (music != null) {
            try {
                if (music.isPlaying()) {
                    music.stop();
                }
                music.release();
            } catch (Exception ignored) {
            }
            music = null;
            musicName = null;
        }
    }

    private int resolve(String name) {
        try {
            return appContext.getResources().getIdentifier(
                    name, "raw", appContext.getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }
}
