package ir.brandimo.pashmak.audio;

import android.content.Context;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.brandimo.pashmak.data.prefs.GamePrefs;

/**
 * Mascot voice lines. Mirrors the prototype's behavior of failing silently when a
 * clip has not been recorded yet; background music lives in MusicEngine.
 */
public final class VoicePlayer {

    private static volatile VoicePlayer instance;

    private final Context appContext;
    private final GamePrefs prefs;

    private MediaPlayer voice;

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

    /** Whether a mascot clip is currently sounding. */
    public boolean isSpeaking() {
        try {
            return voice != null && voice.isPlaying();
        } catch (Exception e) {
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

    private int resolve(String name) {
        try {
            return appContext.getResources().getIdentifier(
                    name, "raw", appContext.getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }
}
