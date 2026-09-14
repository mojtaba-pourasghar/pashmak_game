package ir.brandimo.pashmak.ui.common;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

/**
 * One named, pickable stage — a coloring picture, a letter to trace, a memory
 * board. The same card shape the missions list uses, so every game is chosen the
 * same way.
 */
public final class Stage {

    public final int index;
    public final String title;
    public final String subtitle;
    /** Shown on the right, e.g. "۳ از ۵"; may be empty. */
    public final String progress;
    public final boolean complete;
    public final boolean locked;
    /** Optional picture for the number chip; 0 shows the number instead. */
    @DrawableRes
    public final int icon;
    /** Text for the chip when there is no icon. */
    @Nullable
    public final String badge;

    public Stage(int index, String title, String subtitle, String progress,
                 boolean complete, boolean locked, @DrawableRes int icon,
                 @Nullable String badge) {
        this.index = index;
        this.title = title;
        this.subtitle = subtitle;
        this.progress = progress;
        this.complete = complete;
        this.locked = locked;
        this.icon = icon;
        this.badge = badge;
    }
}
