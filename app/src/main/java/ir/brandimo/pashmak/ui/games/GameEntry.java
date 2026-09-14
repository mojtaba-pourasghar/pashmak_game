package ir.brandimo.pashmak.ui.games;

import android.app.Activity;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** One tile on the games menu. */
public final class GameEntry {

    @DrawableRes
    public final int background;
    @DrawableRes
    public final int icon;
    @ColorRes
    public final int iconTint;
    @StringRes
    public final int title;
    public final String subtitle;
    /** True for the headline card, which spans the whole row. */
    public final boolean wide;
    /** Dark text for the pale cards, white for the rest. */
    public final boolean darkText;
    public final Class<? extends Activity> destination;

    public GameEntry(@DrawableRes int background, @DrawableRes int icon, @ColorRes int iconTint,
                     @StringRes int title, String subtitle, boolean wide, boolean darkText,
                     Class<? extends Activity> destination) {
        this.background = background;
        this.icon = icon;
        this.iconTint = iconTint;
        this.title = title;
        this.subtitle = subtitle;
        this.wide = wide;
        this.darkText = darkText;
        this.destination = destination;
    }
}
