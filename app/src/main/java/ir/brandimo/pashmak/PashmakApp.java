package ir.brandimo.pashmak;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import ir.brandimo.pashmak.audio.SoundBank;
import ir.brandimo.pashmak.data.prefs.GamePrefs;

public class PashmakApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // The artwork is a fixed light palette; a dark override would break it.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        GamePrefs.get(this);
        SoundBank.get(this);
    }
}
