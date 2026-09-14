package ir.brandimo.pashmak.data.catalog;

import android.graphics.Color;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.MissionScene.Anchor;
import ir.brandimo.pashmak.data.catalog.MissionScene.Decor;

/** A hand-set scene for each of the twenty missions. */
public final class MissionSceneCatalog {

    private static final MissionScene[] SCENES = build();

    private MissionSceneCatalog() {
    }

    @NonNull
    public static MissionScene forMission(int missionIndex) {
        return SCENES[Palette.wrap(missionIndex, SCENES.length)];
    }

    /** Four places to stand things: two on the ground, one mid, one up high. */
    private static List<Anchor> ground() {
        return anchors(
                new Anchor(.24f, .70f, .30f, -3f),
                new Anchor(.52f, .74f, .26f, 2f),
                new Anchor(.78f, .68f, .28f, -2f),
                new Anchor(.40f, .40f, .22f, 4f));
    }

    private static List<Anchor> anchors(Anchor... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static List<Decor> decor(Decor... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static Decor at(int icon, float x, float y, float size) {
        return new Decor(icon, x, y, size);
    }

    private static int color(String hex) {
        return Color.parseColor(hex);
    }

    private static MissionScene scene(String sky, String ground, float horizon,
                                      List<Decor> decor, List<Anchor> anchors) {
        return new MissionScene(color(sky), color(ground), horizon, decor, anchors);
    }

    private static MissionScene[] build() {
        return new MissionScene[]{
                // 1 bedroom — the window belongs high on the wall
                scene("#FFE6D2", "#B5793A", .70f,
                        decor(at(R.drawable.face_shape_moon, .86f, .16f, .12f)),
                        anchors(new Anchor(.26f, .66f, .34f, 0f),
                                new Anchor(.60f, .60f, .22f, 0f),
                                new Anchor(.50f, .84f, .32f, 0f),
                                new Anchor(.80f, .32f, .24f, 0f))),
                // 2 kitchen
                scene("#DFF0D6", "#F0B98E", .72f,
                        decor(at(R.drawable.face_apple, .88f, .60f, .11f)),
                        ground()),
                // 3 the yard
                scene("#BFE7F7", "#A8DC85", .62f,
                        decor(at(R.drawable.face_sun, .14f, .16f, .15f),
                                at(R.drawable.face_cloud, .70f, .18f, .17f)),
                        ground()),
                // 4 birthday party
                scene("#F3ECF8", "#EDE3F6", .70f,
                        decor(at(R.drawable.face_shape_star, .16f, .18f, .11f),
                                at(R.drawable.face_shape_heart, .84f, .22f, .10f)),
                        ground()),
                // 5 playroom
                scene("#FFF4E9", "#FFE6D2", .72f,
                        decor(at(R.drawable.face_shape_star, .86f, .20f, .11f)),
                        ground()),
                // 6 sea trip — things float on the water
                scene("#BFE7F7", "#1FA6D6", .58f,
                        decor(at(R.drawable.face_cloud, .22f, .16f, .16f),
                                at(R.drawable.face_sun, .82f, .14f, .14f)),
                        anchors(new Anchor(.30f, .62f, .30f, -4f),
                                new Anchor(.62f, .78f, .24f, 3f),
                                new Anchor(.20f, .24f, .20f, 0f),
                                new Anchor(.80f, .70f, .22f, -2f))),
                // 7 the zoo
                scene("#BFE7F7", "#A8DC85", .60f,
                        decor(at(R.drawable.face_tree, .10f, .52f, .20f),
                                at(R.drawable.face_sun, .86f, .14f, .13f)),
                        ground()),
                // 8 outer space — everything floats
                scene("#18224A", "#2E3D6B", .78f,
                        decor(at(R.drawable.face_shape_star, .18f, .20f, .09f),
                                at(R.drawable.face_shape_star, .74f, .12f, .07f),
                                at(R.drawable.face_shape_star, .52f, .30f, .06f)),
                        anchors(new Anchor(.26f, .50f, .30f, -6f),
                                new Anchor(.58f, .30f, .22f, 8f),
                                new Anchor(.80f, .58f, .24f, -4f),
                                new Anchor(.42f, .72f, .26f, 3f))),
                // 9 first day at school
                scene("#E6F4FB", "#F1EBE3", .70f,
                        decor(at(R.drawable.face_sun, .86f, .16f, .13f)),
                        ground()),
                // 10 the playground
                scene("#BFE7F7", "#A8DC85", .62f,
                        decor(at(R.drawable.face_cloud, .18f, .16f, .16f),
                                at(R.drawable.face_sun, .84f, .16f, .13f)),
                        ground()),
                // 11 the farm
                scene("#BFE7F7", "#C6E9A8", .58f,
                        decor(at(R.drawable.face_sun, .14f, .14f, .14f),
                                at(R.drawable.face_tree, .88f, .48f, .18f)),
                        ground()),
                // 12 pizza
                scene("#FFF6E8", "#F0B98E", .72f,
                        decor(at(R.drawable.face_flower, .90f, .24f, .10f)),
                        ground()),
                // 13 the busy city
                scene("#DFF3FB", "#9AA0A6", .64f,
                        decor(at(R.drawable.face_cloud, .24f, .14f, .15f),
                                at(R.drawable.face_sun, .80f, .14f, .12f)),
                        ground()),
                // 14 the train
                scene("#BFE7F7", "#C6E9A8", .62f,
                        decor(at(R.drawable.face_cloud, .70f, .16f, .16f),
                                at(R.drawable.face_tree, .10f, .50f, .18f)),
                        ground()),
                // 15 under the sea
                scene("#3FA9DC", "#14567F", .55f,
                        decor(at(R.drawable.face_fish, .84f, .26f, .12f),
                                at(R.drawable.face_drop, .16f, .20f, .08f)),
                        anchors(new Anchor(.28f, .60f, .30f, -5f),
                                new Anchor(.58f, .76f, .26f, 4f),
                                new Anchor(.78f, .56f, .24f, -3f),
                                new Anchor(.44f, .34f, .20f, 6f))),
                // 16 forest camp at night
                scene("#2E3D6B", "#2E7A1D", .66f,
                        decor(at(R.drawable.face_shape_moon, .84f, .16f, .12f),
                                at(R.drawable.face_shape_star, .22f, .18f, .07f)),
                        ground()),
                // 17 a rainy day
                scene("#9AA0A6", "#A9D294", .64f,
                        decor(at(R.drawable.face_cloud, .28f, .14f, .18f),
                                at(R.drawable.face_drop, .70f, .28f, .08f)),
                        ground()),
                // 18 the circus
                scene("#7A4FA3", "#E1251B", .70f,
                        decor(at(R.drawable.face_shape_star, .16f, .18f, .10f),
                                at(R.drawable.face_shape_star, .84f, .22f, .08f)),
                        ground()),
                // 19 the robot workshop
                scene("#DFF3FB", "#C3D3DC", .70f,
                        decor(at(R.drawable.face_shape_hexagon, .88f, .20f, .11f)),
                        ground()),
                // 20 snowy winter
                scene("#E8F6FD", "#FFFDF8", .62f,
                        decor(at(R.drawable.face_cloud, .20f, .16f, .16f),
                                at(R.drawable.face_shape_moon, .84f, .18f, .11f)),
                        ground()),
        };
    }
}
