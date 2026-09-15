package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;

import ir.brandimo.pashmak.R;

import static ir.brandimo.pashmak.data.catalog.SceneBuilder.anchors;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.at;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.box;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.decor;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.oval;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.rect;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.scene;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.shapes;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.triUp;

/**
 * A drawn place for each of the twenty stories, in the same shorthand the mission
 * rooms are written in — so a story kitchen and a mission kitchen are the same
 * kitchen. The story's props stand in this; the place itself is never tappable.
 */
public final class StorySceneCatalog {

    private static final MissionScene[] SCENES = build();

    private StorySceneCatalog() {
    }

    @NonNull
    public static MissionScene forStory(int storyIndex) {
        return SCENES[Palette.wrap(storyIndex, SCENES.length)];
    }

    private static MissionScene[] build() {
        return new MissionScene[]{
                // 1 نقاشی جادویی — a meadow under a wide morning sky
                scene("#BFE7F7", "#A8DC85", 0.62f,
                        shapes(
                                oval("#8FCB68", 0.00f, 0.56f, 0.46f, 0.18f),
                                oval("#8FCB68", 0.62f, 0.55f, 0.50f, 0.20f),
                                rect("#93CE72", 0.00f, 0.74f, 1.00f, 0.03f),
                                oval("#FFFDF8", 0.10f, 0.08f, 0.22f, 0.10f),
                                oval("#FFFDF8", 0.66f, 0.12f, 0.26f, 0.11f)),
                        decor(at(R.drawable.face_flower, 0.10f, 0.86f, 0.13f),
                                at(R.drawable.face_flower, 0.90f, 0.82f, 0.10f)),
                        anchors()),

                // 2 توپ گم‌شده — a walled yard with a paved strip
                scene("#DFF3FB", "#C6E9A8", 0.58f,
                        shapes(
                                rect("#E8C9A8", 0.00f, 0.44f, 1.00f, 0.14f),
                                rect("#D8B48C", 0.00f, 0.55f, 1.00f, 0.03f),
                                rect("#CFCFC6", 0.00f, 0.80f, 1.00f, 0.20f),
                                box("#B8B8AE", 0.06f, 0.82f, 0.16f, 0.06f, 0.30f),
                                box("#B8B8AE", 0.40f, 0.86f, 0.16f, 0.06f, 0.30f),
                                box("#B8B8AE", 0.76f, 0.82f, 0.16f, 0.06f, 0.30f)),
                        decor(at(R.drawable.face_tree, 0.88f, 0.42f, 0.30f)),
                        anchors()),

                // 3 سفر به ماه — the lunar surface under a black sky
                scene("#18224A", "#C2C7CB", 0.66f,
                        shapes(
                                oval("#A4ACB2", 0.04f, 0.70f, 0.24f, 0.10f),
                                oval("#A4ACB2", 0.58f, 0.76f, 0.30f, 0.12f),
                                oval("#B4BABF", 0.34f, 0.84f, 0.20f, 0.08f),
                                oval("#FFFDF8", 0.12f, 0.10f, 0.03f, 0.05f),
                                oval("#FFFDF8", 0.30f, 0.20f, 0.02f, 0.035f),
                                oval("#FFFDF8", 0.70f, 0.12f, 0.025f, 0.045f),
                                oval("#FFFDF8", 0.86f, 0.28f, 0.02f, 0.035f)),
                        decor(at(R.drawable.face_shape_moon, 0.16f, 0.22f, 0.22f)),
                        anchors()),

                // 4 مهمونی میوه‌ها — a kitchen with a tiled wall and a counter
                scene("#FFF1E0", "#E8C9A8", 0.60f,
                        shapes(
                                rect("#DFF3FB", 0.00f, 0.08f, 1.00f, 0.42f),
                                rect("#CFE8F5", 0.00f, 0.22f, 1.00f, 0.015f),
                                rect("#CFE8F5", 0.00f, 0.36f, 1.00f, 0.015f),
                                rect("#CFE8F5", 0.32f, 0.08f, 0.012f, 0.42f),
                                rect("#CFE8F5", 0.66f, 0.08f, 0.012f, 0.42f),
                                rect("#F2DCC0", 0.00f, 0.50f, 1.00f, 0.05f),
                                rect("#8A5A2B", 0.00f, 0.55f, 1.00f, 0.45f),
                                box("#A06A38", 0.06f, 0.60f, 0.26f, 0.30f, 0.08f),
                                box("#A06A38", 0.68f, 0.60f, 0.26f, 0.30f, 0.08f),
                                oval("#653E17", 0.17f, 0.73f, 0.04f, 0.03f),
                                oval("#653E17", 0.79f, 0.73f, 0.04f, 0.03f)),
                        decor(at(R.drawable.face_watermelon, 0.50f, 0.44f, 0.15f),
                                at(R.drawable.face_apple, 0.26f, 0.45f, 0.10f)),
                        anchors()),

                // 5 دوست تازه — a clearing at the edge of the woods
                scene("#C9E9F7", "#A8DC85", 0.58f,
                        shapes(
                                triUp("#2E7A1D", 0.02f, 0.26f, 0.22f, 0.34f),
                                triUp("#43B02A", 0.18f, 0.32f, 0.20f, 0.28f),
                                triUp("#2E7A1D", 0.74f, 0.24f, 0.24f, 0.36f),
                                rect("#8FCB68", 0.00f, 0.72f, 1.00f, 0.04f),
                                oval("#93CE72", 0.30f, 0.80f, 0.40f, 0.14f)),
                        decor(at(R.drawable.face_sun, 0.50f, 0.14f, 0.16f)),
                        anchors()),

                // 6 شب بارونی — a street in the rain
                scene("#4A4E8C", "#55607A", 0.60f,
                        shapes(
                                rect("#3C4270", 0.02f, 0.26f, 0.20f, 0.36f),
                                rect("#FFC730", 0.06f, 0.34f, 0.05f, 0.06f),
                                rect("#3C4270", 0.78f, 0.20f, 0.22f, 0.42f),
                                rect("#FFC730", 0.88f, 0.30f, 0.05f, 0.06f),
                                rect("#6A7490", 0.00f, 0.74f, 1.00f, 0.04f),
                                oval("#7C86A0", 0.24f, 0.84f, 0.22f, 0.06f),
                                oval("#7C86A0", 0.60f, 0.88f, 0.26f, 0.06f)),
                        decor(at(R.drawable.face_cloud, 0.46f, 0.14f, 0.24f),
                                at(R.drawable.face_drop, 0.20f, 0.30f, 0.07f),
                                at(R.drawable.face_drop, 0.66f, 0.40f, 0.06f)),
                        anchors()),

                // 7 ته دریا — the seabed
                scene("#1FA6D6", "#E8D9A8", 0.72f,
                        shapes(
                                oval("#4FBBE0", 0.10f, 0.08f, 0.16f, 0.07f),
                                oval("#4FBBE0", 0.60f, 0.16f, 0.20f, 0.08f),
                                box("#2E7A1D", 0.06f, 0.44f, 0.05f, 0.30f, 0.50f),
                                box("#43B02A", 0.14f, 0.52f, 0.04f, 0.22f, 0.50f),
                                box("#2E7A1D", 0.86f, 0.40f, 0.05f, 0.34f, 0.50f),
                                oval("#D8C68C", 0.20f, 0.80f, 0.30f, 0.10f)),
                        decor(at(R.drawable.face_fish, 0.70f, 0.56f, 0.14f)),
                        anchors()),

                // 8 قطار رنگین‌کمان — a valley with a railway line
                scene("#BFE7F7", "#A8DC85", 0.56f,
                        shapes(
                                triUp("#9AA0A6", 0.00f, 0.18f, 0.34f, 0.38f),
                                triUp("#B4BABF", 0.24f, 0.26f, 0.28f, 0.30f),
                                triUp("#9AA0A6", 0.66f, 0.20f, 0.34f, 0.36f),
                                rect("#8A5A2B", 0.00f, 0.76f, 1.00f, 0.04f),
                                rect("#653E17", 0.00f, 0.82f, 1.00f, 0.02f),
                                rect("#653E17", 0.00f, 0.88f, 1.00f, 0.02f)),
                        decor(at(R.drawable.face_train, 0.26f, 0.68f, 0.20f)),
                        anchors()),

                // 9 خرگوش و هویج — a vegetable patch
                scene("#DFF3FB", "#B5793A", 0.60f,
                        shapes(
                                rect("#A8DC85", 0.00f, 0.52f, 1.00f, 0.10f),
                                rect("#8A5A2B", 0.00f, 0.62f, 1.00f, 0.38f),
                                oval("#653E17", 0.02f, 0.72f, 0.28f, 0.05f),
                                oval("#653E17", 0.36f, 0.82f, 0.28f, 0.05f),
                                oval("#653E17", 0.70f, 0.72f, 0.28f, 0.05f),
                                triUp("#43B02A", 0.08f, 0.62f, 0.10f, 0.12f),
                                triUp("#6ED050", 0.18f, 0.64f, 0.09f, 0.10f),
                                triUp("#43B02A", 0.42f, 0.72f, 0.10f, 0.12f),
                                triUp("#6ED050", 0.52f, 0.74f, 0.09f, 0.10f),
                                triUp("#43B02A", 0.76f, 0.62f, 0.10f, 0.12f)),
                        decor(at(R.drawable.face_rabbit, 0.88f, 0.55f, 0.18f)),
                        anchors()),

                // 10 پرنده‌ی کوچولو — high in a treetop
                scene("#C9E9F7", "#2E7A1D", 0.74f,
                        shapes(
                                oval("#2E7A1D", 0.02f, 0.10f, 0.52f, 0.30f),
                                oval("#43B02A", 0.42f, 0.06f, 0.56f, 0.32f),
                                oval("#6ED050", 0.22f, 0.20f, 0.44f, 0.24f),
                                rect("#8A5A2B", 0.46f, 0.36f, 0.09f, 0.44f),
                                rect("#653E17", 0.51f, 0.36f, 0.04f, 0.44f),
                                rect("#8A5A2B", 0.20f, 0.50f, 0.28f, 0.04f),
                                rect("#8A5A2B", 0.53f, 0.58f, 0.26f, 0.04f),
                                box("#B5793A", 0.22f, 0.44f, 0.16f, 0.06f, 0.40f)),
                        decor(at(R.drawable.face_bird, 0.66f, 0.53f, 0.13f)),
                        anchors()),

                // 11 کیک تولد — a party room with bunting
                scene("#FFE9A8", "#F0B98E", 0.64f,
                        shapes(
                                rect("#FFD9B0", 0.00f, 0.00f, 1.00f, 0.64f),
                                triUp("#E1251B", 0.08f, 0.06f, 0.08f, 0.10f),
                                triUp("#1FA6D6", 0.24f, 0.06f, 0.08f, 0.10f),
                                triUp("#43B02A", 0.40f, 0.06f, 0.08f, 0.10f),
                                triUp("#7A4FA3", 0.56f, 0.06f, 0.08f, 0.10f),
                                triUp("#F7941D", 0.72f, 0.06f, 0.08f, 0.10f),
                                rect("#D8956A", 0.00f, 0.64f, 1.00f, 0.04f),
                                box("#FFFDF8", 0.30f, 0.50f, 0.40f, 0.14f, 0.16f),
                                box("#FF9E8A", 0.30f, 0.50f, 0.40f, 0.04f, 0.40f),
                                box("#FFFDF8", 0.36f, 0.38f, 0.28f, 0.12f, 0.18f),
                                box("#FF9E8A", 0.36f, 0.38f, 0.28f, 0.035f, 0.40f),
                                rect("#FFC730", 0.49f, 0.30f, 0.02f, 0.08f)),
                        decor(at(R.drawable.face_cherry, 0.50f, 0.36f, 0.09f)),
                        anchors()),

                // 12 گربه‌ی خوابالو — a living room at dusk
                scene("#FFF1E0", "#B5793A", 0.62f,
                        shapes(
                                rect("#F5E0C8", 0.00f, 0.00f, 1.00f, 0.62f),
                                rect("#E0C4A4", 0.00f, 0.58f, 1.00f, 0.04f),
                                box("#7A4FA3", 0.62f, 0.30f, 0.30f, 0.32f, 0.08f),
                                box("#A278C9", 0.66f, 0.34f, 0.22f, 0.16f, 0.10f),
                                box("#B31C14", 0.04f, 0.58f, 0.40f, 0.18f, 0.20f),
                                box("#E1251B", 0.04f, 0.70f, 0.40f, 0.18f, 0.16f),
                                box("#B31C14", 0.02f, 0.66f, 0.07f, 0.20f, 0.30f),
                                box("#B31C14", 0.39f, 0.66f, 0.07f, 0.20f, 0.30f),
                                oval("#8A5A2B", 0.50f, 0.86f, 0.40f, 0.08f)),
                        decor(at(R.drawable.face_cat, 0.24f, 0.62f, 0.15f)),
                        anchors()),

                // 13 ماشین قرمز — a road through town
                scene("#BFE7F7", "#8A8F96", 0.58f,
                        shapes(
                                rect("#E1251B", 0.02f, 0.24f, 0.18f, 0.34f),
                                rect("#1FA6D6", 0.24f, 0.30f, 0.16f, 0.28f),
                                rect("#F7941D", 0.62f, 0.20f, 0.20f, 0.38f),
                                rect("#7A4FA3", 0.84f, 0.32f, 0.14f, 0.26f),
                                rect("#6E737A", 0.00f, 0.58f, 1.00f, 0.42f),
                                rect("#FFC730", 0.06f, 0.78f, 0.14f, 0.02f),
                                rect("#FFC730", 0.32f, 0.78f, 0.14f, 0.02f),
                                rect("#FFC730", 0.58f, 0.78f, 0.14f, 0.02f),
                                rect("#FFC730", 0.84f, 0.78f, 0.14f, 0.02f)),
                        decor(at(R.drawable.face_car, 0.50f, 0.66f, 0.20f)),
                        anchors()),

                // 14 باغ پروانه‌ها — a flower bed in full sun
                scene("#DFF3FB", "#A8DC85", 0.54f,
                        shapes(
                                oval("#8FCB68", 0.00f, 0.48f, 0.40f, 0.14f),
                                oval("#8FCB68", 0.56f, 0.46f, 0.46f, 0.16f),
                                rect("#43B02A", 0.16f, 0.62f, 0.02f, 0.20f),
                                rect("#43B02A", 0.44f, 0.58f, 0.02f, 0.24f),
                                rect("#43B02A", 0.74f, 0.64f, 0.02f, 0.18f),
                                oval("#E1251B", 0.12f, 0.56f, 0.10f, 0.08f),
                                oval("#FFC730", 0.40f, 0.52f, 0.10f, 0.08f),
                                oval("#7A4FA3", 0.70f, 0.58f, 0.10f, 0.08f)),
                        decor(at(R.drawable.face_sun, 0.86f, 0.14f, 0.16f)),
                        anchors()),

                // 15 برف اول زمستون — a snowy hill
                scene("#DDEBF7", "#FFFDF8", 0.56f,
                        shapes(
                                triUp("#C7DCEE", 0.06f, 0.24f, 0.28f, 0.34f),
                                triUp("#D8E8F5", 0.64f, 0.26f, 0.30f, 0.32f),
                                oval("#DCE9F5", 0.00f, 0.48f, 0.56f, 0.22f),
                                oval("#E8F1F9", 0.52f, 0.46f, 0.56f, 0.24f),
                                oval("#FFFDF8", 0.20f, 0.14f, 0.05f, 0.04f),
                                oval("#FFFDF8", 0.52f, 0.22f, 0.04f, 0.03f),
                                oval("#FFFDF8", 0.78f, 0.12f, 0.045f, 0.035f)),
                        decor(at(R.drawable.face_tree, 0.14f, 0.52f, 0.22f)),
                        anchors()),

                // 16 نانوایی کوچک — a bakery counter
                scene("#FFF4E4", "#C89B6A", 0.58f,
                        shapes(
                                rect("#F2DCC0", 0.00f, 0.00f, 1.00f, 0.58f),
                                box("#8A5A2B", 0.04f, 0.08f, 0.34f, 0.40f, 0.05f),
                                rect("#653E17", 0.04f, 0.26f, 0.34f, 0.02f),
                                rect("#653E17", 0.04f, 0.44f, 0.34f, 0.02f),
                                oval("#D8A96A", 0.08f, 0.17f, 0.11f, 0.08f),
                                oval("#C99458", 0.23f, 0.17f, 0.11f, 0.08f),
                                oval("#D8A96A", 0.13f, 0.35f, 0.13f, 0.08f),
                                rect("#B5793A", 0.00f, 0.50f, 1.00f, 0.06f),
                                rect("#8A5A2B", 0.00f, 0.56f, 1.00f, 0.44f),
                                oval("#D8A96A", 0.54f, 0.43f, 0.18f, 0.08f)),
                        decor(at(R.drawable.face_pear, 0.80f, 0.44f, 0.12f)),
                        anchors()),

                // 17 فیل و چتر — a riverbank on a bright day
                scene("#C9E9F7", "#A8DC85", 0.60f,
                        shapes(
                                oval("#8FCB68", 0.00f, 0.52f, 0.44f, 0.16f),
                                rect("#3FA9DC", 0.00f, 0.70f, 1.00f, 0.30f),
                                oval("#5AC6EC", 0.06f, 0.74f, 0.30f, 0.05f),
                                oval("#5AC6EC", 0.52f, 0.82f, 0.34f, 0.05f),
                                rect("#D8C68C", 0.00f, 0.66f, 1.00f, 0.05f)),
                        decor(at(R.drawable.face_elephant, 0.22f, 0.54f, 0.22f),
                                at(R.drawable.face_umbrella, 0.76f, 0.46f, 0.18f)),
                        anchors()),

                // 18 موشک کاغذی — high above the clouds
                scene("#5AC6EC", "#BFE7F7", 0.70f,
                        shapes(
                                oval("#FFFDF8", 0.00f, 0.62f, 0.40f, 0.16f),
                                oval("#FFFDF8", 0.34f, 0.70f, 0.44f, 0.18f),
                                oval("#FFFDF8", 0.72f, 0.60f, 0.36f, 0.16f),
                                oval("#E8F6FD", 0.14f, 0.18f, 0.22f, 0.09f),
                                oval("#E8F6FD", 0.62f, 0.24f, 0.26f, 0.10f)),
                        decor(at(R.drawable.face_rocket, 0.50f, 0.40f, 0.20f)),
                        anchors()),

                // 19 مزرعه‌ی صبح — a farmyard
                scene("#FFE9A8", "#A8DC85", 0.58f,
                        shapes(
                                rect("#E1251B", 0.58f, 0.28f, 0.34f, 0.30f),
                                triUp("#B31C14", 0.54f, 0.14f, 0.42f, 0.16f),
                                rect("#8A5A2B", 0.70f, 0.42f, 0.10f, 0.16f),
                                rect("#B5793A", 0.00f, 0.56f, 1.00f, 0.03f),
                                oval("#8FCB68", 0.00f, 0.66f, 0.40f, 0.14f),
                                rect("#D8C68C", 0.06f, 0.74f, 0.26f, 0.08f)),
                        decor(at(R.drawable.face_sun, 0.16f, 0.16f, 0.18f),
                                at(R.drawable.face_dog, 0.40f, 0.72f, 0.16f)),
                        anchors()),

                // 20 شب ستاره‌ها — the garden after dark
                scene("#1B2450", "#2E3A72", 0.68f,
                        shapes(
                                oval("#FFFDF8", 0.08f, 0.12f, 0.025f, 0.04f),
                                oval("#FFFDF8", 0.24f, 0.22f, 0.02f, 0.03f),
                                oval("#FFFDF8", 0.44f, 0.10f, 0.03f, 0.045f),
                                oval("#FFFDF8", 0.62f, 0.26f, 0.02f, 0.03f),
                                oval("#FFFDF8", 0.82f, 0.16f, 0.025f, 0.04f),
                                oval("#3C4A88", 0.00f, 0.62f, 0.48f, 0.16f),
                                oval("#3C4A88", 0.56f, 0.64f, 0.50f, 0.16f),
                                rect("#44508E", 0.00f, 0.80f, 1.00f, 0.20f)),
                        decor(at(R.drawable.face_shape_moon, 0.84f, 0.18f, 0.20f),
                                at(R.drawable.face_tree, 0.14f, 0.58f, 0.22f)),
                        anchors()),
        };
    }
}
