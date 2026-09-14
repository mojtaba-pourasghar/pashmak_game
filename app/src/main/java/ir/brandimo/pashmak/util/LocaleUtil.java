package ir.brandimo.pashmak.util;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * The app is Persian-only, so it must not inherit the device's language or layout
 * direction. Without this, an English-locale device lays every screen out
 * left-to-right and the whole design mirrors the wrong way.
 */
public final class LocaleUtil {

    public static final Locale PERSIAN = new Locale("fa", "IR");

    private LocaleUtil() {
    }

    @NonNull
    public static Context persian(@NonNull Context base) {
        Locale.setDefault(PERSIAN);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(PERSIAN);
        config.setLayoutDirection(PERSIAN);
        return base.createConfigurationContext(config);
    }
}
