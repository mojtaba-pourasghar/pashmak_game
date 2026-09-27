package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ir.brandimo.pashmak.R;

/**
 * The bedtime playlist: ten recordings that ship with the app.
 *
 * <p>The titles and the order are the ones supplied with the files; the running
 * times were measured from the recordings themselves rather than estimated, by
 * walking their frame headers, so the length shown beside each one is the length
 * that will actually play. There is no verse text — a recording sings its own
 * words and the screen does not repeat them.
 *
 * <p>{@code clip} is a res/raw name, so any of these is replaced by dropping a
 * file in under the same name.
 */
public final class LullabyCatalog {

    private static final List<Lullaby> ALL = Collections.unmodifiableList(Arrays.asList(
            new Lullaby("lullaby_gonjeshk_sanjab", "گنجشک لالا سنجاب لالا",
                    R.drawable.ic_sparkle, 130),
            new Lullaby("lullaby_mahtab_omade", "لالایی مهتاب اومده",
                    R.drawable.ic_moon, 192),
            new Lullaby("lullaby_ghadimi", "لالایی قدیمی",
                    R.drawable.ic_star, 306),
            new Lullaby("lullaby_gahvare_ghadimi", "لالایی قدیمی گهواره",
                    R.drawable.ic_moon, 306),
            new Lullaby("lullaby_arusak_jun", "عروسک جون",
                    R.drawable.ic_sparkle, 245),
            new Lullaby("lullaby_dastan_sorayi", "لالایی داستان سورایی",
                    R.drawable.ic_star, 497),
            new Lullaby("lullaby_la_la_jangal", "لالایی لا لا جنگل",
                    R.drawable.ic_sparkle, 182),
            new Lullaby("lullaby_shab_shode_baz", "لالایی شب شده باز",
                    R.drawable.ic_moon, 254),
            new Lullaby("lullaby_farzand_ziba", "فرزند زیبا",
                    R.drawable.ic_star, 302),
            new Lullaby("lullaby_madar_mikhune", "مادر میخونه",
                    R.drawable.ic_moon, 182)));

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
