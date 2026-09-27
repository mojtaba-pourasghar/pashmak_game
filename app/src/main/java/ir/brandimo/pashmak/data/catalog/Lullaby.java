package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/**
 * One lullaby. {@link #clip} is the res/raw file name of the recording; nothing
 * has to exist for the screen to work — a missing file is reported to the child
 * politely instead of crashing.
 */
public final class Lullaby {

    /** Raw resource name, e.g. "lullaby_mahtab_omade". */
    public final String clip;
    public final String title;
    @DrawableRes
    public final int icon;
    /** Running time in seconds, measured from the recording. */
    public final int seconds;

    Lullaby(@NonNull String clip, @NonNull String title,
            @DrawableRes int icon, int seconds) {
        this.clip = clip;
        this.title = title;
        this.icon = icon;
        this.seconds = seconds;
    }
}
