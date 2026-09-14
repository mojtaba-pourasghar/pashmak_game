package ir.brandimo.pashmak.data.catalog;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ir.brandimo.pashmak.R;

/**
 * Three story beats, each with props the child can tap. The narration lines come
 * from the prototype; the props are new, because the design only ever showed
 * placeholder tiles here.
 */
public final class StoryCatalog {

    private static volatile String[] lines;
    private static final List<StoryScene> SCENES = build();

    private StoryCatalog() {
    }

    @NonNull
    public static String[] lines(@NonNull Context context) {
        if (lines == null) {
            lines = context.getApplicationContext()
                    .getResources().getStringArray(R.array.story_lines);
        }
        return lines;
    }

    public static int sceneCount() {
        return SCENES.size();
    }

    @NonNull
    public static StoryScene scene(int index) {
        return SCENES.get(Palette.wrap(index, SCENES.size()));
    }

    @NonNull
    public static String line(@NonNull Context context, int index) {
        String[] all = lines(context);
        return all.length == 0 ? "" : all[Palette.wrap(index, all.length)];
    }

    private static List<StoryScene> build() {
        List<StoryScene> scenes = new ArrayList<>(3);

        scenes.add(scene(
                prop("خورشید", Palette.YELLOW, .12f, .14f, .17f, true, "خورشید گفت: صبح بخیر!"),
                prop("کاغذ", Palette.ORANGE, .40f, .42f, .22f, false, "نقاشیت از کاغذ پرید بیرون!"),
                prop("پرنده", Palette.BLUE, .72f, .22f, .14f, true, "پرنده جیک‌جیک کرد."),
                prop("گل", Palette.RED, .74f, .66f, .15f, true, "گل بوی خوبی می‌ده!")));

        scenes.add(scene(
                prop("درخت", Palette.GREEN, .16f, .44f, .24f, false, "برگ‌های درخت تکون خوردن."),
                prop("توپ", Palette.RED, .46f, .68f, .15f, true, "توپ قل خورد و رفت!"),
                prop("ابر", Palette.BLUE, .64f, .18f, .20f, true, "ابر آروم رد شد."),
                prop("دوست", Palette.PURPLE, .80f, .52f, .17f, true, "یه دوست تازه بهمون پیوست!")));

        scenes.add(scene(
                prop("کیک", Palette.ORANGE, .22f, .58f, .20f, false, "کیک آماده‌ی جشنه!"),
                prop("بادکنک", Palette.PURPLE, .44f, .20f, .16f, true, "بادکنک بالا رفت!"),
                prop("ستاره", Palette.YELLOW, .68f, .34f, .14f, true, "ستاره چشمک زد."),
                prop("ماه", Palette.BLUE, .84f, .70f, .16f, true, "ماه گفت: شب بخیر قهرمان!")));

        return scenes;
    }

    private static StoryScene scene(StoryScene.Prop... props) {
        return new StoryScene(new ArrayList<>(Arrays.asList(props)));
    }

    private static StoryScene.Prop prop(String label, int color, float x, float y,
                                        float size, boolean round, String line) {
        return new StoryScene.Prop(label, color, x, y, size, round, line);
    }
}
