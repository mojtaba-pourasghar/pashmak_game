package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ir.brandimo.pashmak.R;

/**
 * Six stories Pashmak tells. Each one stops now and then to ask the child for
 * help — find something in the picture, or choose what happens next — so it is a
 * conversation rather than a page of text.
 */
public final class StoryCatalog {

    private static final List<Story> STORIES = build();

    private StoryCatalog() {
    }

    @NonNull
    public static List<Story> all() {
        return STORIES;
    }

    @NonNull
    public static Story story(int index) {
        return STORIES.get(Palette.wrap(index, STORIES.size()));
    }

    public static int count() {
        return STORIES.size();
    }

    private static StoryProp prop(String id, String label, int icon,
                                  float x, float y, float size) {
        return new StoryProp(id, label, icon, x, y, size);
    }

    private static List<Story> build() {
        /* ---------------- 1. The drawing that flew off the page ---------------- */
        StoryProp sun = prop("sun", "خورشید", R.drawable.face_sun, .16f, .18f, .16f);
        StoryProp tree = prop("tree", "درخت", R.drawable.face_tree, .48f, .58f, .24f);
        StoryProp house = prop("house", "خونه", R.drawable.face_house, .80f, .56f, .22f);
        StoryProp bird = prop("bird", "پرنده", R.drawable.face_bird, .66f, .22f, .14f);
        StoryProp rabbit = prop("rabbit", "خرگوش", R.drawable.face_rabbit, .30f, .66f, .18f);
        StoryProp boat = prop("boat", "قایق", R.drawable.face_boat, .34f, .62f, .22f);
        StoryProp fish = prop("fish", "ماهی", R.drawable.face_fish, .70f, .72f, .16f);

        Story magic = new Story("magic", "نقاشی جادویی", R.drawable.face_flower,
                StoryBeat.narrate(
                        "یه روز صبح، اولین نقاشیِ تو از کاغذ پرید بیرون و گفت: «من می‌خوام دنیا رو ببینم!»",
                        sun, tree, house),
                StoryBeat.ask("قبلش کمکم کن! کدوم تو آسمونه و گرممون می‌کنه؟",
                        "sun", "آفرین قندعسلم! خورشید بالای سرمونه.",
                        sun, tree, house),
                StoryBeat.narrate("خورشید بهمون لبخند زد و ما راه افتادیم.", sun, tree, house),
                StoryBeat.choice("حالا تو بگو از کدوم راه بریم؟",
                        "بریم جنگل", 4, "بریم دریا", 6,
                        tree, boat),
                StoryBeat.narrate("تو جنگل یه خرگوش دیدیم که دنبال دوست می‌گشت.",
                        tree, rabbit, bird),
                StoryBeat.narrateTo("خرگوش کوچولو هم باهامون اومد. چه روز خوبی!", 8,
                        tree, rabbit),
                StoryBeat.narrate("لب دریا یه قایق قرمز منتظرمون بود.", boat, fish, sun),
                StoryBeat.narrateTo("سوار قایق شدیم و یه ماهی طلایی برامون آواز خوند.", 8,
                        boat, fish),
                StoryBeat.celebrate("آخرش همه دور هم جمع شدیم. این قشنگ‌ترین روزِ نقاشیِ تو بود!",
                        sun, tree, rabbit, fish));

        /* ---------------- 2. The lost ball ---------------- */
        StoryProp ball = prop("ball", "توپ", R.drawable.face_ball, .52f, .70f, .18f);
        StoryProp dog = prop("dog", "سگ", R.drawable.face_dog, .24f, .60f, .20f);
        StoryProp cloud = prop("cloud", "ابر", R.drawable.face_cloud, .70f, .20f, .20f);
        StoryProp cat = prop("cat", "گربه", R.drawable.face_cat, .78f, .62f, .18f);

        Story lostBall = new Story("ball", "توپ گم‌شده", R.drawable.face_ball,
                StoryBeat.narrate("پشمک و دوستاش تو حیاط بازی می‌کردن که توپ قل خورد و گم شد!",
                        dog, cat, cloud),
                StoryBeat.ask("کمکم کن! کدوم گرده و باهاش بازی می‌کنیم؟",
                        "ball", "ایول! توپمون رو پیدا کردی!",
                        ball, dog, cat),
                StoryBeat.choice("توپ رو به کی بدیم اول بازی کنه؟",
                        "به سگ کوچولو", 3, "به گربه‌ی مهربون", 4,
                        dog, cat, ball),
                StoryBeat.narrateTo("سگ کوچولو توپ رو با پوزه‌اش هل داد و همه خندیدیم!", 5,
                        dog, ball),
                StoryBeat.narrateTo("گربه توپ رو با دُمش چرخوند، چه بامزه بود!", 5,
                        cat, ball),
                StoryBeat.celebrate("تا شب بازی کردیم. دوستی یعنی همین!", dog, cat, ball, cloud));

        /* ---------------- 3. A trip to the moon ---------------- */
        StoryProp rocket = prop("rocket", "موشک", R.drawable.face_rocket, .30f, .52f, .24f);
        StoryProp moon = prop("moon", "ماه", R.drawable.face_shape_moon, .74f, .22f, .18f);
        StoryProp star = prop("star", "ستاره", R.drawable.face_shape_star, .52f, .16f, .13f);

        Story moonTrip = new Story("moon", "سفر به ماه", R.drawable.face_rocket,
                StoryBeat.narrate("پشمک یه موشک نارنجی ساخت و گفت: «بریم ماه رو ببینیم؟»",
                        rocket, star, moon),
                StoryBeat.ask("برای پرواز باید بدونیم کجا می‌ریم. کدوم ماهه؟",
                        "moon", "درسته! ماه اون بالا منتظرمونه.",
                        rocket, star, moon),
                StoryBeat.narrate("سه، دو، یک… موشک بلند شد و از لای ستاره‌ها رد شد!",
                        rocket, star, moon),
                StoryBeat.choice("رو ماه چی کار کنیم؟",
                        "بپریم بالا پایین", 4, "ستاره جمع کنیم", 5,
                        moon, star),
                StoryBeat.narrateTo("رو ماه همه‌چی سبکه! پشمک تا آسمون پرید و خندید.", 6,
                        moon, rocket),
                StoryBeat.narrateTo("یه جیب پر ستاره جمع کردیم که شب‌ها چراغ اتاقت باشه.", 6,
                        star, moon),
                StoryBeat.celebrate("برگشتیم خونه. حالا تو هم یه ستاره‌ی خودت داری!",
                        rocket, star, moon));

        /* ---------------- 4. The fruit party ---------------- */
        StoryProp apple = prop("apple", "سیب", R.drawable.face_apple, .26f, .40f, .18f);
        StoryProp banana = prop("banana", "موز", R.drawable.face_banana, .50f, .62f, .20f);
        StoryProp grape = prop("grape", "انگور", R.drawable.face_grape, .74f, .40f, .18f);
        StoryProp watermelon = prop("melon", "هندوانه", R.drawable.face_watermelon,
                .50f, .28f, .20f);

        Story fruitParty = new Story("fruit", "مهمونی میوه‌ها", R.drawable.face_apple,
                StoryBeat.narrate("امروز تولد پشمکه و همه‌ی میوه‌ها دعوتن!",
                        apple, banana, grape),
                StoryBeat.ask("یه میوه‌ی قرمز و گرد می‌خوام. کدومه؟",
                        "apple", "آفرین! سیب سرخ اومد به مهمونی.",
                        apple, banana, grape),
                StoryBeat.ask("حالا یکی که درازه و زرده!",
                        "banana", "دقیقاً! موز هم اومد.",
                        apple, banana, grape),
                StoryBeat.choice("برای پذیرایی چی درست کنیم؟",
                        "آب‌میوه", 4, "بستنی میوه‌ای", 5,
                        watermelon, grape),
                StoryBeat.narrateTo("یه پارچ آب‌میوه‌ی خنک درست کردیم، همه سیر شدن!", 6,
                        apple, grape),
                StoryBeat.narrateTo("بستنی میوه‌ای درست کردیم و همه لیس زدن و خندیدن!", 6,
                        watermelon, banana),
                StoryBeat.celebrate("پشمک گفت: «بهترین تولد عمرم بود!» تو هم دعوتی.",
                        apple, banana, grape, watermelon));

        /* ---------------- 5. The new friend ---------------- */
        StoryProp frog = prop("frog", "قورباغه", R.drawable.face_frog, .28f, .64f, .18f);
        StoryProp bear = prop("bear", "خرس", R.drawable.face_bear, .74f, .58f, .22f);
        StoryProp flower = prop("flower", "گل", R.drawable.face_flower, .52f, .70f, .16f);

        Story newFriend = new Story("friend", "دوست تازه", R.drawable.face_bear,
                StoryBeat.narrate("یه خرس کوچولو تنها نشسته بود و غصه‌دار بود.",
                        bear, tree, cloud),
                StoryBeat.choice("چی کار کنیم که خوشحال شه؟",
                        "بریم باهاش حرف بزنیم", 2, "براش گل ببریم", 3,
                        bear, flower),
                StoryBeat.narrateTo("رفتیم سلام کردیم و اسمش رو پرسیدیم. لبخند زد!", 4,
                        bear, frog),
                StoryBeat.narrateTo("یه گل قشنگ چیدیم و بهش دادیم. چشماش برق زد!", 4,
                        bear, flower),
                StoryBeat.ask("حالا یه دوست دیگه هم صدا کنیم. کدوم تو آب می‌پره؟",
                        "frog", "درسته! قورباغه هم اومد تو جمعمون.",
                        frog, bear, flower),
                StoryBeat.celebrate("حالا سه تا دوستیم. هیچ‌کس دیگه تنها نیست!",
                        bear, frog, flower, sun));

        /* ---------------- 6. The rainy night ---------------- */
        StoryProp umbrella = prop("umbrella", "چتر", R.drawable.face_umbrella, .30f, .48f, .22f);
        StoryProp drop = prop("drop", "قطره", R.drawable.face_drop, .58f, .26f, .12f);

        Story rainyNight = new Story("rain", "شب بارونی", R.drawable.face_umbrella,
                StoryBeat.narrate("ابرها جمع شدن و بارون شروع شد. کاش یه چیزی داشتیم!",
                        cloud, drop, house),
                StoryBeat.ask("کمکم کن! زیر کدوم خیس نمی‌شیم؟",
                        "umbrella", "آفرین! چتر نجاتمون داد.",
                        umbrella, drop, cloud),
                StoryBeat.narrate("زیر چتر راه افتادیم و صدای قطره‌ها مثل آهنگ بود.",
                        umbrella, drop, house),
                StoryBeat.choice("بریم خونه یا تو بارون بازی کنیم؟",
                        "بریم خونه", 4, "تو بارون بازی!", 5,
                        house, drop),
                StoryBeat.narrateTo("رفتیم خونه، شیر داغ خوردیم و از پنجره بارون رو تماشا کردیم.",
                        6, house, cloud),
                StoryBeat.narrateTo("تو گودال‌ها پریدیم و کلی خندیدیم! لباسامون خیس شد ولی می‌ارزید.",
                        6, drop, umbrella),
                StoryBeat.celebrate("بارون تموم شد و یه رنگین‌کمون اومد. شب بخیر قهرمان!",
                        sun, cloud, house));

        return Collections.unmodifiableList(Arrays.asList(
                magic, lostBall, moonTrip, fruitParty, newFriend, rainyNight));
    }
}
