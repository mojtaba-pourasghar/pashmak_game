package ir.brandimo.pashmak.audio;

import android.content.Context;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.brandimo.pashmak.data.prefs.GamePrefs;

/**
 * Everything Pashmak says out loud.
 *
 * <p>Two sources, in order: a recording dropped into res/raw under the line's clip
 * name wins if it is there, so a real voice can always be substituted later; if it is
 * not, the device speaks the words through {@link SpeechEngine}. That way the app has
 * a voice out of the box without shipping hundreds of files, and a recorded one the
 * moment anybody records it.
 */
public final class VoicePlayer {

    private static volatile VoicePlayer instance;

    private final Context appContext;
    private final GamePrefs prefs;
    private final SpeechEngine speech;

    private MediaPlayer voice;

    private VoicePlayer(Context context) {
        appContext = context.getApplicationContext();
        prefs = GamePrefs.get(appContext);
        speech = SpeechEngine.get(appContext);
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

    /**
     * Says a line. Returns true when something actually sounded, so callers can hold
     * the mascot's mouth open for as long as it lasts.
     *
     * @param name the clip name to look for in res/raw, or null to go straight to speech
     * @param text the words themselves, spoken when there is no recording
     */
    public boolean speak(@Nullable String name, @Nullable String text,
                         @Nullable CompletionCallback callback) {
        stopVoice();
        if (prefs.isMuted() || !prefs.soundEnabled()) {
            return false;
        }
        if (playRecording(name, callback)) {
            return true;
        }
        return speech.say(text, callback == null ? null : callback::onVoiceFinished);
    }

    private boolean playRecording(@Nullable String name,
                                  @Nullable CompletionCallback callback) {
        if (name == null) {
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

    /** Whether a mascot line is currently sounding, from either source. */
    public boolean isSpeaking() {
        try {
            if (voice != null && voice.isPlaying()) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return speech.isSpeaking();
    }

    public void stopVoice() {
        speech.stop();
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
