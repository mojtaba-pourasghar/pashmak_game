package ir.brandimo.pashmak.data.catalog;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

/** The shared kid-facing color palette, used by coloring, free draw and slots. */
public final class Palette {

    public static final int RED = Color.parseColor("#E1251B");
    public static final int ORANGE = Color.parseColor("#F7941D");
    public static final int YELLOW = Color.parseColor("#FFC730");
    public static final int GREEN = Color.parseColor("#43B02A");
    public static final int BLUE = Color.parseColor("#1FA6D6");
    public static final int PURPLE = Color.parseColor("#7A4FA3");
    public static final int BROWN = Color.parseColor("#8A5A2B");
    /**
     * The prototype used this ink for car wheels and the butterfly body but left it
     * out of the palette, which made those two pages impossible to finish. It is a
     * swatch here so every page is completable.
     */
    public static final int CHARCOAL = Color.parseColor("#28313A");

    public static final int[] SWATCHES = {
            RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, BROWN, CHARCOAL
    };

    /** Free-drawing offers the brighter half of the palette. */
    public static final int[] BRUSH_COLORS = {
            RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, BROWN, CHARCOAL
    };

    /** Item slot hues, indexed by position within a mission (HUES in the prototype). */
    public static final int[] HUE_FILL = {
            Color.parseColor("#EDE3F6"), Color.parseColor("#FFE6D2"),
            Color.parseColor("#DFF0D6"), Color.parseColor("#D9EDF7")
    };
    public static final int[] HUE_BORDER = {
            Color.parseColor("#C8B6DC"), Color.parseColor("#F0B98E"),
            Color.parseColor("#A9D294"), Color.parseColor("#96C9E2")
    };

    private Palette() {
    }

    public static int hueFill(int slot) {
        return HUE_FILL[wrap(slot, HUE_FILL.length)];
    }

    public static int hueBorder(int slot) {
        return HUE_BORDER[wrap(slot, HUE_BORDER.length)];
    }

    /**
     * A light wash of a swatch, for surfaces that sit behind the artwork. The
     * pictures are drawn in these same saturated colours, so a red apple on a red
     * card disappears; on a pale red one it reads at a glance.
     */
    public static int pale(int color) {
        return ColorUtils.blendARGB(color, Color.WHITE, 0.82f);
    }

    /** Math.floorMod is API 24+, and this app ships to API 21. */
    public static int wrap(int value, int length) {
        if (length <= 0) {
            return 0;
        }
        return ((value % length) + length) % length;
    }
}
