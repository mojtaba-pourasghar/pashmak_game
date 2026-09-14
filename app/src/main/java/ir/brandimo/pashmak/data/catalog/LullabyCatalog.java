package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ir.brandimo.pashmak.R;

/**
 * The bedtime playlist: ten Persian lullabies, ordered from the liveliest to the
 * quietest so playing straight through winds the evening down on its own.
 */
public final class LullabyCatalog {

    private static final List<Lullaby> ALL = Collections.unmodifiableList(Arrays.asList(
            new Lullaby("lullaby_lay_lay", "لای‌لای گل پونه",
                    "لای‌لای لای‌لای گل پونه، بخواب ای ماه تابونه", R.drawable.ic_moon, 180),
            new Lullaby("lullaby_mahe_man", "ماه من بخواب",
                    "ماه من آروم بخواب، پلکاتو رو هم بذار", R.drawable.ic_moon, 165),
            new Lullaby("lullaby_gonjeshk", "گنجشک لالایی",
                    "گنجشک کوچولو رفته لونه، شب شده و خوابش می‌آد", R.drawable.ic_sparkle, 150),
            new Lullaby("lullaby_setare", "ستاره‌ها بیدارن",
                    "ستاره‌ها بیدار می‌مونن تا تو راحت بخوابی", R.drawable.ic_star, 195),
            new Lullaby("lullaby_abr_narm", "ابر نرم",
                    "یه ابر نرم مثل پنبه، زیر سرت بالش شده", R.drawable.ic_sparkle, 170),
            new Lullaby("lullaby_baran", "صدای بارون",
                    "بارون آروم می‌باره، برات لالایی می‌خونه", R.drawable.ic_moon, 210),
            new Lullaby("lullaby_ahoo", "آهوی کوچولو",
                    "آهوی کوچولوی من، سرتو بذار رو دستِ من", R.drawable.ic_star, 160),
            new Lullaby("lullaby_darya", "دریای خواب",
                    "موج‌های دریا آرومن، تو هم بخواب عزیز دلم", R.drawable.ic_sparkle, 200),
            new Lullaby("lullaby_pashmak", "لالایی پشمک",
                    "پشمک کنارت می‌شینه تا تو بخوابی قندعسل", R.drawable.ic_moon, 185),
            new Lullaby("lullaby_shab_bekheir", "شب به‌خیر ماه",
                    "شب به‌خیر ماه، شب به‌خیر خواب، شب به‌خیر کوچولوی من", R.drawable.ic_star, 140)));

    public static List<Lullaby> all() {
        return ALL;
    }

    public static int count() {
        return ALL.size();
    }

    /** Keeps an index inside the playlist, wrapping in both directions. */
    public static int wrapIndex(int index) {
        return Palette.wrap(index, ALL.size());
    }

    @NonNull
    public static Lullaby at(int index) {
        return ALL.get(Palette.wrap(index, ALL.size()));
    }

    private LullabyCatalog() {
    }
}
