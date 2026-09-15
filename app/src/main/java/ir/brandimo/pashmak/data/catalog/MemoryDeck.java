package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** One themed set of cards: animals, fruit, letters, and so on. */
public final class MemoryDeck {

    /**
     * A single card face: a drawable, a Persian character, or — for the deck built
     * from the child's own scanned drawings — a cut-out PNG on disk.
     */
    public static final class Face {
        @DrawableRes
        public final int icon;
        @Nullable
        public final String glyph;
        /** Absolute path to a captured drawing, or null for the built-in decks. */
        @Nullable
        public final String path;
        public final int tint;

        Face(@DrawableRes int icon, @Nullable String glyph, @Nullable String path, int tint) {
            this.icon = icon;
            this.glyph = glyph;
            this.path = path;
            this.tint = tint;
        }

        public boolean isGlyph() {
            return glyph != null;
        }

        public boolean isPhoto() {
            return path != null;
        }
    }

    public final String id;
    public final String name;
    @DrawableRes
    public final int badge;
    public final List<Face> faces;

    /** Package-private: decks are built by MemoryCatalog, never by a screen. */
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
