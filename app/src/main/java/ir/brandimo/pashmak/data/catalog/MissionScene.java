package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;

import java.util.Collections;
import java.util.List;

/**
 * The world a mission's drawings live in — an actual drawn place, not a backdrop:
 * the bedroom has a wall, a floor, a skirting board and a bedside table; the
 * kitchen has tiled walls and a counter. The child's scanned drawings then land
 * in the spots reserved for them.
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

    /** A piece of the room itself, in fractions of the scene. */
    public static final class Shape {

        public enum Kind {
            RECT, ROUND, OVAL, TRIANGLE_UP, TRIANGLE_DOWN
        }

        public final Kind kind;
        public final int color;
        public final float x;
        public final float y;
        public final float width;
        public final float height;
        /** Corner radius as a fraction of the shape's shorter side. */
        public final float radius;

        Shape(Kind kind, int color, float x, float y,
              float width, float height, float radius) {
            this.kind = kind;
            this.color = color;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.radius = radius;
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
    /** The room's own furniture and fittings, drawn under the child's work. */
    public final List<Shape> shapes;
    public final List<Decor> decor;
    public final List<Anchor> anchors;

    MissionScene(int skyColor, int groundColor, float horizon, List<Shape> shapes,
                 List<Decor> decor, List<Anchor> anchors) {
        this.skyColor = skyColor;
        this.groundColor = groundColor;
        this.horizon = horizon;
        this.shapes = Collections.unmodifiableList(shapes);
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
