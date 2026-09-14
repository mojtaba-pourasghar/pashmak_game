package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** One themed set of cards: animals, fruit, letters, and so on. */
public final class MemoryDeck {

    /** A single card face — either a picture or a Persian character. */
    public static final class Face {
        @DrawableRes
        public final int icon;
        @Nullable
        public final String glyph;
        public final int tint;

        Face(@DrawableRes int icon, @Nullable String glyph, int tint) {
            this.icon = icon;
            this.glyph = glyph;
            this.tint = tint;
        }

        public boolean isGlyph() {
            return glyph != null;
        }
    }

    public final String id;
    public final String name;
    @DrawableRes
    public final int badge;
    public final List<Face> faces;

    MemoryDeck(String id, String name, @DrawableRes int badge, List<Face> faces) {
        this.id = id;
        this.name = name;
        this.badge = badge;
        this.faces = Collections.unmodifiableList(faces);
    }

    public int size() {
        return faces.size();
    }
}
