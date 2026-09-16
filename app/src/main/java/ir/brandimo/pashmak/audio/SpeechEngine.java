package ir.brandimo.pashmak.audio;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Pashmak's voice, spoken by the device rather than played from a file.
 *
 * <p>Two things about this are worth knowing. First, it is pitched and paced to sound
 * like him — a small, warm creature talking to a three-year-old — not like a
 * navigation app. Second, <b>Persian is not a language most Android devices can
 * speak</b>: Google's engine does not ship it, so on many phones the only honest
 * answer is that there is no voice. {@link #status()} says which case a device is in
 * so the parent screen can explain it rather than leaving the character mute for no
 * visible reason.
 */
public final class SpeechEngine {

    /** Higher than life: a small furry animal, not a newsreader. */
    private static final float PITCH = 1.35f;
    /** A shade under normal, because the listener is three. */
    private static final float RATE = 0.92f;

    public enum Status {
        /** Still asking the device what it can do. */
        STARTING,
        /** A Persian voice is installed and Pashmak can talk. */
        READY,
        /** The engine works but has no Persian — the parent needs to install one. */
        NO_PERSIAN,
        /** No usable text-to-speech engine at all. */
        UNAVAILABLE
    }

    public interface DoneListener {
        void onSpeechFinished();
    }

    private static volatile SpeechEngine instance;

    private final Context appContext;
    /** UtteranceProgressListener fires on the engine's thread, not this one. */
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private Status status = Status.STARTING;
    private int utterance;

    @Nullable
    private DoneListener pending;

    private SpeechEngine(Context context) {
        appContext = context.getApplicationContext();
        start();
    }

    public static SpeechEngine get(@NonNull Context context) {
        SpeechEngine local = instance;
        if (local == null) {
            synchronized (SpeechEngine.class) {
                if (instance == null) {
                    instance = new SpeechEngine(context);
                }
                local = instance;
            }
        }
        return local;
    }

    private void start() {
        try {
            tts = new TextToSpeech(appContext, code -> {
                if (code != TextToSpeech.SUCCESS) {
                    status = Status.UNAVAILABLE;
                    return;
                }
                status = applyPersian() ? Status.READY : Status.NO_PERSIAN;
            });
        } catch (Exception e) {
            status = Status.UNAVAILABLE;
        }
    }

    private boolean applyPersian() {
        try {
            Locale persian = new Locale("fa", "IR");
            int result = tts.setLanguage(persian);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Some engines register the language without the country.
                result = tts.setLanguage(new Locale("fa"));
            }
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                return false;
            }
            tts.setPitch(PITCH);
            tts.setSpeechRate(RATE);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {
                }

                @Override
                public void onDone(String id) {
                    finish();
                }

                @Override
                public void onError(String id) {
                    finish();
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Status status() {
        return status;
    }

    public boolean isReady() {
        return status == Status.READY;
    }

    /** Speaks a line. Returns true only if the device actually took it. */
    public boolean say(@Nullable String text, @Nullable DoneListener listener) {
        if (text == null || text.trim().isEmpty() || !isReady()) {
            return false;
        }
        try {
            pending = listener;
            String id = "pashmak-" + (++utterance);
            int result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle params = new Bundle();
                result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id);
            } else {
                result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
            if (result != TextToSpeech.SUCCESS) {
                pending = null;
                return false;
            }
            return true;
        } catch (Exception e) {
            pending = null;
            return false;
        }
    }

    public boolean isSpeaking() {
        try {
            return tts != null && tts.isSpeaking();
        } catch (Exception e) {
            return false;
        }
    }

    public void stop() {
        pending = null;
        try {
            if (tts != null) {
                tts.stop();
            }
        } catch (Exception ignored) {
        }
    }

    private void finish() {
        final DoneListener listener = pending;
        pending = null;
        if (listener != null) {
            main.post(listener::onSpeechFinished);
        }
    }
}
