package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;

/** Something in a story scene the child can point at. */
public final class StoryProp {

    public final String id;
    public final String label;
    @DrawableRes
    public final int icon;
    /** Position and size as fractions of the scene, so it scales to any screen. */
    public final float x;
    public final float y;
    public final float size;

    public StoryProp(String id, String label, @DrawableRes int icon,
                     float x, float y, float size) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.size = size;
    }
}
