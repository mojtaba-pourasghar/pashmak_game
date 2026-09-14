package ir.brandimo.pashmak.util;

import android.content.Context;
import android.provider.Settings;

/**
 * Honors the system "remove animations" setting, the Android counterpart of the
 * prototype's prefers-reduced-motion media query.
 */
public final class Motion {

    private Motion() {
    }

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
