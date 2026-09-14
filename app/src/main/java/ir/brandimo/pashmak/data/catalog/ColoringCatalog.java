package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ir.brandimo.pashmak.data.catalog.ColorRegion.Shape;

/**
 * The five coloring pages, transcribed from the prototype's PICS table. Positions
 * and sizes are the original CSS percentages expressed as 0..1 fractions.
 */
public final class ColoringCatalog {

    private static final List<ColoringPage> PAGES = build();

    private ColoringCatalog() {
    }

    @NonNull
    public static List<ColoringPage> all() {
        return PAGES;
    }

    @NonNull
    public static ColoringPage get(int index) {
        int clamped = Math.max(0, Math.min(index, PAGES.size() - 1));
        return PAGES.get(clamped);
    }

    public static int count() {
        return PAGES.size();
    }

    private static List<ColoringPage> build() {
        List<ColoringPage> pages = new ArrayList<>(5);

        pages.add(page("خونه",
                oval("sun", "خورشید", Palette.YELLOW, .70f, .04f, .20f, .16f),
                tri("roof", "سقف", Palette.RED, .10f, .16f, .76f, .24f),
                dp("wall", "دیوار", Palette.ORANGE, .17f, .40f, .62f, .46f, 4, 4, 4, 4),
                dp("door", "در", Palette.BROWN, .40f, .60f, .18f, .26f, 9, 9, 0, 0),
                dp("win", "پنجره", Palette.BLUE, .22f, .46f, .15f, .15f, 3, 3, 3, 3)));

        pages.add(page("ماهی",
                oval("body", "بدن", Palette.ORANGE, .20f, .32f, .52f, .34f),
                lead("tail", "دم", Palette.RED, .66f, .36f, .20f, .26f),
                tri("fin", "باله", Palette.PURPLE, .36f, .22f, .20f, .14f),
                dp("water", "آب", Palette.BLUE, .06f, .74f, .88f, .16f, 12, 12, 12, 12),
                oval("bub", "حباب", Palette.YELLOW, .12f, .12f, .13f, .13f)));

        pages.add(page("گل",
                oval("p1", "گلبرگ بالا", Palette.RED, .38f, .10f, .24f, .22f),
                oval("p2", "گلبرگ چپ", Palette.RED, .18f, .28f, .24f, .22f),
                oval("p3", "گلبرگ راست", Palette.RED, .58f, .28f, .24f, .22f),
                oval("mid", "وسط گل", Palette.YELLOW, .38f, .28f, .24f, .22f),
                dp("stem", "ساقه", Palette.GREEN, .46f, .50f, .08f, .40f, 6, 6, 6, 6),
                frac("leaf", "برگ", Palette.GREEN, .54f, .62f, .24f, .14f, 0f, .5f, 0f, .5f)));

        pages.add(page("ماشین",
                dp("top", "سقف", Palette.BLUE, .28f, .26f, .40f, .20f, 14, 14, 0, 0),
                dp("body", "بدنه", Palette.RED, .10f, .44f, .78f, .24f, 14, 14, 14, 14),
                oval("w1", "چرخ جلو", Palette.CHARCOAL, .20f, .62f, .17f, .17f),
                oval("w2", "چرخ عقب", Palette.CHARCOAL, .60f, .62f, .17f, .17f),
                dp("road", "جاده", Palette.BROWN, .04f, .82f, .92f, .09f, 6, 6, 6, 6)));

        pages.add(page("پروانه",
                frac("wtr", "بال بالا راست", Palette.PURPLE, .52f, .18f, .30f, .30f, .5f, .5f, .5f, 0f),
                frac("wtl", "بال بالا چپ", Palette.PURPLE, .18f, .18f, .30f, .30f, .5f, .5f, 0f, .5f),
                frac("wbr", "بال پایین راست", Palette.ORANGE, .52f, .50f, .28f, .26f, .5f, 0f, .5f, .5f),
                frac("wbl", "بال پایین چپ", Palette.ORANGE, .20f, .50f, .28f, .26f, 0f, .5f, .5f, .5f),
                pill("body", "بدن", Palette.CHARCOAL, .46f, .22f, .08f, .52f)));

        return Collections.unmodifiableList(pages);
    }

    private static ColoringPage page(String name, ColorRegion... regions) {
        return new ColoringPage(name, new ArrayList<>(Arrays.asList(regions)));
    }

    private static ColorRegion oval(String id, String label, int color,
                                    float l, float t, float w, float h) {
        return new ColorRegion(id, label, color, l, t, w, h, Shape.OVAL, new float[]{0, 0, 0, 0});
    }

    private static ColorRegion pill(String id, String label, int color,
                                    float l, float t, float w, float h) {
        return new ColorRegion(id, label, color, l, t, w, h, Shape.PILL, new float[]{0, 0, 0, 0});
    }

    private static ColorRegion tri(String id, String label, int color,
                                   float l, float t, float w, float h) {
        return new ColorRegion(id, label, color, l, t, w, h, Shape.TRIANGLE_UP, new float[]{0, 0, 0, 0});
    }

    private static ColorRegion lead(String id, String label, int color,
                                    float l, float t, float w, float h) {
        return new ColorRegion(id, label, color, l, t, w, h, Shape.TRIANGLE_LEAD, new float[]{0, 0, 0, 0});
    }

    private static ColorRegion dp(String id, String label, int color,
                                  float l, float t, float w, float h,
                                  float tl, float tr, float br, float bl) {
        return new ColorRegion(id, label, color, l, t, w, h, Shape.ROUND_DP,
                new float[]{tl, tr, br, bl});
    }

    private static ColorRegion frac(String id, String label, int color,
                                    float l, float t, float w, float h,
                                    float tl, float tr, float br, float bl) {
        return new ColorRegion(id, label, color, l, t, w, h, Shape.ROUND_FRACTION,
                new float[]{tl, tr, br, bl});
    }
}
