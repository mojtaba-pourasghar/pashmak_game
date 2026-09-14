package ir.brandimo.pashmak.data.catalog;

import android.content.Context;

import androidx.annotation.NonNull;

import ir.brandimo.pashmak.R;

/** The glyph set for tracing: the Persian alphabet, then the digits. */
public final class TraceCatalog {

    private static volatile String[] letters;
    private static volatile String[] digits;

    private TraceCatalog() {
    }

    @NonNull
    public static String[] letters(@NonNull Context context) {
        if (letters == null) {
            letters = context.getApplicationContext()
                    .getResources().getStringArray(R.array.trace_letters);
        }
        return letters;
    }

    @NonNull
    public static String[] digits(@NonNull Context context) {
        if (digits == null) {
            digits = context.getApplicationContext()
                    .getResources().getStringArray(R.array.trace_digits);
        }
        return digits;
    }

    public static String[] set(@NonNull Context context, boolean digitsMode) {
        return digitsMode ? digits(context) : letters(context);
    }
}
