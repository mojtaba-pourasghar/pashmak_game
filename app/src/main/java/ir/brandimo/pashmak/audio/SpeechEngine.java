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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pashmak's voice, spoken by the device rather than played from a file.
 *
 * <p>Two things about this are worth knowing. First, it is pitched and paced to sound
 * like him — a small, warm creature talking to a three-year-old — not like a
 * navigation app. Second, <b>Persian is not a language most Android devices can
 * speak</b>: Google's engine, which is the default almost everywhere, does not ship
 * it.
 *
 * <p>So this does not simply ask the default engine and give up. Android lets an app
 * name the engine it wants, and a device often has one installed that the system
 * default is not — so every installed engine is tried in turn, and the first that can
 * speak Persian is the one Pashmak uses. The parent does not have to go into Android
 * settings and change the device-wide default for a children's app.
 *
 * <p>When none of them can, {@link #status()} says so and the parent screen offers to
 * install one. {@link #restart()} picks it up as soon as it is there.
 */
public final class SpeechEngine {

    /** Higher than life: a small furry animal, not a newsreader. */
    private static final float PITCH = 1.35f;
    /** A shade under normal, because the listener is three. */
    private static final float RATE = 0.92f;

    public enum Status {
        /** Still asking the device what it can do. */
        STARTING,
        /** A Persian voice was found and Pashmak can talk. */
        READY,
        /** Engines are installed but none of them speaks Persian. */
        NO_PERSIAN,
        /** No usable text-to-speech engine at all. */
        UNAVAILABLE
    }

    public interface DoneListener {
        void onSpeechFinished();
    }

    /** Told when the search finishes, so a screen can stop saying "getting ready". */
    public interface StatusListener {
        void onSpeechStatus(Status status);
    }

    private static volatile SpeechEngine instance;

    private final Context appContext;
    /** UtteranceProgressListener fires on the engine's thread, not this one. */
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<StatusListener> watchers = new ArrayList<>();

    private TextToSpeech tts;
    private Status status = Status.STARTING;
    private String voiceEngine;
    private int utterance;

    /** Engine packages still to try, in order. */
    private List<String> queue = new ArrayList<>();
    /** Whether the device has a working engine at all, whatever it can speak. */
    private boolean anyEngineWorks;

    @Nullable
    private DoneListener pending;

    private SpeechEngine(Context context) {
        appContext = context.getApplicationContext();
        restart();
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

    /**
     * Starts the search again. Called when the screen comes back, because the parent
     * may have installed an engine while they were away.
     */
    public void restart() {
        shutdown();
        status = Status.STARTING;
        anyEngineWorks = false;
        queue = new ArrayList<>();
        // The system default first: if it can do Persian, nothing else need be asked.
        queue.add(null);
        tryNext();
    }

    private void tryNext() {
        if (queue.isEmpty()) {
            settle(Status.NO_PERSIAN);
            return;
        }
        final String engine = queue.remove(0);
        try {
            TextToSpeech.OnInitListener init = code -> {
                if (code != TextToSpeech.SUCCESS) {
                    step(engine);
                    return;
                }
                anyEngineWorks = true;
                if (engine == null) {
                    // First time through: now we know what else is installed.
                    collectEngines();
                }
                if (applyPersian()) {
                    voiceEngine = engine;
                    settle(Status.READY);
                } else {
                    step(engine);
                }
            };
            tts = engine == null
                    ? new TextToSpeech(appContext, init)
                    : new TextToSpeech(appContext, init, engine);
        } catch (Exception e) {
            step(engine);
        }
    }

    /** This one cannot do it; let it go and ask the next. */
    private void step(@Nullable String failed) {
        try {
            if (tts != null) {
                tts.shutdown();
            }
        } catch (Exception ignored) {
        }
        tts = null;
        if (queue.isEmpty()) {
            // Nothing left to try. Which of the two bad endings it is matters: one
            // asks the parent to install a voice, the other says the device cannot
            // speak at all, and telling them the wrong one wastes their time.
            settle(anyEngineWorks ? Status.NO_PERSIAN : Status.UNAVAILABLE);
            return;
        }
        main.post(this::tryNext);
    }

    /** Every other engine installed on the device, so each gets asked in turn. */
    private void collectEngines() {
        try {
            String alreadyTried = tts.getDefaultEngine();
            for (TextToSpeech.EngineInfo info : tts.getEngines()) {
                if (info == null || info.name == null) {
                    continue;
                }
                if (info.name.equals(alreadyTried) || queue.contains(info.name)) {
                    continue;
                }
                queue.add(info.name);
            }
        } catch (Exception ignored) {
        }
    }

    private void settle(Status next) {
        status = next;
        final Status settled = next;
        main.post(() -> {
            for (int i = 0; i < watchers.size(); i++) {
                watchers.get(i).onSpeechStatus(settled);
            }
        });
    }

    private boolean applyPersian() {
        try {
            int result = tts.setLanguage(new Locale("fa", "IR"));
            if (isMissing(result)) {
                // Some engines register the language without the country.
                result = tts.setLanguage(new Locale("fa"));
            }
            if (isMissing(result)) {
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

    private static boolean isMissing(int result) {
        return result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED;
    }

    public Status status() {
        return status;
    }

    /** Which engine is doing the talking, or null for the system default. */
    @Nullable
    public String voiceEngine() {
        return voiceEngine;
    }

    public void watch(@NonNull StatusListener listener) {
        if (!watchers.contains(listener)) {
            watchers.add(listener);
        }
    }

    public void unwatch(@NonNull StatusListener listener) {
        watchers.remove(listener);
    }

    public boolean isReady() {
        return status == Status.READY;
    }

    /** Speaks a line. Returns true only if the device actually took it. */
    public boolean say(@Nullable String text, @Nullable DoneListener listener) {
        if (text == null || text.trim().isEmpty() || !isReady() || tts == null) {
            return false;
        }
        try {
            pending = listener;
            String id = "pashmak-" + (++utterance);
            int result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, new Bundle(), id);
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

    private void shutdown() {
        stop();
        try {
            if (tts != null) {
                tts.shutdown();
            }
        } catch (Exception ignored) {
        }
        tts = null;
        voiceEngine = null;
    }

    private void finish() {
        final DoneListener listener = pending;
        pending = null;
        if (listener != null) {
            main.post(listener::onSpeechFinished);
        }
    }
}
