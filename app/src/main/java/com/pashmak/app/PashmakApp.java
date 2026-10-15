package com.pashmak.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.pashmak.app.audio.MusicEngine;
import com.pashmak.app.audio.SoundBank;
import com.pashmak.app.data.prefs.GamePrefs;
import com.pashmak.app.util.LocaleUtil;

public class PashmakApp extends Application {

    /** Screens currently visible; music stops only when the last one goes away. */
    private int visibleActivities;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.persian(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // The artwork is a fixed light palette; a dark override would break it.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        GamePrefs.get(this);
        SoundBank.get(this);
        watchForBackground();
    }

    /**
     * Music is owned by the app rather than by any one screen, so moving between
     * screens never restarts it — but leaving the app does stop it.
     */
    private void watchForBackground() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                visibleActivities++;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                visibleActivities--;
                if (visibleActivities <= 0) {
                    visibleActivities = 0;
                    MusicEngine.get(PashmakApp.this).stop();
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle s) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }
}
