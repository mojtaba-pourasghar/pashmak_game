package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;

import java.util.Collections;
import java.util.List;

/**
 * The world a mission's drawings live in: a two-band backdrop, a little scenery
 * of its own, and a place for each drawing to land.
 */
public final class MissionScene {

    /** Where one captured drawing settles, in fractions of the scene. */
    public static final class Anchor {
        public final float x;
        public final float y;
        public final float scale;
        public final float rotation;

        Anchor(float x, float y, float scale, float rotation) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rotation = rotation;
        }
    }

    /** Scenery drawn behind the child's work — never something they must draw. */
    public static final class Decor {
        @DrawableRes
        public final int icon;
        public final float x;
        public final float y;
        public final float size;

        Decor(@DrawableRes int icon, float x, float y, float size) {
            this.icon = icon;
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }

    public final int skyColor;
    public final int groundColor;
    /** Height of the sky band, as a fraction of the scene. */
    public final float horizon;
    public final List<Decor> decor;
    public final List<Anchor> anchors;

    MissionScene(int skyColor, int groundColor, float horizon,
                 List<Decor> decor, List<Anchor> anchors) {
        this.skyColor = skyColor;
        this.groundColor = groundColor;
        this.horizon = horizon;
        this.decor = Collections.unmodifiableList(decor);
        this.anchors = Collections.unmodifiableList(anchors);
    }

    public Anchor anchor(int slot) {
        if (anchors.isEmpty()) {
            return new Anchor(.5f, .5f, .3f, 0f);
        }
        return anchors.get(Math.max(0, Math.min(slot, anchors.size() - 1)));
    }
}
