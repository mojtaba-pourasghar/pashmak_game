package ir.brandimo.pashmak.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import ir.brandimo.pashmak.data.prefs.GamePrefs;

/** Low-latency effects. Unknown or missing sounds are ignored, never an error. */
public final class SoundBank {

    private static final int MAX_STREAMS = 6;

    private static volatile SoundBank instance;

    private final Context appContext;
    private final GamePrefs prefs;
    private final SoundPool pool;
    private final Map<String, Integer> loaded = new HashMap<>();

    private SoundBank(Context context) {
        appContext = context.getApplicationContext();
        prefs = GamePrefs.get(appContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            pool = new SoundPool.Builder()
                    .setMaxStreams(MAX_STREAMS)
                    .setAudioAttributes(attrs)
                    .build();
        } else {
            pool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        }
        preload();
    }

    public static SoundBank get(@NonNull Context context) {
        SoundBank local = instance;
        if (local == null) {
            synchronized (SoundBank.class) {
                if (instance == null) {
                    instance = new SoundBank(context);
                }
                local = instance;
            }
        }
        return local;
    }

    private void preload() {
        for (String name : AudioManifest.PRELOAD_SFX) {
            int resId = resolve(name);
            if (resId != 0) {
                try {
                    loaded.put(name, pool.load(appContext, resId, 1));
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void play(String name) {
        play(name, 1f);
    }

    public void play(String name, float rate) {
        if (name == null || prefs.isMuted() || !prefs.soundEnabled()) {
            return;
        }
        Integer soundId = loaded.get(name);
        if (soundId == null) {
            int resId = resolve(name);
            if (resId == 0) {
                return;
            }
            try {
                soundId = pool.load(appContext, resId, 1);
                loaded.put(name, soundId);
            } catch (Exception e) {
                return;
            }
        }
        try {
            pool.play(soundId, 1f, 1f, 1, 0, Math.max(0.5f, Math.min(2f, rate)));
        } catch (Exception ignored) {
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
