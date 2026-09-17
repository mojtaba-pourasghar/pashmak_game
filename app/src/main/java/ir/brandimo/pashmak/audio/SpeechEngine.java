package ir.brandimo.pashmak.audio;

import android.content.Context;
import android.content.Intent;
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
 * <p><b>Why this is not simply "ask the default engine".</b> The engine a phone comes
 * set to is often the manufacturer's — Samsung's or Xiaomi's — and those do not speak
 * Persian. Asking only that one and giving up is how the app ended up mute on devices
 * that had a perfectly good Persian voice installed under a different engine. Android
 * lets an app name the engine it wants, so every installed engine is tried in turn:
 * Google's first, because it is the one most likely to be there and to have language
 * packs, then the system default, then everything else. The first that can speak
 * Persian is the one Pashmak uses, and the device-wide default is never touched.
 *
 * <p>Nothing here blocks the child. The search runs in the background, the words are
 * on screen either way, and a recording in res/raw always wins over synthesis — see
 * {@link VoicePlayer}. If no engine can be found the app simply carries on quietly.
 */
public final class SpeechEngine {

    /**
     * A friendly boyish bear: a little above a grown-up's pitch and a little under
     * their pace, because the listener is three and the speaker is small and warm.
     */
    private static final float PITCH = 1.2f;
    private static final float RATE = 0.92f;

    /** Asked first: the most widely installed engine, and the one with language packs. */
    public static final String GOOGLE_TTS = "com.google.android.tts";

    /**
     * Persian answers to more than one code. fa-IR is the full tag, fas is the
     * ISO 639-2 form some engines register under, and bare fa is what the rest use.
     */
    private static final Locale[] PERSIAN = {
            new Locale("fa", "IR"), new Locale("fas"), new Locale("fa")
    };

    public enum Status {
        /** Still asking the device what it can do. */
        STARTING,
        /** A Persian voice was found and Pashmak can talk. */
        READY,
        /**
         * An engine knows Persian but has not downloaded it. One tap fixes this, so
         * it is deliberately not the same answer as "this device cannot speak Persian".
         */
        NEEDS_DATA,
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
    /** Whether any engine started at all, whatever it could speak. */
    private boolean anyEngineWorks;
    /** An engine that knows Persian but is missing the data for it. */
    private String engineNeedingData;

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
     * Starts the search again. Called when a screen comes back, because a voice may
     * have been installed or downloaded while the parent was away.
     */
    public void restart() {
        shutdown();
        status = Status.STARTING;
        anyEngineWorks = false;
        engineNeedingData = null;
        queue = new ArrayList<>();
        queue.add(GOOGLE_TTS);      // asked first, by name
        queue.add(null);            // then whatever the device is set to
        tryNext();
    }

    private void tryNext() {
        if (queue.isEmpty()) {
            settle(finalVerdict());
            return;
        }
        final String engine = queue.remove(0);
        try {
            TextToSpeech.OnInitListener init = code -> {
                if (code != TextToSpeech.SUCCESS) {
                    step();
                    return;
                }
                anyEngineWorks = true;
                if (engine == null) {
                    // The default engine started, so now we can see the rest.
                    collectEngines();
                }
                if (applyPersian(engine)) {
                    voiceEngine = installed(engine) ? engine : safeDefaultEngine();
                    settle(Status.READY);
                } else {
                    step();
                }
            };
            tts = engine == null
                    ? new TextToSpeech(appContext, init)
                    : new TextToSpeech(appContext, init, engine);
        } catch (Exception e) {
            // Naming an engine that is not installed throws; that is not an error,
            // it is the answer to the question we asked.
            step();
        }
    }

    /** This one cannot do it; let it go and ask the next. */
    private void step() {
        try {
            if (tts != null) {
                tts.shutdown();
            }
        } catch (Exception ignored) {
        }
        tts = null;
        if (queue.isEmpty()) {
            settle(finalVerdict());
            return;
        }
        main.post(this::tryNext);
    }

    /**
     * Which of the three unhappy endings this is. They are told apart because the
     * remedy differs: a download, an install, or nothing the parent can do.
     */
    private Status finalVerdict() {
        if (engineNeedingData != null) {
            return Status.NEEDS_DATA;
        }
        return anyEngineWorks ? Status.NO_PERSIAN : Status.UNAVAILABLE;
    }

    /** Every other engine installed on the device, so each gets asked in turn. */
    private void collectEngines() {
        try {
            for (TextToSpeech.EngineInfo info : tts.getEngines()) {
                if (info == null || info.name == null) {
                    continue;
                }
                if (GOOGLE_TTS.equals(info.name) || queue.contains(info.name)) {
                    continue;          // already asked, or already queued
                }
                queue.add(info.name);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Whether a named engine is really on the device.
     *
     * <p>Asking for an engine that is not installed does not always fail: some
     * versions quietly hand back the default one instead. Without this check the
     * screen would report that Pashmak speaks through Google when he does not.
     */
    private boolean installed(@Nullable String engine) {
        if (engine == null) {
            return false;
        }
        try {
            appContext.getPackageManager().getPackageInfo(engine, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    private String safeDefaultEngine() {
        try {
            return tts == null ? null : tts.getDefaultEngine();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Asks the engine in hand whether it can say Persian, and sets it up if so.
     *
     * <p>{@code isLanguageAvailable} is used to ask rather than {@code setLanguage},
     * because asking should not change anything until the answer is yes. An engine
     * that answers MISSING_DATA is remembered: it knows the language and only wants
     * the pack, which is a one-tap errand rather than a dead end.
     */
    private boolean applyPersian(@Nullable String engine) {
        try {
            Locale chosen = null;
            for (Locale locale : PERSIAN) {
                int answer = tts.isLanguageAvailable(locale);
                if (answer == TextToSpeech.LANG_AVAILABLE
                        || answer == TextToSpeech.LANG_COUNTRY_AVAILABLE
                        || answer == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                    chosen = locale;
                    break;
                }
                if (answer == TextToSpeech.LANG_MISSING_DATA
                        && engineNeedingData == null) {
                    // This engine knows Persian and only wants the pack. Remember
                    // which one, so the download can be pointed at it by name.
                    engineNeedingData = installed(engine) ? engine : safeDefaultEngine();
                }
            }
            if (chosen == null) {
                return false;
            }
            if (isMissing(tts.setLanguage(chosen))) {
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

    /**
     * The intent that downloads an engine's missing language data.
     *
     * <p>It is handed back rather than fired here on purpose. It opens another app's
     * screen, and doing that by itself would throw a three-year-old out of the middle
     * of a story. The parent screen fires it when a grown-up asks for it, which is a
     * single tap and no hunting through Android's settings.
     */
    @Nullable
    public Intent voiceDataIntent() {
        if (status != Status.NEEDS_DATA) {
            return null;
        }
        Intent install = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (engineNeedingData != null) {
            install.setPackage(engineNeedingData);
        }
        return install.resolveActivity(appContext.getPackageManager()) == null
                ? null : install;
    }

    public Status status() {
        return status;
    }

    /** Which engine is doing the talking, or null when nothing is. */
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

    private void settle(Status next) {
        status = next;
        final Status settled = next;
        main.post(() -> {
            for (int i = 0; i < watchers.size(); i++) {
                watchers.get(i).onSpeechStatus(settled);
            }
        });
    }

    private void finish() {
        final DoneListener listener = pending;
        pending = null;
        if (listener != null) {
            main.post(listener::onSpeechFinished);
        }
    }
}
