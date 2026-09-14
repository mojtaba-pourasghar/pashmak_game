package ir.brandimo.pashmak.util;

import android.content.Context;
import android.provider.Settings;

/**
 * Honors the system "remove animations" setting — but only for the big, sweeping
 * motion. A character that never blinks or moves its mouth reads as broken rather
 * than calm, and animations are switched off by default on many emulators, so the
 * small signs of life always play.
 */
public final class Motion {

    private Motion() {
    }

    /** True when the user has asked for less movement on screen. */
    public static boolean reduced(Context context) {
        if (context == null) {
            return false;
        }
        try {
            float scale = Settings.Global.getFloat(
                    context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            return scale == 0f;
        } catch (Exception e) {
            return false;
        }
    }
}
