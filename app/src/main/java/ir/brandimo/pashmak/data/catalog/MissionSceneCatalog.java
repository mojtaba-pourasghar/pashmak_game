package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.MissionScene.Anchor;

import static ir.brandimo.pashmak.data.catalog.SceneBuilder.anchors;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.at;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.box;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.decor;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.oval;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.rect;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.scene;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.shapes;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.triDown;
import static ir.brandimo.pashmak.data.catalog.SceneBuilder.triUp;

/**
 * A drawn place for each of the twenty missions — a bedroom with a skirting board,
 * a door and a bedside table; a kitchen with tiled walls and a counter; a seabed
 * with weed and bubbles. Each one reserves a spot for every item the child draws.
 */
public final class MissionSceneCatalog {

    private static final MissionScene[] SCENES = build();

    private MissionSceneCatalog() {
    }

    @NonNull
    public static MissionScene forMission(int missionIndex) {
        return SCENES[Palette.wrap(missionIndex, SCENES.length)];
    }

    private static MissionScene[] build() {
        return new MissionScene[]{
                // 1 اتاق خواب
                scene("#FFF1E0", "#B5793A", 0.64f,
                        shapes(
                                rect("#E8C9A8", 0.000f, 0.640f, 1.000f, 0.030f),
                                box("#8A5A2B", 0.800f, 0.460f, 0.160f, 0.200f, 0.15f),
                                rect("#6B4423", 0.820f, 0.520f, 0.120f, 0.020f),
                                box("#C8B6DC", 0.100f, 0.120f, 0.130f, 0.170f, 0.12f),
                                box("#FFFDF8", 0.115f, 0.135f, 0.100f, 0.130f, 0.1f),
                                box("#8A5A2B", 0.020f, 0.220f, 0.100f, 0.420f, 0.1f)),
                        decor(),
                        anchors(
                                new Anchor(0.30f, 0.56f, 0.34f, -2f),
                                new Anchor(0.86f, 0.38f, 0.13f, 0f),
                                new Anchor(0.46f, 0.84f, 0.36f, 0f),
                                new Anchor(0.56f, 0.26f, 0.24f, 0f))),
                // 2 آشپزخونه
                scene("#D7EEF6", "#E8C9A8", 0.64f,
                        shapes(
                                rect("#FFFDF8", 0.020f, 0.500f, 0.960f, 0.070f),
                                rect("#C9A882", 0.020f, 0.570f, 0.960f, 0.070f),
                                oval("#C3D3DC", 0.100f, 0.505f, 0.160f, 0.045f),
                                box("#8A5A2B", 0.060f, 0.660f, 0.200f, 0.160f, 0.1f),
                                box("#8A5A2B", 0.300f, 0.660f, 0.200f, 0.160f, 0.1f),
                                rect("#FFFFFF", 0.040f, 0.160f, 0.130f, 0.130f),
                                rect("#FFFFFF", 0.200f, 0.160f, 0.130f, 0.130f),
                                rect("#FFFFFF", 0.360f, 0.160f, 0.130f, 0.130f),
                                rect("#FFFFFF", 0.520f, 0.160f, 0.130f, 0.130f),
                                rect("#FFFFFF", 0.680f, 0.160f, 0.130f, 0.130f),
                                rect("#FFFFFF", 0.840f, 0.160f, 0.130f, 0.130f)),
                        decor(),
                        anchors(
                                new Anchor(0.84f, 0.40f, 0.30f, 0f),
                                new Anchor(0.42f, 0.44f, 0.15f, 0f),
                                new Anchor(0.28f, 0.74f, 0.30f, 0f),
                                new Anchor(0.60f, 0.45f, 0.09f, 0f))),
                // 3 حیاط
                scene("#BFE7F7", "#A8DC85", 0.58f,
                        shapes(
                                box("#E8C9A8", 0.300f, 0.760f, 0.420f, 0.140f, 0.4f),
                                rect("#A9D294", 0.000f, 0.560f, 1.000f, 0.030f)),
                        decor(
                                at(R.drawable.face_sun, 0.12f, 0.14f, 0.15f),
                                at(R.drawable.face_cloud, 0.72f, 0.16f, 0.17f)),
                        anchors(
                                new Anchor(0.20f, 0.44f, 0.34f, 0f),
                                new Anchor(0.54f, 0.48f, 0.26f, 0f),
                                new Anchor(0.80f, 0.74f, 0.15f, 0f),
                                new Anchor(0.46f, 0.66f, 0.40f, 0f))),
                // 4 جشن تولد
                scene("#F7EFFA", "#EDE3F6", 0.68f,
                        shapes(
                                rect("#FFFDF8", 0.140f, 0.600f, 0.720f, 0.050f),
                                rect("#D8CFE4", 0.200f, 0.650f, 0.040f, 0.140f),
                                rect("#D8CFE4", 0.760f, 0.650f, 0.040f, 0.140f),
                                triDown("#E1251B", 0.060f, 0.040f, 0.100f, 0.100f),
                                triDown("#FFC730", 0.210f, 0.040f, 0.100f, 0.100f),
                                triDown("#43B02A", 0.360f, 0.040f, 0.100f, 0.100f),
                                triDown("#1FA6D6", 0.510f, 0.040f, 0.100f, 0.100f),
                                triDown("#7A4FA3", 0.660f, 0.040f, 0.100f, 0.100f),
                                triDown("#F7941D", 0.810f, 0.040f, 0.100f, 0.100f)),
                        decor(),
                        anchors(
                                new Anchor(0.50f, 0.50f, 0.22f, 0f),
                                new Anchor(0.18f, 0.26f, 0.20f, 0f),
                                new Anchor(0.78f, 0.30f, 0.15f, 0f),
                                new Anchor(0.74f, 0.74f, 0.20f, 0f))),
                // 5 اتاق بازی
                scene("#FFF7EE", "#FFE6D2", 0.66f,
                        shapes(
                                rect("#8A5A2B", 0.080f, 0.280f, 0.360f, 0.035f),
                                oval("#D9EDF7", 0.260f, 0.740f, 0.480f, 0.180f),
                                box("#F0B98E", 0.780f, 0.540f, 0.180f, 0.140f, 0.15f)),
                        decor(
                                at(R.drawable.face_shape_star, 0.86f, 0.16f, 0.10f)),
                        anchors(
                                new Anchor(0.32f, 0.60f, 0.20f, 0f),
                                new Anchor(0.56f, 0.56f, 0.22f, 0f),
                                new Anchor(0.80f, 0.72f, 0.20f, 0f),
                                new Anchor(0.18f, 0.22f, 0.14f, 0f))),
                // 6 سفر به دریا
                scene("#BFE7F7", "#2FAEDD", 0.52f,
                        shapes(
                                rect("#FFE6D2", 0.000f, 0.880f, 1.000f, 0.120f),
                                box("#5BBBE3", 0.020f, 0.600f, 0.160f, 0.015f, 0.5f),
                                box("#5BBBE3", 0.220f, 0.660f, 0.160f, 0.015f, 0.5f),
                                box("#5BBBE3", 0.420f, 0.600f, 0.160f, 0.015f, 0.5f),
                                box("#5BBBE3", 0.620f, 0.660f, 0.160f, 0.015f, 0.5f),
                                box("#5BBBE3", 0.820f, 0.600f, 0.160f, 0.015f, 0.5f)),
                        decor(
                                at(R.drawable.face_cloud, 0.20f, 0.14f, 0.16f)),
                        anchors(
                                new Anchor(0.34f, 0.56f, 0.30f, -3f),
                                new Anchor(0.68f, 0.74f, 0.16f, 0f),
                                new Anchor(0.82f, 0.16f, 0.16f, 0f),
                                new Anchor(0.16f, 0.90f, 0.12f, 0f))),
                // 7 باغ‌وحش
                scene("#BFE7F7", "#A8DC85", 0.56f,
                        shapes(
                                box("#E8C9A8", 0.020f, 0.800f, 0.960f, 0.100f, 0.2f),
                                rect("#8A5A2B", 0.040f, 0.580f, 0.020f, 0.120f),
                                rect("#8A5A2B", 0.180f, 0.580f, 0.020f, 0.120f),
                                rect("#8A5A2B", 0.320f, 0.580f, 0.020f, 0.120f),
                                rect("#8A5A2B", 0.460f, 0.580f, 0.020f, 0.120f),
                                rect("#8A5A2B", 0.600f, 0.580f, 0.020f, 0.120f),
                                rect("#8A5A2B", 0.740f, 0.580f, 0.020f, 0.120f),
                                rect("#8A5A2B", 0.880f, 0.580f, 0.020f, 0.120f),
                                rect("#8A5A2B", 0.040f, 0.620f, 0.920f, 0.015f)),
                        decor(
                                at(R.drawable.face_sun, 0.88f, 0.12f, 0.13f),
                                at(R.drawable.face_tree, 0.08f, 0.44f, 0.18f)),
                        anchors(
                                new Anchor(0.30f, 0.48f, 0.32f, 0f),
                                new Anchor(0.58f, 0.40f, 0.30f, 0f),
                                new Anchor(0.80f, 0.54f, 0.20f, 0f),
                                new Anchor(0.46f, 0.80f, 0.16f, 0f))),
                // 8 فضا
                scene("#161F44", "#2A3566", 0.80f,
                        shapes(
                                oval("#3B2E6B", 0.620f, 0.700f, 0.600f, 0.340f),
                                oval("#FFFDF8", 0.030f, 0.050f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.092f, 0.163f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.153f, 0.276f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.215f, 0.389f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.277f, 0.502f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.339f, 0.615f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.400f, 0.728f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.462f, 0.041f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.524f, 0.154f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.585f, 0.267f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.647f, 0.380f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.709f, 0.493f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.770f, 0.606f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.832f, 0.719f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.894f, 0.032f, 0.012f, 0.012f),
                                oval("#FFFDF8", 0.956f, 0.145f, 0.012f, 0.012f)),
                        decor(
                                at(R.drawable.face_shape_star, 0.14f, 0.16f, 0.08f)),
                        anchors(
                                new Anchor(0.28f, 0.46f, 0.30f, -6f),
                                new Anchor(0.60f, 0.24f, 0.14f, 0f),
                                new Anchor(0.82f, 0.56f, 0.20f, 0f),
                                new Anchor(0.44f, 0.74f, 0.24f, 4f))),
                // 9 مدرسه
                scene("#EAF4FB", "#F1EBE3", 0.66f,
                        shapes(
                                rect("#D8CFC3", 0.000f, 0.660f, 1.000f, 0.030f),
                                box("#8A5A2B", 0.600f, 0.520f, 0.300f, 0.140f, 0.08f),
                                rect("#6B4423", 0.640f, 0.660f, 0.030f, 0.120f),
                                rect("#6B4423", 0.840f, 0.660f, 0.030f, 0.120f)),
                        decor(
                                at(R.drawable.face_sun, 0.88f, 0.12f, 0.12f)),
                        anchors(
                                new Anchor(0.22f, 0.72f, 0.22f, 0f),
                                new Anchor(0.74f, 0.48f, 0.12f, 0f),
                                new Anchor(0.34f, 0.32f, 0.36f, 0f),
                                new Anchor(0.88f, 0.26f, 0.12f, 0f))),
                // 10 زمین بازی
                scene("#BFE7F7", "#A8DC85", 0.60f,
                        shapes(
                                oval("#FFE6D2", 0.300f, 0.740f, 0.440f, 0.180f)),
                        decor(
                                at(R.drawable.face_cloud, 0.16f, 0.14f, 0.16f),
                                at(R.drawable.face_sun, 0.86f, 0.14f, 0.13f)),
                        anchors(
                                new Anchor(0.24f, 0.48f, 0.32f, 0f),
                                new Anchor(0.58f, 0.56f, 0.30f, 0f),
                                new Anchor(0.82f, 0.76f, 0.14f, 0f),
                                new Anchor(0.46f, 0.34f, 0.22f, 0f))),
                // 11 مزرعه
                scene("#BFE7F7", "#C6E9A8", 0.56f,
                        shapes(
                                box("#93C77A", 0.020f, 0.700f, 0.960f, 0.030f, 0.5f),
                                box("#93C77A", 0.020f, 0.760f, 0.960f, 0.030f, 0.5f),
                                box("#93C77A", 0.020f, 0.820f, 0.960f, 0.030f, 0.5f),
                                box("#93C77A", 0.020f, 0.880f, 0.960f, 0.030f, 0.5f),
                                box("#93C77A", 0.020f, 0.940f, 0.960f, 0.030f, 0.5f)),
                        decor(
                                at(R.drawable.face_sun, 0.10f, 0.12f, 0.13f),
                                at(R.drawable.face_tree, 0.90f, 0.44f, 0.16f)),
                        anchors(
                                new Anchor(0.34f, 0.52f, 0.28f, 0f),
                                new Anchor(0.58f, 0.62f, 0.14f, 0f),
                                new Anchor(0.74f, 0.50f, 0.26f, 0f),
                                new Anchor(0.16f, 0.42f, 0.28f, 0f))),
                // 12 پیتزا
                scene("#FFF6E8", "#EBD9C0", 0.52f,
                        shapes(
                                rect("#E8C9A8", 0.000f, 0.520f, 1.000f, 0.060f),
                                oval("#B5793A", 0.240f, 0.500f, 0.520f, 0.400f),
                                oval("#C9A882", 0.280f, 0.530f, 0.440f, 0.330f),
                                oval("#FFFDF8", 0.080f, 0.800f, 0.030f, 0.020f),
                                oval("#FFFDF8", 0.140f, 0.800f, 0.030f, 0.020f),
                                oval("#FFFDF8", 0.200f, 0.800f, 0.030f, 0.020f)),
                        decor(),
                        anchors(
                                new Anchor(0.50f, 0.68f, 0.30f, 0f),
                                new Anchor(0.36f, 0.62f, 0.14f, 0f),
                                new Anchor(0.62f, 0.62f, 0.12f, 0f),
                                new Anchor(0.50f, 0.34f, 0.12f, 0f))),
                // 13 شهر
                scene("#DFF3FB", "#8E959B", 0.62f,
                        shapes(
                                rect("#C8CCD0", 0.000f, 0.600f, 1.000f, 0.040f),
                                rect("#FFFDF8", 0.060f, 0.800f, 0.080f, 0.012f),
                                rect("#FFFDF8", 0.220f, 0.800f, 0.080f, 0.012f),
                                rect("#FFFDF8", 0.380f, 0.800f, 0.080f, 0.012f),
                                rect("#FFFDF8", 0.540f, 0.800f, 0.080f, 0.012f),
                                rect("#FFFDF8", 0.700f, 0.800f, 0.080f, 0.012f),
                                rect("#FFFDF8", 0.860f, 0.800f, 0.080f, 0.012f)),
                        decor(
                                at(R.drawable.face_cloud, 0.22f, 0.12f, 0.15f),
                                at(R.drawable.face_sun, 0.84f, 0.12f, 0.12f)),
                        anchors(
                                new Anchor(0.30f, 0.72f, 0.26f, 0f),
                                new Anchor(0.88f, 0.50f, 0.14f, 0f),
                                new Anchor(0.20f, 0.36f, 0.34f, 0f),
                                new Anchor(0.66f, 0.70f, 0.28f, 0f))),
                // 14 قطار
                scene("#BFE7F7", "#C6E9A8", 0.60f,
                        shapes(
                                rect("#8A5A2B", 0.000f, 0.740f, 1.000f, 0.030f),
                                rect("#6B4423", 0.020f, 0.760f, 0.060f, 0.030f),
                                rect("#6B4423", 0.130f, 0.760f, 0.060f, 0.030f),
                                rect("#6B4423", 0.240f, 0.760f, 0.060f, 0.030f),
                                rect("#6B4423", 0.350f, 0.760f, 0.060f, 0.030f),
                                rect("#6B4423", 0.460f, 0.760f, 0.060f, 0.030f),
                                rect("#6B4423", 0.570f, 0.760f, 0.060f, 0.030f),
                                rect("#6B4423", 0.680f, 0.760f, 0.060f, 0.030f),
                                rect("#6B4423", 0.790f, 0.760f, 0.060f, 0.030f),
                                rect("#6B4423", 0.900f, 0.760f, 0.060f, 0.030f)),
                        decor(
                                at(R.drawable.face_cloud, 0.70f, 0.14f, 0.16f),
                                at(R.drawable.face_tree, 0.06f, 0.46f, 0.16f)),
                        anchors(
                                new Anchor(0.26f, 0.56f, 0.28f, 0f),
                                new Anchor(0.56f, 0.58f, 0.24f, 0f),
                                new Anchor(0.50f, 0.80f, 0.40f, 0f),
                                new Anchor(0.84f, 0.44f, 0.24f, 0f))),
                // 15 زیر دریا
                scene("#3FA9DC", "#1A6A94", 0.55f,
                        shapes(
                                rect("#E8C9A8", 0.000f, 0.840f, 1.000f, 0.160f),
                                box("#2E7A1D", 0.060f, 0.660f, 0.035f, 0.180f, 0.5f),
                                box("#2E7A1D", 0.140f, 0.720f, 0.035f, 0.120f, 0.5f),
                                box("#2E7A1D", 0.880f, 0.680f, 0.035f, 0.160f, 0.5f),
                                box("#2E7A1D", 0.800f, 0.740f, 0.035f, 0.100f, 0.5f),
                                oval("#9BD9F0", 0.220f, 0.220f, 0.030f, 0.030f),
                                oval("#9BD9F0", 0.300f, 0.140f, 0.020f, 0.020f),
                                oval("#9BD9F0", 0.700f, 0.280f, 0.025f, 0.025f)),
                        decor(
                                at(R.drawable.face_fish, 0.86f, 0.22f, 0.11f)),
                        anchors(
                                new Anchor(0.30f, 0.58f, 0.28f, -4f),
                                new Anchor(0.58f, 0.74f, 0.24f, 0f),
                                new Anchor(0.78f, 0.56f, 0.22f, 0f),
                                new Anchor(0.44f, 0.30f, 0.14f, 0f))),
                // 16 کمپ جنگل
                scene("#2A3566", "#2E7A1D", 0.64f,
                        shapes(
                                oval("#FFFDF8", 0.040f, 0.040f, 0.011f, 0.011f),
                                oval("#FFFDF8", 0.131f, 0.111f, 0.011f, 0.011f),
                                oval("#FFFDF8", 0.223f, 0.182f, 0.011f, 0.011f),
                                oval("#FFFDF8", 0.314f, 0.253f, 0.011f, 0.011f),
                                oval("#FFFDF8", 0.405f, 0.324f, 0.011f, 0.011f),
                                oval("#FFFDF8", 0.496f, 0.395f, 0.011f, 0.011f),
                                oval("#FFFDF8", 0.588f, 0.026f, 0.011f, 0.011f),
                                oval("#FFFDF8", 0.679f, 0.097f, 0.011f, 0.011f),
                                oval("#FFFDF8", 0.770f, 0.168f, 0.011f, 0.011f),
                                oval("#FFFDF8", 0.862f, 0.239f, 0.011f, 0.011f),
                                box("#8A5A2B", 0.060f, 0.780f, 0.180f, 0.035f, 0.5f),
                                box("#8A5A2B", 0.760f, 0.800f, 0.160f, 0.035f, 0.5f)),
                        decor(
                                at(R.drawable.face_shape_moon, 0.86f, 0.14f, 0.11f),
                                at(R.drawable.face_shape_star, 0.20f, 0.16f, 0.07f)),
                        anchors(
                                new Anchor(0.30f, 0.52f, 0.32f, 0f),
                                new Anchor(0.56f, 0.72f, 0.16f, 0f),
                                new Anchor(0.82f, 0.46f, 0.26f, 0f),
                                new Anchor(0.14f, 0.62f, 0.22f, 0f))),
                // 17 روز بارونی
                scene("#9AA0A6", "#A9D294", 0.62f,
                        shapes(
                                box("#A9C9DC", 0.050f, 0.300f, 0.008f, 0.060f, 0.5f),
                                box("#A9C9DC", 0.145f, 0.380f, 0.008f, 0.060f, 0.5f),
                                box("#A9C9DC", 0.240f, 0.460f, 0.008f, 0.060f, 0.5f),
                                box("#A9C9DC", 0.335f, 0.300f, 0.008f, 0.060f, 0.5f),
                                box("#A9C9DC", 0.430f, 0.380f, 0.008f, 0.060f, 0.5f),
                                box("#A9C9DC", 0.525f, 0.460f, 0.008f, 0.060f, 0.5f),
                                box("#A9C9DC", 0.620f, 0.300f, 0.008f, 0.060f, 0.5f),
                                box("#A9C9DC", 0.715f, 0.380f, 0.008f, 0.060f, 0.5f),
                                box("#A9C9DC", 0.810f, 0.460f, 0.008f, 0.060f, 0.5f),
                                box("#A9C9DC", 0.905f, 0.300f, 0.008f, 0.060f, 0.5f),
                                oval("#8FAEC0", 0.160f, 0.820f, 0.200f, 0.050f),
                                oval("#8FAEC0", 0.620f, 0.860f, 0.220f, 0.050f)),
                        decor(
                                at(R.drawable.face_cloud, 0.26f, 0.12f, 0.18f),
                                at(R.drawable.face_drop, 0.72f, 0.24f, 0.07f)),
                        anchors(
                                new Anchor(0.70f, 0.18f, 0.20f, 0f),
                                new Anchor(0.32f, 0.52f, 0.28f, 0f),
                                new Anchor(0.54f, 0.40f, 0.10f, 0f),
                                new Anchor(0.50f, 0.72f, 0.36f, 0f))),
                // 18 سیرک
                scene("#7A4FA3", "#C1470F", 0.66f,
                        shapes(
                                oval("#FFC730", 0.080f, 0.660f, 0.840f, 0.280f),
                                oval("#E1251B", 0.120f, 0.690f, 0.760f, 0.220f),
                                triDown("#FFFDF8", 0.020f, 0.000f, 0.125f, 0.100f),
                                triDown("#F7941D", 0.145f, 0.000f, 0.125f, 0.100f),
                                triDown("#FFFDF8", 0.270f, 0.000f, 0.125f, 0.100f),
                                triDown("#F7941D", 0.395f, 0.000f, 0.125f, 0.100f),
                                triDown("#FFFDF8", 0.520f, 0.000f, 0.125f, 0.100f),
                                triDown("#F7941D", 0.645f, 0.000f, 0.125f, 0.100f),
                                triDown("#FFFDF8", 0.770f, 0.000f, 0.125f, 0.100f),
                                triDown("#F7941D", 0.895f, 0.000f, 0.125f, 0.100f)),
                        decor(
                                at(R.drawable.face_shape_star, 0.14f, 0.18f, 0.09f)),
                        anchors(
                                new Anchor(0.50f, 0.44f, 0.36f, 0f),
                                new Anchor(0.26f, 0.70f, 0.22f, 0f),
                                new Anchor(0.74f, 0.76f, 0.14f, 0f),
                                new Anchor(0.80f, 0.34f, 0.20f, 0f))),
                // 19 کارگاه ربات
                scene("#DFF3FB", "#BCC6CE", 0.66f,
                        shapes(
                                rect("#8A94A0", 0.020f, 0.580f, 0.960f, 0.050f),
                                rect("#6E7883", 0.060f, 0.630f, 0.880f, 0.060f),
                                oval("#9FB0BD", 0.080f, 0.140f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.180f, 0.140f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.280f, 0.140f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.380f, 0.140f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.480f, 0.140f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.580f, 0.140f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.680f, 0.140f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.780f, 0.140f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.080f, 0.240f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.180f, 0.240f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.280f, 0.240f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.380f, 0.240f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.480f, 0.240f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.580f, 0.240f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.680f, 0.240f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.780f, 0.240f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.080f, 0.340f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.180f, 0.340f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.280f, 0.340f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.380f, 0.340f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.480f, 0.340f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.580f, 0.340f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.680f, 0.340f, 0.018f, 0.018f),
                                oval("#9FB0BD", 0.780f, 0.340f, 0.018f, 0.018f)),
                        decor(
                                at(R.drawable.face_shape_hexagon, 0.90f, 0.18f, 0.10f)),
                        anchors(
                                new Anchor(0.32f, 0.44f, 0.26f, 0f),
                                new Anchor(0.58f, 0.46f, 0.22f, 0f),
                                new Anchor(0.78f, 0.72f, 0.12f, 0f),
                                new Anchor(0.16f, 0.24f, 0.14f, 0f))),
                // 20 زمستون
                scene("#DCEEF8", "#FFFDF8", 0.60f,
                        shapes(
                                oval("#FFFFFF", 0.020f, 0.660f, 0.420f, 0.160f),
                                oval("#FFFFFF", 0.580f, 0.700f, 0.440f, 0.160f),
                                rect("#8A5A2B", 0.120f, 0.360f, 0.030f, 0.260f),
                                rect("#8A5A2B", 0.860f, 0.340f, 0.030f, 0.280f),
                                oval("#FFFFFF", 0.020f, 0.020f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.093f, 0.112f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.166f, 0.203f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.239f, 0.295f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.312f, 0.387f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.386f, 0.479f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.459f, 0.010f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.532f, 0.102f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.605f, 0.194f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.678f, 0.285f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.751f, 0.377f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.824f, 0.469f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.897f, 0.000f, 0.014f, 0.014f),
                                oval("#FFFFFF", 0.010f, 0.092f, 0.014f, 0.014f)),
                        decor(
                                at(R.drawable.face_cloud, 0.20f, 0.14f, 0.15f),
                                at(R.drawable.face_shape_moon, 0.84f, 0.16f, 0.10f)),
                        anchors(
                                new Anchor(0.32f, 0.50f, 0.32f, 0f),
                                new Anchor(0.62f, 0.24f, 0.10f, 0f),
                                new Anchor(0.50f, 0.72f, 0.22f, 0f),
                                new Anchor(0.80f, 0.62f, 0.26f, 0f))),
        };
    }
}
