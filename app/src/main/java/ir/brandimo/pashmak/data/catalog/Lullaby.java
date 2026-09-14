package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/**
 * One lullaby. {@link #clip} is the res/raw file name the recording will carry;
 * nothing has to exist for the screen to work — a missing file is reported to the
 * child politely instead of crashing.
 */
public final class Lullaby {

    /** Raw resource name, e.g. "lullaby_lay_lay". */
    public final String clip;
    public final String title;
    /** A line of the verse, so a pre-reader's grown-up can tell them apart. */
    public final String line;
    @DrawableRes
    public final int icon;
    /** Rough running time in seconds, shown before the file is dropped in. */
    public final int seconds;

    Lullaby(@NonNull String clip, @NonNull String title, @NonNull String line,
            @DrawableRes int icon, int seconds) {
        this.clip = clip;
        this.title = title;
        this.line = line;
        this.icon = icon;
        this.seconds = seconds;
    }
}
