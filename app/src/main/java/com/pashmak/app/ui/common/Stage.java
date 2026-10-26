package com.pashmak.app.ui.common;

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
    /**
     * Locked because it is part of the full version, not because of the child's
     * progress. Tapping it offers the purchase instead of "finish the last one".
     */
    public final boolean forSale;

    public Stage(int index, String title, String subtitle, String progress,
                 boolean complete, boolean locked, @DrawableRes int icon,
                 @Nullable String badge) {
        this(index, title, subtitle, progress, complete, locked, icon, badge, false);
    }

    private Stage(int index, String title, String subtitle, String progress,
                  boolean complete, boolean locked, @DrawableRes int icon,
                  @Nullable String badge, boolean forSale) {
        this.index = index;
        this.title = title;
        this.subtitle = subtitle;
        this.progress = progress;
        this.complete = complete;
        this.locked = locked;
        this.icon = icon;
        this.badge = badge;
        this.forSale = forSale;
    }

    /** The same stage, shut until the full version is bought. */
    public Stage forSale() {
        return new Stage(index, title, subtitle, progress, complete, true, icon, badge, true);
    }
}
