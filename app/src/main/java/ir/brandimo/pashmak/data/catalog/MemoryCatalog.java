package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.MemoryDeck.Face;

/**
 * Six themed decks and five levels each, so the memory game grows with the child
 * instead of being the same twelve cards every time — plus a seventh deck, built at
 * runtime from the drawings the child has scanned, which only exists once there are
 * enough of them.
 */
public final class MemoryCatalog {

    /** Pairs per level. Three pairs is gentle for a three-year-old; eight is a real test. */
    public static final int[] LEVEL_PAIRS = {3, 4, 5, 6, 8};

    /** The id of the deck made from the child's own drawings. */
    public static final String DRAWINGS_ID = "drawings";

    /** A board needs at least three pairs, so fewer drawings than this means no deck. */
    public static final int MIN_DRAWINGS = 3;

    /** At most eight faces, matching the largest level. */
    public static final int MAX_DRAWINGS = 8;

    private static final List<MemoryDeck> DECKS = build();

    /**
     * The authored decks; the drawings deck sits at this index when it exists.
     * Derived from DECKS so adding a themed deck cannot silently move it.
     */
    public static final int STATIC_COUNT = DECKS.size();

    @Nullable
    private static volatile MemoryDeck drawings;

    /** DECKS, plus the drawings deck when there is one. Rebuilt only when it changes. */
    private static volatile List<MemoryDeck> visible = DECKS;

    private MemoryCatalog() {
    }

    @NonNull
    public static List<MemoryDeck> all() {
        return visible;
    }

    @NonNull
    public static MemoryDeck deck(int index) {
        List<MemoryDeck> decks = visible;
        return decks.get(Palette.wrap(index, decks.size()));
    }

    public static int deckCount() {
        return visible.size();
    }

    /** True when this index is the drawings deck, whether or not it is loaded. */
    public static boolean isDrawings(int index) {
        return index == STATIC_COUNT;
    }

    public static boolean hasDrawings() {
        return drawings != null;
    }

    /**
     * Installs — or clears, with null — the deck made from the child's scanned
     * drawings. Called from the repository once the rows have been read off disk.
     */
    public static void setDrawings(@Nullable MemoryDeck deck) {
        drawings = deck;
        if (deck == null) {
            visible = DECKS;
            return;
        }
        List<MemoryDeck> combined = new ArrayList<>(DECKS);
        combined.add(deck);
        visible = Collections.unmodifiableList(combined);
    }

    /**
     * Builds the drawings deck from cut-out PNGs already on disk. Returns null when
     * there are too few for a board, so callers can keep the stages locked.
     */
    @Nullable
    public static MemoryDeck buildDrawings(@NonNull String name,
                                           @NonNull List<String> paths,
                                           @DrawableRes int badge) {
        if (paths.size() < MIN_DRAWINGS) {
            return null;
        }
        List<Face> faces = new ArrayList<>();
        for (int i = 0; i < paths.size() && i < MAX_DRAWINGS; i++) {
            faces.add(new Face(0, null, paths.get(i),
                    Palette.SWATCHES[i % Palette.SWATCHES.length]));
        }
        return new MemoryDeck(DRAWINGS_ID, name, badge, faces);
    }

    public static int levelCount() {
        return LEVEL_PAIRS.length;
    }

    public static int pairsForLevel(int level) {
        return LEVEL_PAIRS[Math.max(0, Math.min(level, LEVEL_PAIRS.length - 1))];
    }

    private static List<MemoryDeck> build() {
        List<MemoryDeck> decks = new ArrayList<>(6);

        decks.add(new MemoryDeck("animals", "حیوانات", R.drawable.face_cat, pictures(
                R.drawable.face_cat, R.drawable.face_dog, R.drawable.face_rabbit,
                R.drawable.face_bear, R.drawable.face_bird, R.drawable.face_fish,
                R.drawable.face_frog, R.drawable.face_elephant)));

        decks.add(new MemoryDeck("fruit", "میوه‌ها", R.drawable.face_apple, pictures(
                R.drawable.face_apple, R.drawable.face_banana, R.drawable.face_grape,
                R.drawable.face_orange, R.drawable.face_strawberry, R.drawable.face_watermelon,
                R.drawable.face_pear, R.drawable.face_cherry)));

        decks.add(new MemoryDeck("vehicles", "وسیله‌ها", R.drawable.face_car, pictures(
                R.drawable.face_car, R.drawable.face_bus, R.drawable.face_train,
                R.drawable.face_plane, R.drawable.face_boat, R.drawable.face_bike,
                R.drawable.face_rocket, R.drawable.face_truck)));

        decks.add(new MemoryDeck("shapes", "شکل‌ها", R.drawable.face_shape_star, pictures(
                R.drawable.face_shape_circle, R.drawable.face_shape_square,
                R.drawable.face_shape_triangle, R.drawable.face_shape_star,
                R.drawable.face_shape_heart, R.drawable.face_shape_diamond,
                R.drawable.face_shape_hexagon, R.drawable.face_shape_moon)));

        decks.add(new MemoryDeck("letters", "حرف‌ها", R.drawable.ic_pencil,
                glyphs("ا", "ب", "پ", "ت", "س", "م", "ن", "ی")));

        decks.add(new MemoryDeck("numbers", "عددها", R.drawable.ic_star,
                glyphs("۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸")));

        return Collections.unmodifiableList(decks);
    }

    private static List<Face> pictures(@DrawableRes int... icons) {
        List<Face> faces = new ArrayList<>(icons.length);
        for (int i = 0; i < icons.length; i++) {
            faces.add(new Face(icons[i], null, null,
                    Palette.SWATCHES[i % Palette.SWATCHES.length]));
        }
        return faces;
    }

    private static List<Face> glyphs(String... characters) {
        List<Face> faces = new ArrayList<>(characters.length);
        List<String> list = Arrays.asList(characters);
        for (int i = 0; i < list.size(); i++) {
            faces.add(new Face(0, list.get(i), null,
                    Palette.SWATCHES[i % Palette.SWATCHES.length]));
        }
        return faces;
    }
}
