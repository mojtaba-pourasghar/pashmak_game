package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ir.brandimo.pashmak.data.catalog.ColorRegion.Shape;

/**
 * Sixty coloring pages in twelve themed packs. Geometry is expressed as fractions
 * of the page, exactly as the original design did in CSS percentages, so every
 * picture fills whatever canvas it is given.
 */
public final class ColoringCatalog {

    private static final List<ColorPack> PACKS = build();

    private ColoringCatalog() {
    }

    @NonNull
    public static List<ColorPack> packs() {
        return PACKS;
    }

    @NonNull
    public static ColorPack pack(int index) {
        return PACKS.get(Palette.wrap(index, PACKS.size()));
    }

    public static int packCount() {
        return PACKS.size();
    }

    /** Every page, flattened — used where a pack is not relevant. */
    @NonNull
    public static List<ColoringPage> allPages() {
        List<ColoringPage> pages = new ArrayList<>();
        for (int i = 0; i < PACKS.size(); i++) {
            pages.addAll(PACKS.get(i).pages);
        }
        return pages;
    }

    private static List<ColorPack> build() {
        List<ColorPack> packs = new ArrayList<>();

        packs.add(new ColorPack("animals", "حیوانات", pages(
                page("گربه",
                        oval("head", "سر", Palette.ORANGE, 0.30f, 0.28f, 0.40f, 0.38f),
                        tri("ear1", "گوش راست", Palette.ORANGE, 0.30f, 0.12f, 0.14f, 0.18f),
                        tri("ear2", "گوش چپ", Palette.ORANGE, 0.56f, 0.12f, 0.14f, 0.18f),
                        oval("eye1", "چشم راست", Palette.CHARCOAL, 0.38f, 0.40f, 0.07f, 0.08f),
                        oval("eye2", "چشم چپ", Palette.CHARCOAL, 0.55f, 0.40f, 0.07f, 0.08f),
                        tri("nose", "بینی", Palette.RED, 0.46f, 0.52f, 0.08f, 0.07f),
                        pill("tail", "دم", Palette.BROWN, 0.68f, 0.62f, 0.24f, 0.07f)),
                page("ماهی",
                        oval("body", "بدن", Palette.ORANGE, 0.20f, 0.32f, 0.50f, 0.34f),
                        lead("tail", "دم", Palette.RED, 0.66f, 0.34f, 0.20f, 0.30f),
                        tri("fin", "باله", Palette.PURPLE, 0.34f, 0.20f, 0.18f, 0.14f),
                        oval("eye", "چشم", Palette.CHARCOAL, 0.28f, 0.42f, 0.07f, 0.08f),
                        dp("water", "آب", Palette.BLUE, 0.04f, 0.76f, 0.92f, 0.16f, 12f, 12f, 12f, 12f),
                        oval("bubble", "حباب", Palette.YELLOW, 0.14f, 0.12f, 0.10f, 0.11f)),
                page("پرنده",
                        oval("body", "بدن", Palette.BLUE, 0.24f, 0.34f, 0.40f, 0.34f),
                        oval("head", "سر", Palette.BLUE, 0.56f, 0.24f, 0.22f, 0.22f),
                        lead("beak", "نوک", Palette.YELLOW, 0.74f, 0.30f, 0.14f, 0.10f),
                        oval("wing", "بال", Palette.PURPLE, 0.34f, 0.44f, 0.22f, 0.16f),
                        oval("eye", "چشم", Palette.CHARCOAL, 0.64f, 0.30f, 0.06f, 0.07f),
                        pill("leg", "پا", Palette.BROWN, 0.40f, 0.66f, 0.05f, 0.20f)),
                page("خرس",
                        oval("head", "سر", Palette.BROWN, 0.28f, 0.24f, 0.44f, 0.42f),
                        oval("ear1", "گوش راست", Palette.BROWN, 0.26f, 0.14f, 0.16f, 0.16f),
                        oval("ear2", "گوش چپ", Palette.BROWN, 0.58f, 0.14f, 0.16f, 0.16f),
                        oval("muzzle", "پوزه", Palette.ORANGE, 0.40f, 0.46f, 0.20f, 0.16f),
                        oval("nose", "بینی", Palette.CHARCOAL, 0.46f, 0.48f, 0.08f, 0.07f),
                        oval("eye1", "چشم راست", Palette.CHARCOAL, 0.37f, 0.36f, 0.06f, 0.07f),
                        oval("eye2", "چشم چپ", Palette.CHARCOAL, 0.57f, 0.36f, 0.06f, 0.07f)),
                page("لاک‌پشت",
                        oval("shell", "لاک", Palette.GREEN, 0.24f, 0.34f, 0.48f, 0.34f),
                        oval("head", "سر", Palette.YELLOW, 0.70f, 0.40f, 0.16f, 0.18f),
                        oval("eye", "چشم", Palette.CHARCOAL, 0.78f, 0.44f, 0.05f, 0.06f),
                        dp("leg1", "پای جلو", Palette.YELLOW, 0.28f, 0.64f, 0.14f, 0.12f, 6f, 6f, 6f, 6f),
                        dp("leg2", "پای عقب", Palette.YELLOW, 0.54f, 0.64f, 0.14f, 0.12f, 6f, 6f, 6f, 6f),
                        oval("spot", "خال", Palette.BROWN, 0.42f, 0.42f, 0.14f, 0.14f))
)));

        packs.add(new ColorPack("vehicles", "وسیله‌ها", pages(
                page("ماشین",
                        dp("top", "سقف", Palette.BLUE, 0.28f, 0.26f, 0.40f, 0.20f, 14f, 14f, 0f, 0f),
                        dp("body", "بدنه", Palette.RED, 0.10f, 0.44f, 0.78f, 0.24f, 14f, 14f, 14f, 14f),
                        oval("w1", "چرخ جلو", Palette.CHARCOAL, 0.20f, 0.62f, 0.17f, 0.17f),
                        oval("w2", "چرخ عقب", Palette.CHARCOAL, 0.60f, 0.62f, 0.17f, 0.17f),
                        dp("road", "جاده", Palette.BROWN, 0.04f, 0.82f, 0.92f, 0.09f, 6f, 6f, 6f, 6f),
                        dp("window", "شیشه", Palette.YELLOW, 0.34f, 0.30f, 0.14f, 0.13f, 4f, 4f, 4f, 4f)),
                page("قطار",
                        dp("engine", "لوکوموتیو", Palette.RED, 0.10f, 0.36f, 0.36f, 0.30f, 10f, 10f, 4f, 4f),
                        dp("wagon", "واگن", Palette.GREEN, 0.50f, 0.42f, 0.34f, 0.24f, 6f, 6f, 6f, 6f),
                        dp("chimney", "دودکش", Palette.CHARCOAL, 0.16f, 0.22f, 0.10f, 0.14f, 4f, 4f, 0f, 0f),
                        oval("w1", "چرخ جلو", Palette.CHARCOAL, 0.16f, 0.66f, 0.13f, 0.13f),
                        oval("w2", "چرخ عقب", Palette.CHARCOAL, 0.62f, 0.68f, 0.11f, 0.11f),
                        dp("rail", "ریل", Palette.BROWN, 0.02f, 0.82f, 0.96f, 0.06f, 4f, 4f, 4f, 4f)),
                page("قایق",
                        dp("hull", "بدنه", Palette.BROWN, 0.14f, 0.58f, 0.62f, 0.16f, 0f, 0f, 30f, 30f),
                        pill("mast", "دکل", Palette.CHARCOAL, 0.44f, 0.20f, 0.04f, 0.40f),
                        lead("sail", "بادبان", Palette.RED, 0.48f, 0.22f, 0.26f, 0.34f),
                        dp("sea", "دریا", Palette.BLUE, 0.02f, 0.76f, 0.96f, 0.18f, 10f, 10f, 10f, 10f),
                        oval("sun", "خورشید", Palette.YELLOW, 0.76f, 0.12f, 0.16f, 0.18f)),
                page("موشک",
                        frac("body", "بدنه", Palette.RED, 0.38f, 0.14f, 0.24f, 0.52f, 0.5f, 0.5f, 0f, 0f),
                        oval("window", "پنجره", Palette.BLUE, 0.43f, 0.26f, 0.14f, 0.15f),
                        tri("fin1", "باله راست", Palette.ORANGE, 0.26f, 0.48f, 0.14f, 0.20f),
                        tri("fin2", "باله چپ", Palette.ORANGE, 0.60f, 0.48f, 0.14f, 0.20f),
                        tri("flame", "شعله", Palette.YELLOW, 0.40f, 0.66f, 0.20f, 0.22f),
                        oval("star", "ستاره", Palette.PURPLE, 0.78f, 0.16f, 0.10f, 0.11f)),
                page("دوچرخه",
                        oval("w1", "چرخ جلو", Palette.CHARCOAL, 0.08f, 0.48f, 0.30f, 0.32f),
                        oval("w2", "چرخ عقب", Palette.CHARCOAL, 0.60f, 0.48f, 0.30f, 0.32f),
                        dp("frame", "بدنه", Palette.RED, 0.28f, 0.44f, 0.34f, 0.06f, 4f, 4f, 4f, 4f),
                        dp("seat", "زین", Palette.BROWN, 0.30f, 0.34f, 0.16f, 0.07f, 6f, 6f, 2f, 2f),
                        pill("bar", "فرمان", Palette.BLUE, 0.62f, 0.32f, 0.05f, 0.18f))
)));

        packs.add(new ColorPack("fruit", "میوه‌ها", pages(
                page("سیب",
                        oval("body", "سیب", Palette.RED, 0.24f, 0.28f, 0.48f, 0.46f),
                        dp("stem", "ساقه", Palette.BROWN, 0.46f, 0.14f, 0.05f, 0.16f, 2f, 2f, 0f, 0f),
                        frac("leaf", "برگ", Palette.GREEN, 0.52f, 0.16f, 0.20f, 0.11f, 0f, 0.5f, 0f, 0.5f),
                        oval("shine", "برق", Palette.YELLOW, 0.34f, 0.36f, 0.10f, 0.10f)),
                page("هندوانه",
                        frac("rind", "پوست", Palette.GREEN, 0.10f, 0.34f, 0.80f, 0.44f, 0f, 0f, 0.5f, 0.5f),
                        frac("white", "لایه سفید", Palette.YELLOW, 0.14f, 0.38f, 0.72f, 0.36f, 0f, 0f, 0.5f, 0.5f),
                        frac("flesh", "داخل", Palette.RED, 0.18f, 0.42f, 0.64f, 0.30f, 0f, 0f, 0.5f, 0.5f),
                        oval("seed1", "دانه", Palette.CHARCOAL, 0.36f, 0.56f, 0.05f, 0.07f),
                        oval("seed2", "دانه", Palette.CHARCOAL, 0.50f, 0.52f, 0.05f, 0.07f),
                        oval("seed3", "دانه", Palette.CHARCOAL, 0.60f, 0.60f, 0.05f, 0.07f)),
                page("انگور",
                        oval("g1", "دانه", Palette.PURPLE, 0.34f, 0.26f, 0.16f, 0.16f),
                        oval("g2", "دانه", Palette.PURPLE, 0.52f, 0.26f, 0.16f, 0.16f),
                        oval("g3", "دانه", Palette.PURPLE, 0.26f, 0.42f, 0.16f, 0.16f),
                        oval("g4", "دانه", Palette.PURPLE, 0.44f, 0.42f, 0.16f, 0.16f),
                        oval("g5", "دانه", Palette.PURPLE, 0.62f, 0.42f, 0.16f, 0.16f),
                        oval("g6", "دانه", Palette.PURPLE, 0.44f, 0.60f, 0.16f, 0.16f),
                        dp("stem", "ساقه", Palette.BROWN, 0.46f, 0.10f, 0.05f, 0.16f, 2f, 2f, 0f, 0f),
                        frac("leaf", "برگ", Palette.GREEN, 0.52f, 0.12f, 0.18f, 0.10f, 0f, 0.5f, 0f, 0.5f)),
                page("گلابی",
                        oval("bottom", "پایین", Palette.GREEN, 0.28f, 0.44f, 0.42f, 0.38f),
                        oval("top", "بالا", Palette.GREEN, 0.36f, 0.26f, 0.26f, 0.26f),
                        dp("stem", "ساقه", Palette.BROWN, 0.47f, 0.12f, 0.05f, 0.16f, 2f, 2f, 0f, 0f),
                        frac("leaf", "برگ", Palette.RED, 0.52f, 0.14f, 0.18f, 0.10f, 0f, 0.5f, 0f, 0.5f)),
                page("آناناس",
                        frac("body", "بدنه", Palette.YELLOW, 0.30f, 0.36f, 0.40f, 0.46f, 0.3f, 0.3f, 0.4f, 0.4f),
                        tri("l1", "برگ راست", Palette.GREEN, 0.28f, 0.14f, 0.16f, 0.24f),
                        tri("l2", "برگ وسط", Palette.GREEN, 0.42f, 0.08f, 0.16f, 0.28f),
                        tri("l3", "برگ چپ", Palette.GREEN, 0.56f, 0.14f, 0.16f, 0.24f),
                        oval("dot1", "خال", Palette.BROWN, 0.40f, 0.50f, 0.08f, 0.08f),
                        oval("dot2", "خال", Palette.BROWN, 0.54f, 0.62f, 0.08f, 0.08f))
)));

        packs.add(new ColorPack("home", "خونه", pages(
                page("خونه",
                        oval("sun", "خورشید", Palette.YELLOW, 0.70f, 0.04f, 0.20f, 0.16f),
                        tri("roof", "سقف", Palette.RED, 0.10f, 0.16f, 0.76f, 0.24f),
                        dp("wall", "دیوار", Palette.ORANGE, 0.17f, 0.40f, 0.62f, 0.46f, 4f, 4f, 4f, 4f),
                        dp("door", "در", Palette.BROWN, 0.40f, 0.60f, 0.18f, 0.26f, 9f, 9f, 0f, 0f),
                        dp("win", "پنجره", Palette.BLUE, 0.22f, 0.46f, 0.15f, 0.15f, 3f, 3f, 3f, 3f)),
                page("در",
                        dp("frame", "چهارچوب", Palette.BROWN, 0.24f, 0.14f, 0.52f, 0.72f, 8f, 8f, 0f, 0f),
                        dp("panel", "لنگه", Palette.RED, 0.30f, 0.20f, 0.40f, 0.62f, 6f, 6f, 0f, 0f),
                        oval("knob", "دستگیره", Palette.YELLOW, 0.62f, 0.48f, 0.07f, 0.08f),
                        dp("mat", "پادری", Palette.GREEN, 0.20f, 0.86f, 0.60f, 0.08f, 4f, 4f, 4f, 4f)),
                page("پنجره",
                        dp("frame", "چهارچوب", Palette.BROWN, 0.16f, 0.18f, 0.68f, 0.58f, 6f, 6f, 6f, 6f),
                        dp("glass1", "شیشه بالا راست", Palette.BLUE, 0.20f, 0.22f, 0.28f, 0.24f, 3f, 3f, 3f, 3f),
                        dp("glass2", "شیشه بالا چپ", Palette.BLUE, 0.52f, 0.22f, 0.28f, 0.24f, 3f, 3f, 3f, 3f),
                        dp("glass3", "شیشه پایین راست", Palette.BLUE, 0.20f, 0.50f, 0.28f, 0.24f, 3f, 3f, 3f, 3f),
                        dp("glass4", "شیشه پایین چپ", Palette.BLUE, 0.52f, 0.50f, 0.28f, 0.24f, 3f, 3f, 3f, 3f),
                        dp("sill", "لبه", Palette.ORANGE, 0.12f, 0.76f, 0.76f, 0.07f, 3f, 3f, 3f, 3f)),
                page("چراغ",
                        tri("shade", "کلاهک", Palette.ORANGE, 0.24f, 0.14f, 0.52f, 0.30f),
                        pill("pole", "پایه", Palette.CHARCOAL, 0.47f, 0.42f, 0.06f, 0.32f),
                        dp("base", "قاعده", Palette.BROWN, 0.32f, 0.74f, 0.36f, 0.09f, 6f, 6f, 3f, 3f),
                        oval("bulb", "لامپ", Palette.YELLOW, 0.44f, 0.42f, 0.12f, 0.12f)),
                page("صندلی",
                        dp("back", "پشتی", Palette.RED, 0.28f, 0.14f, 0.44f, 0.30f, 8f, 8f, 2f, 2f),
                        dp("seat", "نشیمن", Palette.ORANGE, 0.22f, 0.44f, 0.56f, 0.12f, 4f, 4f, 4f, 4f),
                        pill("leg1", "پایه راست", Palette.BROWN, 0.26f, 0.56f, 0.06f, 0.30f),
                        pill("leg2", "پایه چپ", Palette.BROWN, 0.68f, 0.56f, 0.06f, 0.30f))
)));

        packs.add(new ColorPack("nature", "طبیعت", pages(
                page("درخت",
                        pill("trunk", "تنه", Palette.BROWN, 0.45f, 0.54f, 0.10f, 0.34f),
                        tri("top", "تاج", Palette.GREEN, 0.20f, 0.10f, 0.60f, 0.34f),
                        tri("mid", "وسط", Palette.GREEN, 0.24f, 0.30f, 0.52f, 0.28f),
                        oval("apple", "میوه", Palette.RED, 0.40f, 0.34f, 0.10f, 0.11f)),
                page("گل",
                        oval("p1", "گلبرگ بالا", Palette.RED, 0.38f, 0.10f, 0.24f, 0.22f),
                        oval("p2", "گلبرگ چپ", Palette.RED, 0.18f, 0.28f, 0.24f, 0.22f),
                        oval("p3", "گلبرگ راست", Palette.RED, 0.58f, 0.28f, 0.24f, 0.22f),
                        oval("mid", "وسط", Palette.YELLOW, 0.38f, 0.28f, 0.24f, 0.22f),
                        dp("stem", "ساقه", Palette.GREEN, 0.46f, 0.50f, 0.08f, 0.40f, 6f, 6f, 6f, 6f),
                        frac("leaf", "برگ", Palette.GREEN, 0.54f, 0.62f, 0.24f, 0.14f, 0f, 0.5f, 0f, 0.5f)),
                page("کوه",
                        tri("peak1", "قله بزرگ", Palette.BROWN, 0.10f, 0.20f, 0.50f, 0.52f),
                        tri("peak2", "قله کوچک", Palette.BROWN, 0.48f, 0.32f, 0.42f, 0.40f),
                        tri("snow", "برف", Palette.YELLOW, 0.26f, 0.20f, 0.18f, 0.16f),
                        oval("sun", "خورشید", Palette.ORANGE, 0.74f, 0.10f, 0.16f, 0.16f),
                        dp("ground", "زمین", Palette.GREEN, 0.02f, 0.72f, 0.96f, 0.20f, 8f, 8f, 8f, 8f)),
                page("رودخانه",
                        dp("water", "آب", Palette.BLUE, 0.04f, 0.44f, 0.92f, 0.26f, 14f, 14f, 14f, 14f),
                        dp("bank1", "ساحل بالا", Palette.GREEN, 0.02f, 0.20f, 0.96f, 0.24f, 10f, 10f, 10f, 10f),
                        dp("bank2", "ساحل پایین", Palette.GREEN, 0.02f, 0.70f, 0.96f, 0.22f, 10f, 10f, 10f, 10f),
                        oval("stone", "سنگ", Palette.CHARCOAL, 0.32f, 0.50f, 0.12f, 0.12f),
                        oval("fish", "ماهی", Palette.ORANGE, 0.62f, 0.52f, 0.14f, 0.10f)),
                page("قارچ",
                        frac("cap", "کلاهک", Palette.RED, 0.18f, 0.22f, 0.64f, 0.36f, 0.5f, 0.5f, 0f, 0f),
                        dp("stem", "ساقه", Palette.YELLOW, 0.40f, 0.54f, 0.20f, 0.30f, 6f, 6f, 10f, 10f),
                        oval("dot1", "خال", Palette.YELLOW, 0.30f, 0.30f, 0.11f, 0.11f),
                        oval("dot2", "خال", Palette.YELLOW, 0.56f, 0.28f, 0.11f, 0.11f),
                        dp("grass", "چمن", Palette.GREEN, 0.06f, 0.82f, 0.88f, 0.08f, 4f, 4f, 4f, 4f))
)));

        packs.add(new ColorPack("space", "فضا", pages(
                page("سفینه",
                        oval("dome", "گنبد", Palette.BLUE, 0.36f, 0.24f, 0.28f, 0.22f),
                        frac("body", "بدنه", Palette.CHARCOAL, 0.16f, 0.40f, 0.68f, 0.20f, 0.5f, 0.5f, 0.5f, 0.5f),
                        oval("light1", "چراغ", Palette.YELLOW, 0.28f, 0.46f, 0.08f, 0.08f),
                        oval("light2", "چراغ", Palette.RED, 0.62f, 0.46f, 0.08f, 0.08f),
                        tri("beam", "نور", Palette.YELLOW, 0.34f, 0.60f, 0.32f, 0.30f)),
                page("سیاره",
                        oval("planet", "سیاره", Palette.PURPLE, 0.24f, 0.28f, 0.52f, 0.46f),
                        frac("ring", "حلقه", Palette.ORANGE, 0.08f, 0.44f, 0.84f, 0.12f, 0.5f, 0.5f, 0.5f, 0.5f),
                        oval("spot", "لکه", Palette.RED, 0.36f, 0.38f, 0.14f, 0.12f),
                        oval("moon", "قمر", Palette.YELLOW, 0.78f, 0.16f, 0.14f, 0.14f)),
                page("ستاره",
                        tri("up", "بالا", Palette.YELLOW, 0.30f, 0.10f, 0.40f, 0.40f),
                        tri("down", "پایین", Palette.YELLOW, 0.30f, 0.44f, 0.40f, 0.40f),
                        oval("mid", "وسط", Palette.ORANGE, 0.42f, 0.40f, 0.16f, 0.16f),
                        oval("small", "ستاره کوچک", Palette.YELLOW, 0.78f, 0.70f, 0.12f, 0.12f)),
                page("ماه",
                        oval("moon", "ماه", Palette.YELLOW, 0.22f, 0.20f, 0.52f, 0.52f),
                        oval("crater1", "گودال", Palette.BROWN, 0.34f, 0.32f, 0.12f, 0.12f),
                        oval("crater2", "گودال", Palette.BROWN, 0.52f, 0.48f, 0.14f, 0.14f),
                        oval("crater3", "گودال", Palette.BROWN, 0.32f, 0.56f, 0.09f, 0.09f),
                        oval("star", "ستاره", Palette.PURPLE, 0.80f, 0.14f, 0.12f, 0.12f)),
                page("فضانورد",
                        oval("helmet", "کلاه", Palette.BLUE, 0.34f, 0.14f, 0.32f, 0.30f),
                        oval("visor", "شیشه", Palette.CHARCOAL, 0.40f, 0.20f, 0.20f, 0.16f),
                        frac("body", "لباس", Palette.ORANGE, 0.32f, 0.42f, 0.36f, 0.32f, 0.2f, 0.2f, 0.2f, 0.2f),
                        pill("arm1", "دست راست", Palette.ORANGE, 0.20f, 0.46f, 0.12f, 0.06f),
                        pill("arm2", "دست چپ", Palette.ORANGE, 0.68f, 0.46f, 0.12f, 0.06f),
                        pill("leg1", "پای راست", Palette.ORANGE, 0.38f, 0.72f, 0.08f, 0.18f),
                        pill("leg2", "پای چپ", Palette.ORANGE, 0.54f, 0.72f, 0.08f, 0.18f))
)));

        packs.add(new ColorPack("sea", "دریا", pages(
                page("هشت‌پا",
                        oval("head", "سر", Palette.PURPLE, 0.28f, 0.16f, 0.44f, 0.38f),
                        oval("eye1", "چشم راست", Palette.YELLOW, 0.38f, 0.28f, 0.10f, 0.10f),
                        oval("eye2", "چشم چپ", Palette.YELLOW, 0.54f, 0.28f, 0.10f, 0.10f),
                        pill("leg1", "پا", Palette.PURPLE, 0.24f, 0.54f, 0.08f, 0.30f),
                        pill("leg2", "پا", Palette.PURPLE, 0.40f, 0.58f, 0.08f, 0.30f),
                        pill("leg3", "پا", Palette.PURPLE, 0.56f, 0.58f, 0.08f, 0.30f),
                        pill("leg4", "پا", Palette.PURPLE, 0.70f, 0.54f, 0.08f, 0.30f)),
                page("صدف",
                        frac("shell", "صدف", Palette.ORANGE, 0.20f, 0.30f, 0.60f, 0.44f, 0.5f, 0.5f, 0f, 0f),
                        tri("l1", "خط راست", Palette.YELLOW, 0.28f, 0.34f, 0.14f, 0.38f),
                        tri("l2", "خط وسط", Palette.YELLOW, 0.44f, 0.30f, 0.14f, 0.42f),
                        tri("l3", "خط چپ", Palette.YELLOW, 0.58f, 0.34f, 0.14f, 0.38f),
                        oval("pearl", "مروارید", Palette.PURPLE, 0.44f, 0.62f, 0.12f, 0.12f),
                        dp("sand", "ماسه", Palette.BROWN, 0.04f, 0.80f, 0.92f, 0.10f, 6f, 6f, 6f, 6f)),
                page("ستاره دریایی",
                        tri("a1", "بازو بالا", Palette.ORANGE, 0.36f, 0.08f, 0.28f, 0.30f),
                        tri("a2", "بازو راست", Palette.ORANGE, 0.10f, 0.36f, 0.30f, 0.28f),
                        tri("a3", "بازو چپ", Palette.ORANGE, 0.60f, 0.36f, 0.30f, 0.28f),
                        tri("a4", "بازو پایین راست", Palette.ORANGE, 0.22f, 0.60f, 0.28f, 0.30f),
                        tri("a5", "بازو پایین چپ", Palette.ORANGE, 0.50f, 0.60f, 0.28f, 0.30f),
                        oval("mid", "وسط", Palette.YELLOW, 0.40f, 0.38f, 0.20f, 0.20f)),
                page("اسب دریایی",
                        frac("body", "بدن", Palette.YELLOW, 0.34f, 0.26f, 0.26f, 0.46f, 0.5f, 0.5f, 0.1f, 0.4f),
                        oval("head", "سر", Palette.YELLOW, 0.34f, 0.16f, 0.24f, 0.20f),
                        lead("snout", "پوزه", Palette.ORANGE, 0.54f, 0.20f, 0.16f, 0.10f),
                        oval("eye", "چشم", Palette.CHARCOAL, 0.40f, 0.20f, 0.06f, 0.07f),
                        frac("tail", "دم", Palette.GREEN, 0.30f, 0.66f, 0.22f, 0.20f, 0.5f, 0f, 0.5f, 0.5f),
                        dp("fin", "باله", Palette.GREEN, 0.58f, 0.40f, 0.10f, 0.18f, 4f, 4f, 4f, 4f)),
                page("نهنگ",
                        oval("body", "بدن", Palette.BLUE, 0.14f, 0.34f, 0.58f, 0.36f),
                        lead("tail", "دم", Palette.BLUE, 0.66f, 0.32f, 0.22f, 0.28f),
                        oval("eye", "چشم", Palette.CHARCOAL, 0.24f, 0.42f, 0.06f, 0.07f),
                        dp("belly", "شکم", Palette.YELLOW, 0.20f, 0.54f, 0.42f, 0.12f, 10f, 10f, 10f, 10f),
                        pill("spout", "فواره", Palette.PURPLE, 0.28f, 0.12f, 0.06f, 0.20f),
                        dp("sea", "دریا", Palette.GREEN, 0.02f, 0.80f, 0.96f, 0.12f, 8f, 8f, 8f, 8f))
)));

        packs.add(new ColorPack("party", "جشن", pages(
                page("کیک",
                        dp("plate", "بشقاب", Palette.BLUE, 0.10f, 0.76f, 0.80f, 0.08f, 6f, 6f, 6f, 6f),
                        dp("base", "پایه کیک", Palette.BROWN, 0.18f, 0.54f, 0.64f, 0.24f, 6f, 6f, 4f, 4f),
                        dp("cream", "خامه", Palette.YELLOW, 0.18f, 0.44f, 0.64f, 0.12f, 8f, 8f, 2f, 2f),
                        pill("candle", "شمع", Palette.RED, 0.47f, 0.24f, 0.06f, 0.22f),
                        tri("flame", "شعله", Palette.ORANGE, 0.44f, 0.12f, 0.12f, 0.14f),
                        oval("cherry", "گیلاس", Palette.RED, 0.30f, 0.36f, 0.10f, 0.10f)),
                page("بادکنک",
                        oval("b1", "بادکنک قرمز", Palette.RED, 0.18f, 0.14f, 0.28f, 0.34f),
                        oval("b2", "بادکنک آبی", Palette.BLUE, 0.50f, 0.10f, 0.28f, 0.34f),
                        oval("b3", "بادکنک زرد", Palette.YELLOW, 0.34f, 0.34f, 0.26f, 0.30f),
                        pill("string1", "نخ", Palette.CHARCOAL, 0.31f, 0.48f, 0.03f, 0.34f),
                        pill("string2", "نخ", Palette.CHARCOAL, 0.63f, 0.44f, 0.03f, 0.38f)),
                page("کادو",
                        dp("box", "جعبه", Palette.PURPLE, 0.18f, 0.38f, 0.64f, 0.44f, 6f, 6f, 6f, 6f),
                        dp("lid", "درپوش", Palette.ORANGE, 0.14f, 0.28f, 0.72f, 0.14f, 6f, 6f, 2f, 2f),
                        dp("ribbon", "روبان", Palette.YELLOW, 0.45f, 0.28f, 0.10f, 0.54f, 2f, 2f, 2f, 2f),
                        oval("bow1", "پاپیون راست", Palette.RED, 0.32f, 0.16f, 0.18f, 0.16f),
                        oval("bow2", "پاپیون چپ", Palette.RED, 0.50f, 0.16f, 0.18f, 0.16f)),
                page("کلاه جشن",
                        tri("cone", "کلاه", Palette.RED, 0.26f, 0.16f, 0.48f, 0.54f),
                        oval("ball", "توپک", Palette.YELLOW, 0.44f, 0.08f, 0.12f, 0.12f),
                        dp("band", "نوار", Palette.BLUE, 0.24f, 0.66f, 0.52f, 0.08f, 4f, 4f, 4f, 4f),
                        oval("dot1", "خال", Palette.YELLOW, 0.42f, 0.34f, 0.09f, 0.09f),
                        oval("dot2", "خال", Palette.GREEN, 0.52f, 0.50f, 0.09f, 0.09f)),
                page("شمع",
                        pill("body", "بدنه", Palette.BLUE, 0.40f, 0.28f, 0.20f, 0.52f),
                        tri("flame", "شعله", Palette.ORANGE, 0.42f, 0.10f, 0.16f, 0.18f),
                        oval("glow", "نور", Palette.YELLOW, 0.44f, 0.16f, 0.12f, 0.10f),
                        dp("holder", "جاشمعی", Palette.BROWN, 0.28f, 0.78f, 0.44f, 0.10f, 6f, 6f, 4f, 4f))
)));

        packs.add(new ColorPack("farm", "مزرعه", pages(
                page("گاو",
                        oval("body", "بدن", Palette.YELLOW, 0.18f, 0.36f, 0.52f, 0.34f),
                        oval("head", "سر", Palette.YELLOW, 0.62f, 0.32f, 0.26f, 0.26f),
                        oval("spot1", "خال", Palette.CHARCOAL, 0.28f, 0.44f, 0.14f, 0.13f),
                        oval("spot2", "خال", Palette.CHARCOAL, 0.48f, 0.52f, 0.12f, 0.11f),
                        pill("leg1", "پا", Palette.BROWN, 0.26f, 0.68f, 0.07f, 0.22f),
                        pill("leg2", "پا", Palette.BROWN, 0.54f, 0.68f, 0.07f, 0.22f),
                        oval("eye", "چشم", Palette.CHARCOAL, 0.72f, 0.38f, 0.06f, 0.07f)),
                page("مرغ",
                        oval("body", "بدن", Palette.ORANGE, 0.26f, 0.38f, 0.40f, 0.34f),
                        oval("head", "سر", Palette.ORANGE, 0.58f, 0.28f, 0.22f, 0.22f),
                        tri("comb", "تاج", Palette.RED, 0.62f, 0.16f, 0.14f, 0.14f),
                        lead("beak", "نوک", Palette.YELLOW, 0.76f, 0.34f, 0.12f, 0.09f),
                        pill("leg1", "پا", Palette.YELLOW, 0.36f, 0.70f, 0.05f, 0.18f),
                        pill("leg2", "پا", Palette.YELLOW, 0.52f, 0.70f, 0.05f, 0.18f),
                        oval("eye", "چشم", Palette.CHARCOAL, 0.66f, 0.32f, 0.05f, 0.06f)),
                page("تراکتور",
                        dp("body", "بدنه", Palette.GREEN, 0.20f, 0.36f, 0.50f, 0.26f, 6f, 6f, 4f, 4f),
                        dp("cabin", "کابین", Palette.GREEN, 0.44f, 0.20f, 0.24f, 0.20f, 8f, 8f, 0f, 0f),
                        oval("w1", "چرخ بزرگ", Palette.CHARCOAL, 0.52f, 0.56f, 0.30f, 0.32f),
                        oval("w2", "چرخ کوچک", Palette.CHARCOAL, 0.18f, 0.64f, 0.20f, 0.22f),
                        dp("ground", "زمین", Palette.BROWN, 0.02f, 0.86f, 0.96f, 0.08f, 4f, 4f, 4f, 4f)),
                page("انبار",
                        tri("roof", "سقف", Palette.RED, 0.10f, 0.16f, 0.80f, 0.26f),
                        dp("wall", "دیوار", Palette.ORANGE, 0.16f, 0.40f, 0.68f, 0.44f, 4f, 4f, 4f, 4f),
                        dp("door", "در", Palette.BROWN, 0.38f, 0.54f, 0.24f, 0.30f, 10f, 10f, 0f, 0f),
                        dp("win", "پنجره", Palette.YELLOW, 0.22f, 0.48f, 0.12f, 0.12f, 3f, 3f, 3f, 3f),
                        dp("ground", "زمین", Palette.GREEN, 0.02f, 0.84f, 0.96f, 0.10f, 4f, 4f, 4f, 4f)),
                page("گندم",
                        pill("stem", "ساقه", Palette.GREEN, 0.46f, 0.40f, 0.06f, 0.48f),
                        frac("ear1", "خوشه", Palette.YELLOW, 0.36f, 0.16f, 0.14f, 0.24f, 0.5f, 0.5f, 0.5f, 0.5f),
                        frac("ear2", "خوشه", Palette.YELLOW, 0.50f, 0.12f, 0.14f, 0.24f, 0.5f, 0.5f, 0.5f, 0.5f),
                        frac("ear3", "خوشه", Palette.ORANGE, 0.43f, 0.28f, 0.14f, 0.24f, 0.5f, 0.5f, 0.5f, 0.5f),
                        dp("ground", "خاک", Palette.BROWN, 0.04f, 0.86f, 0.92f, 0.08f, 4f, 4f, 4f, 4f))
)));

        packs.add(new ColorPack("city", "شهر", pages(
                page("ساختمان",
                        dp("tower", "برج", Palette.BLUE, 0.20f, 0.14f, 0.32f, 0.72f, 6f, 6f, 0f, 0f),
                        dp("block", "ساختمان کوتاه", Palette.PURPLE, 0.54f, 0.40f, 0.28f, 0.46f, 6f, 6f, 0f, 0f),
                        dp("w1", "پنجره", Palette.YELLOW, 0.26f, 0.22f, 0.09f, 0.10f, 2f, 2f, 2f, 2f),
                        dp("w2", "پنجره", Palette.YELLOW, 0.38f, 0.22f, 0.09f, 0.10f, 2f, 2f, 2f, 2f),
                        dp("w3", "پنجره", Palette.YELLOW, 0.60f, 0.48f, 0.09f, 0.10f, 2f, 2f, 2f, 2f),
                        dp("street", "خیابان", Palette.CHARCOAL, 0.02f, 0.86f, 0.96f, 0.10f, 4f, 4f, 4f, 4f)),
                page("اتوبوس",
                        dp("body", "بدنه", Palette.YELLOW, 0.10f, 0.30f, 0.76f, 0.38f, 8f, 8f, 4f, 4f),
                        dp("w1", "پنجره", Palette.BLUE, 0.16f, 0.36f, 0.18f, 0.16f, 3f, 3f, 3f, 3f),
                        dp("w2", "پنجره", Palette.BLUE, 0.38f, 0.36f, 0.18f, 0.16f, 3f, 3f, 3f, 3f),
                        dp("w3", "پنجره", Palette.BLUE, 0.60f, 0.36f, 0.18f, 0.16f, 3f, 3f, 3f, 3f),
                        oval("wheel1", "چرخ", Palette.CHARCOAL, 0.20f, 0.62f, 0.16f, 0.17f),
                        oval("wheel2", "چرخ", Palette.CHARCOAL, 0.62f, 0.62f, 0.16f, 0.17f)),
                page("چراغ راهنما",
                        dp("box", "جعبه", Palette.CHARCOAL, 0.36f, 0.10f, 0.28f, 0.62f, 8f, 8f, 8f, 8f),
                        oval("red", "قرمز", Palette.RED, 0.41f, 0.16f, 0.18f, 0.16f),
                        oval("yellow", "زرد", Palette.YELLOW, 0.41f, 0.34f, 0.18f, 0.16f),
                        oval("green", "سبز", Palette.GREEN, 0.41f, 0.52f, 0.18f, 0.16f),
                        pill("pole", "پایه", Palette.BROWN, 0.47f, 0.70f, 0.06f, 0.24f)),
                page("پل",
                        dp("deck", "عرشه", Palette.BROWN, 0.04f, 0.44f, 0.92f, 0.10f, 4f, 4f, 4f, 4f),
                        pill("rope1", "کابل", Palette.RED, 0.24f, 0.20f, 0.05f, 0.26f),
                        pill("rope2", "کابل", Palette.RED, 0.71f, 0.20f, 0.05f, 0.26f),
                        dp("tower1", "برج", Palette.ORANGE, 0.18f, 0.14f, 0.14f, 0.34f, 4f, 4f, 0f, 0f),
                        dp("tower2", "برج", Palette.ORANGE, 0.68f, 0.14f, 0.14f, 0.34f, 4f, 4f, 0f, 0f),
                        dp("water", "آب", Palette.BLUE, 0.02f, 0.62f, 0.96f, 0.28f, 10f, 10f, 10f, 10f)),
                page("فواره",
                        oval("pool", "حوض", Palette.BLUE, 0.14f, 0.60f, 0.72f, 0.26f),
                        pill("jet", "فواره", Palette.PURPLE, 0.47f, 0.24f, 0.06f, 0.38f),
                        oval("drop1", "قطره", Palette.BLUE, 0.34f, 0.20f, 0.10f, 0.11f),
                        oval("drop2", "قطره", Palette.BLUE, 0.56f, 0.18f, 0.10f, 0.11f),
                        dp("rim", "لبه", Palette.CHARCOAL, 0.10f, 0.78f, 0.80f, 0.08f, 6f, 6f, 6f, 6f))
)));

        packs.add(new ColorPack("weather", "آب‌وهوا", pages(
                page("خورشید",
                        oval("sun", "خورشید", Palette.YELLOW, 0.30f, 0.30f, 0.40f, 0.40f),
                        tri("r1", "پرتو بالا", Palette.ORANGE, 0.42f, 0.06f, 0.16f, 0.20f),
                        tri("r2", "پرتو راست", Palette.ORANGE, 0.06f, 0.42f, 0.20f, 0.16f),
                        tri("r3", "پرتو چپ", Palette.ORANGE, 0.74f, 0.42f, 0.20f, 0.16f),
                        tri("r4", "پرتو پایین", Palette.ORANGE, 0.42f, 0.74f, 0.16f, 0.20f),
                        oval("face", "لبخند", Palette.BROWN, 0.42f, 0.46f, 0.16f, 0.10f)),
                page("ابر",
                        oval("c1", "ابر راست", Palette.BLUE, 0.14f, 0.32f, 0.30f, 0.28f),
                        oval("c2", "ابر وسط", Palette.BLUE, 0.34f, 0.24f, 0.34f, 0.32f),
                        oval("c3", "ابر چپ", Palette.BLUE, 0.58f, 0.32f, 0.28f, 0.26f),
                        oval("d1", "قطره", Palette.PURPLE, 0.28f, 0.66f, 0.10f, 0.16f),
                        oval("d2", "قطره", Palette.PURPLE, 0.46f, 0.70f, 0.10f, 0.16f),
                        oval("d3", "قطره", Palette.PURPLE, 0.62f, 0.66f, 0.10f, 0.16f)),
                page("رنگین‌کمان",
                        frac("a1", "نوار قرمز", Palette.RED, 0.06f, 0.26f, 0.88f, 0.60f, 0.5f, 0.5f, 0f, 0f),
                        frac("a2", "نوار نارنجی", Palette.ORANGE, 0.13f, 0.36f, 0.74f, 0.50f, 0.5f, 0.5f, 0f, 0f),
                        frac("a3", "نوار زرد", Palette.YELLOW, 0.20f, 0.46f, 0.60f, 0.40f, 0.5f, 0.5f, 0f, 0f),
                        frac("a4", "نوار سبز", Palette.GREEN, 0.27f, 0.56f, 0.46f, 0.30f, 0.5f, 0.5f, 0f, 0f),
                        oval("cloud1", "ابر", Palette.BLUE, 0.02f, 0.72f, 0.22f, 0.18f),
                        oval("cloud2", "ابر", Palette.BLUE, 0.76f, 0.72f, 0.22f, 0.18f)),
                page("چتر",
                        frac("canopy", "سایه‌بان", Palette.PURPLE, 0.10f, 0.22f, 0.80f, 0.40f, 0.5f, 0.5f, 0f, 0f),
                        pill("handle", "دسته", Palette.BROWN, 0.47f, 0.58f, 0.06f, 0.30f),
                        tri("h1", "لبه راست", Palette.RED, 0.14f, 0.52f, 0.24f, 0.12f),
                        tri("h2", "لبه چپ", Palette.RED, 0.62f, 0.52f, 0.24f, 0.12f),
                        oval("top", "سرچتر", Palette.YELLOW, 0.45f, 0.14f, 0.10f, 0.10f)),
                page("آدم‌برفی",
                        oval("bottom", "پایین", Palette.BLUE, 0.26f, 0.54f, 0.48f, 0.34f),
                        oval("middle", "وسط", Palette.BLUE, 0.32f, 0.34f, 0.36f, 0.26f),
                        oval("head", "سر", Palette.BLUE, 0.38f, 0.16f, 0.24f, 0.22f),
                        oval("eye1", "چشم", Palette.CHARCOAL, 0.43f, 0.22f, 0.05f, 0.06f),
                        oval("eye2", "چشم", Palette.CHARCOAL, 0.53f, 0.22f, 0.05f, 0.06f),
                        lead("nose", "دماغ", Palette.ORANGE, 0.50f, 0.26f, 0.12f, 0.07f),
                        dp("scarf", "شال", Palette.RED, 0.32f, 0.32f, 0.36f, 0.07f, 3f, 3f, 3f, 3f))
)));

        packs.add(new ColorPack("toys", "اسباب‌بازی", pages(
                page("توپ",
                        oval("ball", "توپ", Palette.RED, 0.20f, 0.24f, 0.56f, 0.56f),
                        frac("band", "نوار", Palette.YELLOW, 0.18f, 0.42f, 0.60f, 0.16f, 0.5f, 0.5f, 0.5f, 0.5f),
                        oval("star", "ستاره", Palette.BLUE, 0.42f, 0.36f, 0.14f, 0.14f)),
                page("عروسک",
                        oval("head", "سر", Palette.ORANGE, 0.36f, 0.12f, 0.28f, 0.26f),
                        frac("body", "بدن", Palette.RED, 0.32f, 0.36f, 0.36f, 0.34f, 0.3f, 0.3f, 0.1f, 0.1f),
                        pill("arm1", "دست", Palette.ORANGE, 0.20f, 0.40f, 0.12f, 0.06f),
                        pill("arm2", "دست", Palette.ORANGE, 0.68f, 0.40f, 0.12f, 0.06f),
                        pill("leg1", "پا", Palette.ORANGE, 0.38f, 0.70f, 0.07f, 0.18f),
                        pill("leg2", "پا", Palette.ORANGE, 0.54f, 0.70f, 0.07f, 0.18f),
                        oval("eye1", "چشم", Palette.CHARCOAL, 0.43f, 0.20f, 0.05f, 0.06f),
                        oval("eye2", "چشم", Palette.CHARCOAL, 0.53f, 0.20f, 0.05f, 0.06f)),
                page("ربات",
                        dp("head", "سر", Palette.BLUE, 0.32f, 0.14f, 0.36f, 0.26f, 6f, 6f, 6f, 6f),
                        dp("body", "بدن", Palette.PURPLE, 0.28f, 0.42f, 0.44f, 0.34f, 6f, 6f, 4f, 4f),
                        oval("eye1", "چشم", Palette.YELLOW, 0.39f, 0.20f, 0.09f, 0.10f),
                        oval("eye2", "چشم", Palette.YELLOW, 0.53f, 0.20f, 0.09f, 0.10f),
                        pill("antenna", "آنتن", Palette.RED, 0.48f, 0.04f, 0.05f, 0.12f),
                        pill("arm1", "بازو", Palette.CHARCOAL, 0.18f, 0.46f, 0.10f, 0.06f),
                        pill("arm2", "بازو", Palette.CHARCOAL, 0.72f, 0.46f, 0.10f, 0.06f),
                        dp("btn", "دکمه", Palette.GREEN, 0.44f, 0.54f, 0.12f, 0.10f, 3f, 3f, 3f, 3f)),
                page("بادبادک",
                        frac("kite", "بادبادک", Palette.PURPLE, 0.28f, 0.10f, 0.44f, 0.44f, 0.1f, 0.5f, 0.5f, 0.5f),
                        pill("tail", "دم", Palette.RED, 0.48f, 0.54f, 0.04f, 0.34f),
                        oval("bow1", "پاپیون", Palette.YELLOW, 0.42f, 0.62f, 0.10f, 0.08f),
                        oval("bow2", "پاپیون", Palette.GREEN, 0.42f, 0.76f, 0.10f, 0.08f),
                        dp("cross", "چوب", Palette.BROWN, 0.30f, 0.30f, 0.40f, 0.04f, 2f, 2f, 2f, 2f)),
                page("طبل",
                        dp("body", "بدنه", Palette.RED, 0.20f, 0.34f, 0.60f, 0.36f, 4f, 4f, 8f, 8f),
                        dp("top", "رویه", Palette.YELLOW, 0.18f, 0.28f, 0.64f, 0.10f, 8f, 8f, 4f, 4f),
                        pill("stick1", "چوب", Palette.BROWN, 0.16f, 0.14f, 0.05f, 0.24f),
                        pill("stick2", "چوب", Palette.BROWN, 0.78f, 0.14f, 0.05f, 0.24f),
                        dp("band", "نوار", Palette.BLUE, 0.20f, 0.48f, 0.60f, 0.07f, 2f, 2f, 2f, 2f))
)));


        return Collections.unmodifiableList(packs);
    }

    private static List<ColoringPage> pages(ColoringPage... items) {
        return new ArrayList<>(Arrays.asList(items));
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
        return new ColorRegion(id, label, color, l, t, w, h, Shape.TRIANGLE_UP,
                new float[]{0, 0, 0, 0});
    }

    private static ColorRegion lead(String id, String label, int color,
                                    float l, float t, float w, float h) {
        return new ColorRegion(id, label, color, l, t, w, h, Shape.TRIANGLE_LEAD,
                new float[]{0, 0, 0, 0});
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
