package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ir.brandimo.pashmak.R;

/**
 * Twenty stories Pashmak tells. Each one stops now and then to ask the child for
 * help — find something in the picture, or choose what happens next — so it is a
 * conversation rather than a page of text.
 *
 * <p>The order here is the order of {@link StorySceneCatalog}: story <em>n</em>
 * happens in place <em>n</em>, so the two files are read side by side.
 *
 * <p>Every story after the first six follows the same seven-beat shape — open, ask,
 * go on, choose, the two branches, and an ending they both reach — which keeps the
 * branch targets easy to check and gives a three-year-old a rhythm they learn.
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


        /* ---------------- 7. The sea floor ---------------- */
        StoryProp shell = prop("shell", "صدف", R.drawable.face_shape_circle, .22f, .78f, .13f);
        StoryProp starfish = prop("starfish", "ستاره‌ی دریایی", R.drawable.face_shape_star,
                .70f, .80f, .15f);
        StoryProp seaFish = prop("fish", "ماهی", R.drawable.face_fish, .48f, .40f, .18f);

        Story deepSea = new Story("sea", "ته دریا", R.drawable.face_fish,
                StoryBeat.narrate("پشمک یه نفس عمیق کشید و رفت ته دریا. اون پایین همه‌چی آبیه!",
                        seaFish, shell, starfish),
                StoryBeat.ask("کمکم کن! کدوم شنا می‌کنه و باله داره؟",
                        "fish", "آفرین! ماهی طلایی اومد راهنمامون بشه.",
                        seaFish, shell, starfish),
                StoryBeat.narrate("ماهی گفت: «دنبالم بیا، یه چیز قشنگ نشونت می‌دم.»",
                        seaFish, shell),
                StoryBeat.choice("کدوم رو برداریم ببریم خونه؟",
                        "صدف صورتی", 4, "ستاره‌ی دریایی", 5,
                        shell, starfish),
                StoryBeat.narrateTo("صدف رو که دم گوشت بذاری، صدای دریا میاد!", 6,
                        shell, seaFish),
                StoryBeat.narrateTo("ستاره‌ی دریایی پنج تا بازو داره و آروم راه می‌ره.", 6,
                        starfish, seaFish),
                StoryBeat.celebrate("برگشتیم بالا. دریا هزار تا راز داره و ما یکیش رو دیدیم!",
                        seaFish, shell, starfish));

        /* ---------------- 8. The rainbow train ---------------- */
        StoryProp train = prop("train", "قطار", R.drawable.face_train, .30f, .62f, .22f);
        StoryProp mountain = prop("hill", "کوه", R.drawable.face_shape_triangle,
                .74f, .40f, .18f);
        StoryProp trainCloud = prop("cloud", "ابر", R.drawable.face_cloud, .52f, .20f, .16f);

        Story rainbowTrain = new Story("train", "قطار رنگین‌کمان", R.drawable.face_train,
                StoryBeat.narrate("یه قطار قرمز سوت زد: «سوار شو! امروز می‌ریم تا ته درّه.»",
                        train, mountain, trainCloud),
                StoryBeat.ask("اول بگو کدوم قطاره که ما رو می‌بره؟",
                        "train", "درسته! بپر بالا قندعسلم.",
                        train, mountain, trainCloud),
                StoryBeat.narrate("قطار راه افتاد. تلق‌تلق، تلق‌تلق، از کنار کوه‌ها رد شدیم.",
                        train, mountain),
                StoryBeat.choice("از پنجره به چی نگاه کنیم؟",
                        "به کوه‌ها", 4, "به ابرها", 5,
                        mountain, trainCloud),
                StoryBeat.narrateTo("کوه‌ها بزرگ و آروم بودن، انگار خوابیده بودن.", 6,
                        mountain, train),
                StoryBeat.narrateTo("ابرها شکل خرگوش و فیل شده بودن. تو چی می‌دیدی؟", 6,
                        trainCloud, train),
                StoryBeat.celebrate("قطار رسید و ما با یه عالمه عکس تو ذهنمون پیاده شدیم!",
                        train, mountain, trainCloud));

        /* ---------------- 9. The rabbit and the carrot ---------------- */
        StoryProp patchRabbit = prop("rabbit", "خرگوش", R.drawable.face_rabbit,
                .24f, .48f, .20f);
        StoryProp carrotTop = prop("carrot", "هویج", R.drawable.face_shape_triangle,
                .56f, .66f, .14f);
        StoryProp wateringSun = prop("sun", "خورشید", R.drawable.face_sun, .84f, .18f, .15f);

        Story carrotPatch = new Story("carrot", "خرگوش و هویج", R.drawable.face_rabbit,
                StoryBeat.narrate("خرگوش کوچولو تو باغچه دنبال ناهارش می‌گشت.",
                        patchRabbit, carrotTop, wateringSun),
                StoryBeat.ask("کمکش کن! کدوم نارنجیه و زیر خاکه؟",
                        "carrot", "ایول! هویج رو پیدا کردی.",
                        patchRabbit, carrotTop, wateringSun),
                StoryBeat.narrate("خرگوش هویج رو کشید بیرون: «چقدر بزرگه! تنهایی نمی‌خورمش.»",
                        patchRabbit, carrotTop),
                StoryBeat.choice("هویج رو با کی قسمت کنه؟",
                        "با مامانش", 4, "با دوستاش", 5,
                        patchRabbit, carrotTop),
                StoryBeat.narrateTo("مامانش بوسش کرد و گفت: «چه بچه‌ی مهربونی دارم!»", 6,
                        patchRabbit, carrotTop),
                StoryBeat.narrateTo("دوستاش دور هم نشستن و هویج رو تیکه‌تیکه خوردن.", 6,
                        patchRabbit, carrotTop),
                StoryBeat.celebrate("قسمت کردن، غذا رو خوشمزه‌تر می‌کنه. نه؟",
                        patchRabbit, carrotTop, wateringSun));

        /* ---------------- 10. The little bird ---------------- */
        StoryProp chick = prop("bird", "پرنده", R.drawable.face_bird, .66f, .50f, .16f);
        StoryProp nestTree = prop("tree", "درخت", R.drawable.face_tree, .22f, .56f, .20f);
        StoryProp skyCloud = prop("cloud", "ابر", R.drawable.face_cloud, .80f, .20f, .16f);

        Story littleBird = new Story("bird", "پرنده‌ی کوچولو", R.drawable.face_bird,
                StoryBeat.narrate("یه پرنده‌ی کوچولو لب لونه نشسته بود و می‌ترسید پرواز کنه.",
                        chick, nestTree, skyCloud),
                StoryBeat.ask("بگو کدوم بال داره و می‌تونه بپره؟",
                        "bird", "آفرین! همین کوچولوی آبی.",
                        chick, nestTree, skyCloud),
                StoryBeat.narrate("پشمک گفت: «نترس، من اینجام. یه بار امتحان کن.»",
                        chick, nestTree),
                StoryBeat.choice("اول چی کار کنه؟",
                        "بال‌هاشو باز کنه", 4, "یه نفس عمیق بکشه", 5,
                        chick, nestTree),
                StoryBeat.narrateTo("بال‌هاشو باز کرد و باد زیرشون رو گرفت. وااای!", 6,
                        chick, skyCloud),
                StoryBeat.narrateTo("یه نفس عمیق کشید، دلش آروم شد و پرید!", 6,
                        chick, skyCloud),
                StoryBeat.celebrate("حالا داره بالای درخت می‌چرخه. اولین پروازش مبارک!",
                        chick, nestTree, skyCloud));

        /* ---------------- 11. The birthday cake ---------------- */
        StoryProp cake = prop("cake", "کیک", R.drawable.face_cherry, .50f, .44f, .16f);
        StoryProp balloon = prop("balloon", "بادکنک", R.drawable.face_shape_circle,
                .18f, .28f, .14f);
        StoryProp gift = prop("gift", "کادو", R.drawable.face_shape_square, .80f, .70f, .15f);

        Story birthday = new Story("cake", "کیک تولد", R.drawable.face_cherry,
                StoryBeat.narrate("امروز تولد خرس کوچولوئه و ما مسئول سورپرایزیم!",
                        cake, balloon, gift),
                StoryBeat.ask("کدوم گرده و تو هوا می‌مونه؟",
                        "balloon", "دقیقاً! بادکنک‌ها رو بزنیم به سقف.",
                        cake, balloon, gift),
                StoryBeat.narrate("بادکنک‌ها رفتن بالا و اتاق شد مثل یه باغ رنگی.",
                        balloon, cake),
                StoryBeat.choice("حالا چی کم داریم؟",
                        "شمع روی کیک", 4, "کادوی خرسه", 5,
                        cake, gift),
                StoryBeat.narrateTo("شمع رو روشن کردیم و چراغ‌ها رو خاموش. چه قشنگ شد!", 6,
                        cake, balloon),
                StoryBeat.narrateTo("کادو رو گذاشتیم پشت در تا اول کیک رو ببینه.", 6,
                        gift, cake),
                StoryBeat.celebrate("خرس کوچولو در رو باز کرد و گفت: «وااای، ممنونم!»",
                        cake, balloon, gift));

        /* ---------------- 12. The sleepy cat ---------------- */
        StoryProp sleepyCat = prop("cat", "گربه", R.drawable.face_cat, .26f, .52f, .18f);
        StoryProp yarn = prop("ball", "توپ کاموا", R.drawable.face_ball, .56f, .74f, .14f);
        StoryProp window = prop("moon", "ماه", R.drawable.face_shape_moon, .82f, .22f, .15f);

        Story sleepyKitten = new Story("cat", "گربه‌ی خوابالو", R.drawable.face_cat,
                StoryBeat.narrate("گربه‌ی خوابالو روی مبل ولو شده بود و پلک‌هاش سنگین بود.",
                        sleepyCat, yarn, window),
                StoryBeat.ask("کمکم کن! کدوم پشمالوئه و میو میو می‌کنه؟",
                        "cat", "آفرین! همین خانم خوابالو.",
                        sleepyCat, yarn, window),
                StoryBeat.narrate("ولی هر بار که چشماشو می‌بست، یه صدایی بیدارش می‌کرد.",
                        sleepyCat, yarn),
                StoryBeat.choice("چی کار کنیم تا راحت بخوابه؟",
                        "لالایی بخونیم", 4, "چراغ رو کم کنیم", 5,
                        sleepyCat, window),
                StoryBeat.narrateTo("براش لالایی خوندیم و کم‌کم خرخرش بلند شد.", 6,
                        sleepyCat, window),
                StoryBeat.narrateTo("چراغ رو کم کردیم و اتاق شد آروم و نارنجی.", 6,
                        sleepyCat, window),
                StoryBeat.celebrate("گربه خوابید. هیس… آروم راه بریم!",
                        sleepyCat, yarn, window));

        /* ---------------- 13. The red car ---------------- */
        StoryProp redCar = prop("car", "ماشین", R.drawable.face_car, .46f, .68f, .20f);
        StoryProp townTree = prop("tree", "درخت", R.drawable.face_tree, .16f, .46f, .18f);
        StoryProp townHouse = prop("house", "خونه", R.drawable.face_house, .82f, .42f, .20f);

        Story redCarRide = new Story("car", "ماشین قرمز", R.drawable.face_car,
                StoryBeat.narrate("ماشین قرمز روشن شد: «بوق بوق! کی میاد یه دور بزنیم؟»",
                        redCar, townTree, townHouse),
                StoryBeat.ask("کدوم چهار تا چرخ داره؟",
                        "car", "ایول! بپر تو، کمربندت رو ببند.",
                        redCar, townTree, townHouse),
                StoryBeat.narrate("از خیابون رد شدیم و برای همه‌ی همسایه‌ها دست تکون دادیم.",
                        redCar, townHouse),
                StoryBeat.choice("سر چهارراه کدوم طرف بریم؟",
                        "طرف پارک", 4, "طرف خونه‌ی مادربزرگ", 5,
                        townTree, townHouse),
                StoryBeat.narrateTo("تو پارک تاب بازی کردیم تا آفتاب رفت پشت درخت‌ها.", 6,
                        townTree, redCar),
                StoryBeat.narrateTo("مادربزرگ در رو باز کرد و بوی کیکش تا کوچه می‌اومد.", 6,
                        townHouse, redCar),
                StoryBeat.celebrate("شب برگشتیم خونه. چه روز پُرماجرایی بود!",
                        redCar, townTree, townHouse));

        /* ---------------- 14. The butterfly garden ---------------- */
        StoryProp gardenFlower = prop("flower", "گل", R.drawable.face_flower,
                .30f, .56f, .18f);
        StoryProp waterDrop = prop("drop", "قطره", R.drawable.face_drop, .66f, .44f, .14f);
        StoryProp gardenSun = prop("sun", "خورشید", R.drawable.face_sun, .84f, .18f, .16f);

        Story butterflyGarden = new Story("garden", "باغ پروانه‌ها", R.drawable.face_flower,
                StoryBeat.narrate("تو باغ، گل‌ها تشنه بودن و سرشون رو انداخته بودن پایین.",
                        gardenFlower, waterDrop, gardenSun),
                StoryBeat.ask("کدوم آبیه و از آسمون می‌چکه؟",
                        "drop", "آفرین! یه قطره‌ی خنک.",
                        gardenFlower, waterDrop, gardenSun),
                StoryBeat.narrate("به همه‌ی گل‌ها آب دادیم و اونا کم‌کم سرشون رو بلند کردن.",
                        gardenFlower, waterDrop),
                StoryBeat.choice("حالا چی لازم دارن؟",
                        "کمی آفتاب", 4, "کمی سایه", 5,
                        gardenSun, gardenFlower),
                StoryBeat.narrateTo("آفتاب گرمشون کرد و گلبرگ‌ها باز شدن.", 6,
                        gardenSun, gardenFlower),
                StoryBeat.narrateTo("زیر سایه نفسی تازه کردن و رنگشون پررنگ‌تر شد.", 6,
                        gardenFlower, waterDrop),
                StoryBeat.celebrate("حالا باغ پر از پروانه‌ست. تو بهشون رسیدی!",
                        gardenFlower, gardenSun, waterDrop));

        /* ---------------- 15. The first snow ---------------- */
        StoryProp snowTree = prop("tree", "درخت", R.drawable.face_tree, .18f, .50f, .20f);
        StoryProp snowCloud = prop("cloud", "ابر", R.drawable.face_cloud, .56f, .18f, .18f);
        StoryProp snowBall = prop("ball", "گلوله‌ی برف", R.drawable.face_ball,
                .74f, .74f, .14f);

        Story firstSnow = new Story("snow", "برف اول زمستون", R.drawable.face_cloud,
                StoryBeat.narrate("صبح که پرده رو کنار زدیم، همه‌جا سفید شده بود!",
                        snowTree, snowCloud, snowBall),
                StoryBeat.ask("کدوم نرمه و از آسمون میاد؟",
                        "cloud", "درسته! ابرها دیشب حسابی کار کردن.",
                        snowTree, snowCloud, snowBall),
                StoryBeat.narrate("کلاه و دستکش پوشیدیم و دویدیم تو حیاط.",
                        snowTree, snowBall),
                StoryBeat.choice("اول چی بسازیم؟",
                        "آدم‌برفی", 4, "قلعه‌ی برفی", 5,
                        snowBall, snowTree),
                StoryBeat.narrateTo("آدم‌برفی ساختیم و شال پشمک رو انداختیم گردنش.", 6,
                        snowBall, snowTree),
                StoryBeat.narrateTo("یه قلعه‌ی بلند ساختیم که در و برج داشت!", 6,
                        snowBall, snowTree),
                StoryBeat.celebrate("دستامون یخ کرد ولی دلمون گرم بود. زمستون مبارک!",
                        snowTree, snowCloud, snowBall));

        /* ---------------- 16. The little bakery ---------------- */
        StoryProp bread = prop("bread", "نان", R.drawable.face_pear, .62f, .40f, .15f);
        StoryProp wheat = prop("wheat", "گندم", R.drawable.face_shape_triangle,
                .26f, .62f, .14f);
        StoryProp bakerySun = prop("sun", "خورشید", R.drawable.face_sun, .84f, .20f, .14f);

        Story bakery = new Story("bakery", "نانوایی کوچک", R.drawable.face_pear,
                StoryBeat.narrate("نانوا صبح زود بیدار شد. بوی خوب از تنور می‌اومد.",
                        bread, wheat, bakerySun),
                StoryBeat.ask("کدوم رو با گندم درست می‌کنن و می‌خوریم؟",
                        "bread", "آفرین! یه نون داغ و برشته.",
                        bread, wheat, bakerySun),
                StoryBeat.narrate("نانوا گفت: «امروز کمکم می‌کنی؟» و ما آستین بالا زدیم.",
                        bread, wheat),
                StoryBeat.choice("چی کار کنیم؟",
                        "خمیر رو ورز بدیم", 4, "نون‌ها رو بچینیم", 5,
                        wheat, bread),
                StoryBeat.narrateTo("خمیر رو ورز دادیم تا نرم شد مثل بالش.", 6,
                        wheat, bread),
                StoryBeat.narrateTo("نون‌ها رو قشنگ روی طبقه چیدیم، ردیف به ردیف.", 6,
                        bread, wheat),
                StoryBeat.celebrate("اولین مشتری اومد و گفت: «چه نون خوشمزه‌ای!»",
                        bread, wheat, bakerySun));

        /* ---------------- 17. The elephant and the umbrella ---------------- */
        StoryProp bigElephant = prop("elephant", "فیل", R.drawable.face_elephant,
                .26f, .52f, .22f);
        StoryProp bigUmbrella = prop("umbrella", "چتر", R.drawable.face_umbrella,
                .72f, .46f, .18f);
        StoryProp riverDrop = prop("drop", "قطره", R.drawable.face_drop, .52f, .26f, .13f);

        Story elephantUmbrella = new Story("elephant", "فیل و چتر",
                R.drawable.face_elephant,
                StoryBeat.narrate("فیل کوچولو لب رودخونه نشسته بود که چند قطره بارون اومد.",
                        bigElephant, bigUmbrella, riverDrop),
                StoryBeat.ask("زیر کدوم خیس نمی‌شیم؟",
                        "umbrella", "آفرین! چتر رو باز کن.",
                        bigElephant, bigUmbrella, riverDrop),
                StoryBeat.narrate("ولی چتر برای فیل خیلی کوچیک بود! نصفش بیرون موند.",
                        bigElephant, bigUmbrella),
                StoryBeat.choice("چی کار کنیم؟",
                        "با خرطومش آب بپاشه", 4, "بریم زیر درخت", 5,
                        bigElephant, riverDrop),
                StoryBeat.narrateTo("فیل خرطومش رو بالا گرفت و خودش شد یه فواره‌ی بامزه!", 6,
                        bigElephant, riverDrop),
                StoryBeat.narrateTo("رفتیم زیر درخت و به صدای بارون گوش دادیم.", 6,
                        bigElephant, bigUmbrella),
                StoryBeat.celebrate("بارون تموم شد و فیل گفت: «خیس شدن هم قشنگه!»",
                        bigElephant, bigUmbrella, riverDrop));

        /* ---------------- 18. The paper rocket ---------------- */
        StoryProp paperRocket = prop("rocket", "موشک", R.drawable.face_rocket,
                .50f, .40f, .20f);
        StoryProp highCloud = prop("cloud", "ابر", R.drawable.face_cloud, .22f, .64f, .18f);
        StoryProp farStar = prop("star", "ستاره", R.drawable.face_shape_star,
                .80f, .22f, .15f);

        Story paperRocketStory = new Story("paper", "موشک کاغذی", R.drawable.face_rocket,
                StoryBeat.narrate("از یه کاغذ ساده یه موشک ساختیم و فوتش کردیم به آسمون.",
                        paperRocket, highCloud, farStar),
                StoryBeat.ask("کدوم بالا می‌ره و آتیش از تهش میاد؟",
                        "rocket", "دقیقاً! موشک کاغذی ما.",
                        paperRocket, highCloud, farStar),
                StoryBeat.narrate("موشک از ابرها رد شد. اون بالا همه‌چی نرم و سفید بود.",
                        paperRocket, highCloud),
                StoryBeat.choice("تا کجا بریم؟",
                        "تا روی ابرها", 4, "تا پیش ستاره‌ها", 5,
                        highCloud, farStar),
                StoryBeat.narrateTo("روی ابرها راه رفتیم، انگار داشتیم رو پنبه می‌دویدیم.", 6,
                        highCloud, paperRocket),
                StoryBeat.narrateTo("به یه ستاره سلام کردیم و اون برامون چشمک زد.", 6,
                        farStar, paperRocket),
                StoryBeat.celebrate("موشک آروم برگشت تو دستت. فردا بازم می‌سازیمش!",
                        paperRocket, highCloud, farStar));

        /* ---------------- 19. Morning on the farm ---------------- */
        StoryProp farmDog = prop("dog", "سگ", R.drawable.face_dog, .40f, .66f, .18f);
        StoryProp farmHouse = prop("house", "طویله", R.drawable.face_house, .76f, .40f, .20f);
        StoryProp farmSun = prop("sun", "خورشید", R.drawable.face_sun, .16f, .18f, .16f);

        Story farmMorning = new Story("farm", "مزرعه‌ی صبح", R.drawable.face_dog,
                StoryBeat.narrate("آفتاب که زد، همه‌ی حیوون‌های مزرعه بیدار شدن.",
                        farmDog, farmHouse, farmSun),
                StoryBeat.ask("کدوم هاپ هاپ می‌کنه و از مزرعه نگهبانی می‌ده؟",
                        "dog", "آفرین! سگ نگهبان ما.",
                        farmDog, farmHouse, farmSun),
                StoryBeat.narrate("سگه دور مزرعه دوید تا مطمئن شه همه‌چی مرتبه.",
                        farmDog, farmHouse),
                StoryBeat.choice("اول به کی سر بزنیم؟",
                        "به مرغ‌ها", 4, "به گاوها", 5,
                        farmHouse, farmDog),
                StoryBeat.narrateTo("مرغ‌ها برامون تخم‌مرغ گذاشته بودن، هنوز گرم بود!", 6,
                        farmHouse, farmDog),
                StoryBeat.narrateTo("گاوها شیر تازه دادن و ما براشون علف بردیم.", 6,
                        farmHouse, farmDog),
                StoryBeat.celebrate("صبحونه آماده‌ست! مزرعه بدون کمک تو نمی‌چرخه.",
                        farmDog, farmHouse, farmSun));

        /* ---------------- 20. A night of stars ---------------- */
        StoryProp nightStar = prop("star", "ستاره", R.drawable.face_shape_star,
                .62f, .28f, .16f);
        StoryProp nightMoon = prop("moon", "ماه", R.drawable.face_shape_moon,
                .84f, .20f, .16f);
        StoryProp nightTree = prop("tree", "درخت", R.drawable.face_tree, .16f, .56f, .20f);

        Story starryNight = new Story("stars", "شب ستاره‌ها", R.drawable.face_shape_star,
                StoryBeat.narrate("پتو رو بردیم تو حیاط و دراز کشیدیم به آسمون نگاه کردیم.",
                        nightStar, nightMoon, nightTree),
                StoryBeat.ask("کدوم چشمک می‌زنه و خیلی دوره؟",
                        "star", "آفرین! هزارتا ستاره اون بالاست.",
                        nightStar, nightMoon, nightTree),
                StoryBeat.narrate("پشمک گفت: «هر ستاره یه قصه داره. بیا یکی رو انتخاب کنیم.»",
                        nightStar, nightMoon),
                StoryBeat.choice("کدوم رو انتخاب کنیم؟",
                        "روشن‌ترینش", 4, "کوچیک‌ترینش", 5,
                        nightStar, nightMoon),
                StoryBeat.narrateTo("روشن‌ترین ستاره گفت: «من چراغ راه دریانوردام.»", 6,
                        nightStar, nightMoon),
                StoryBeat.narrateTo("کوچیک‌ترین ستاره گفت: «من کوچیکم ولی هیچ‌وقت خاموش نمی‌شم.»",
                        6, nightStar, nightTree),
                StoryBeat.celebrate("چشمات داره سنگین می‌شه… شب به‌خیر قهرمان من.",
                        nightStar, nightMoon, nightTree));

        return Collections.unmodifiableList(Arrays.asList(
                magic, lostBall, moonTrip, fruitParty, newFriend, rainyNight,
                deepSea, rainbowTrain, carrotPatch, littleBird, birthday, sleepyKitten,
                redCarRide, butterflyGarden, firstSnow, bakery, elephantUmbrella,
                paperRocketStory, farmMorning, starryNight));
    }
}
