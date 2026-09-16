package ir.brandimo.pashmak.data.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * The places the told tales happen in.
 *
 * <p>Forty tales of five scenes each is two hundred places, and two hundred
 * hand-typed scene literals would be neither writable nor readable. So a place is
 * built here instead — a meadow, a forest, a room, the sea — and takes its sky,
 * ground and accent colours from the tale using it. Same shapes, different light:
 * a dawn meadow and a dusk meadow are recognisably the same field at different
 * hours, which is what a story that moves through a day actually needs.
 */
final class TaleSceneKit {

    /** A tale's colour scheme, applied to every place it visits. */
    static final class Mood {
        final String sky;
        final String ground;
        final String accent;
        final String shade;

        Mood(String sky, String ground, String accent, String shade) {
            this.sky = sky;
            this.ground = ground;
            this.accent = accent;
            this.shade = shade;
        }
    }

    /* Times of day, reused across tales so a morning always looks like a morning. */
    static final Mood DAWN = new Mood("#FFD9A8", "#A8DC85", "#F7941D", "#2E7A1D");
    static final Mood NOON = new Mood("#BFE7F7", "#A8DC85", "#43B02A", "#2E7A1D");
    static final Mood DUSK = new Mood("#F0A87A", "#7FA86A", "#E1251B", "#4A5A42");
    static final Mood NIGHT = new Mood("#1B2450", "#2E3A72", "#FFD97A", "#3C4A88");
    static final Mood SEA = new Mood("#1FA6D6", "#E8D9A8", "#43B02A", "#14759A");
    static final Mood SNOW = new Mood("#DDEBF7", "#FFFDF8", "#C7DCEE", "#A9C4DC");
    static final Mood INDOORS = new Mood("#FFF1E0", "#B5793A", "#E1251B", "#8A5A2B");

    private TaleSceneKit() {
    }

    /** Open grass under a wide sky: the default outdoors. */
    static MissionScene meadow(Mood mood) {
        return scene(mood.sky, mood.ground, 0.62f,
                shapes(oval(mood.shade, 0.00f, 0.56f, 0.46f, 0.18f),
                        oval(mood.shade, 0.60f, 0.55f, 0.50f, 0.20f),
                        rect(mood.shade, 0.00f, 0.76f, 1.00f, 0.03f),
                        oval("#66FFFFFF", 0.12f, 0.10f, 0.22f, 0.09f),
                        oval("#66FFFFFF", 0.64f, 0.14f, 0.26f, 0.10f)),
                decor(at(R.drawable.face_flower, 0.10f, 0.86f, 0.12f),
                        at(R.drawable.face_flower, 0.90f, 0.82f, 0.10f)),
                anchors());
    }

    /** Trees crowding in, with a path between them. */
    static MissionScene forest(Mood mood) {
        return scene(mood.sky, mood.ground, 0.58f,
                shapes(triUp(mood.shade, 0.00f, 0.20f, 0.24f, 0.40f),
                        triUp(mood.accent, 0.17f, 0.28f, 0.20f, 0.32f),
                        triUp(mood.shade, 0.72f, 0.18f, 0.26f, 0.42f),
                        triUp(mood.accent, 0.60f, 0.30f, 0.18f, 0.30f),
                        oval(mood.shade, 0.28f, 0.74f, 0.44f, 0.12f),
                        rect("#8A5A2B", 0.07f, 0.56f, 0.04f, 0.10f),
                        rect("#8A5A2B", 0.83f, 0.56f, 0.04f, 0.10f)),
                decor(at(R.drawable.face_tree, 0.42f, 0.50f, 0.20f)),
                anchors());
    }

    /** A room with a skirting board, a window and a rug. */
    static MissionScene room(Mood mood) {
        return scene(mood.sky, mood.ground, 0.64f,
                shapes(rect("#F5E0C8", 0.00f, 0.00f, 1.00f, 0.64f),
                        rect(mood.shade, 0.00f, 0.60f, 1.00f, 0.04f),
                        box("#BFE7F7", 0.60f, 0.16f, 0.30f, 0.30f, 0.06f),
                        rect("#FFFDF8", 0.745f, 0.16f, 0.02f, 0.30f),
                        rect("#FFFDF8", 0.60f, 0.295f, 0.30f, 0.02f),
                        oval(mood.accent, 0.08f, 0.76f, 0.44f, 0.12f)),
                decor(at(R.drawable.face_house, 0.24f, 0.44f, 0.16f)),
                anchors());
    }

    /** Under the water, with weed and a sandy floor. */
    static MissionScene sea(Mood mood) {
        return scene(mood.sky, mood.ground, 0.74f,
                shapes(oval("#4FBBE0", 0.08f, 0.10f, 0.18f, 0.07f),
                        oval("#4FBBE0", 0.58f, 0.18f, 0.22f, 0.08f),
                        box(mood.accent, 0.06f, 0.46f, 0.05f, 0.30f, 0.50f),
                        box(mood.accent, 0.15f, 0.54f, 0.04f, 0.22f, 0.50f),
                        box(mood.accent, 0.86f, 0.42f, 0.05f, 0.34f, 0.50f),
                        oval("#D8C68C", 0.22f, 0.82f, 0.30f, 0.09f)),
                decor(at(R.drawable.face_fish, 0.68f, 0.58f, 0.13f)),
                anchors());
    }

    /** Above the clouds, or the night sky if the mood is dark. */
    static MissionScene aloft(Mood mood) {
        return scene(mood.sky, mood.ground, 0.70f,
                shapes(oval("#EEFFFFFF", 0.00f, 0.62f, 0.40f, 0.16f),
                        oval("#EEFFFFFF", 0.34f, 0.72f, 0.44f, 0.18f),
                        oval("#EEFFFFFF", 0.72f, 0.60f, 0.36f, 0.16f),
                        oval("#88FFFFFF", 0.14f, 0.18f, 0.20f, 0.08f),
                        oval("#88FFFFFF", 0.62f, 0.24f, 0.24f, 0.09f)),
                decor(at(R.drawable.face_cloud, 0.46f, 0.36f, 0.18f)),
                anchors());
    }

    /** Houses along a street. */
    static MissionScene village(Mood mood) {
        return scene(mood.sky, mood.ground, 0.58f,
                shapes(rect(mood.accent, 0.02f, 0.26f, 0.20f, 0.32f),
                        triUp(mood.shade, 0.00f, 0.16f, 0.24f, 0.11f),
                        rect("#1FA6D6", 0.26f, 0.32f, 0.18f, 0.26f),
                        triUp("#14759A", 0.24f, 0.22f, 0.22f, 0.11f),
                        rect("#F7941D", 0.64f, 0.24f, 0.22f, 0.34f),
                        triUp("#D2760B", 0.62f, 0.14f, 0.26f, 0.11f),
                        rect(mood.shade, 0.00f, 0.72f, 1.00f, 0.04f)),
                decor(at(R.drawable.face_house, 0.50f, 0.66f, 0.15f)),
                anchors());
    }

    /** Peaks in the distance, stone underfoot. */
    static MissionScene mountain(Mood mood) {
        return scene(mood.sky, "#9AA0A6", 0.60f,
                shapes(triUp("#7E868D", 0.00f, 0.16f, 0.36f, 0.44f),
                        triUp("#A4ACB2", 0.26f, 0.24f, 0.30f, 0.36f),
                        triUp("#7E868D", 0.64f, 0.14f, 0.38f, 0.46f),
                        oval("#FFFDF8", 0.04f, 0.16f, 0.10f, 0.05f),
                        oval("#FFFDF8", 0.70f, 0.14f, 0.11f, 0.05f),
                        oval("#B4BABF", 0.22f, 0.78f, 0.40f, 0.10f)),
                decor(at(R.drawable.face_shape_triangle, 0.50f, 0.50f, 0.14f)),
                anchors());
    }

    /** A bank with water running past it. */
    static MissionScene river(Mood mood) {
        return scene(mood.sky, mood.ground, 0.58f,
                shapes(oval(mood.shade, 0.00f, 0.50f, 0.44f, 0.16f),
                        rect("#3FA9DC", 0.00f, 0.70f, 1.00f, 0.30f),
                        oval("#5AC6EC", 0.06f, 0.75f, 0.30f, 0.05f),
                        oval("#5AC6EC", 0.54f, 0.84f, 0.34f, 0.05f),
                        rect("#D8C68C", 0.00f, 0.66f, 1.00f, 0.05f)),
                decor(at(R.drawable.face_boat, 0.68f, 0.62f, 0.16f)),
                anchors());
    }

    /** A yard with a wall and something growing in it. */
    static MissionScene garden(Mood mood) {
        return scene(mood.sky, mood.ground, 0.56f,
                shapes(rect("#E8C9A8", 0.00f, 0.42f, 1.00f, 0.14f),
                        rect("#D8B48C", 0.00f, 0.53f, 1.00f, 0.03f),
                        rect(mood.accent, 0.14f, 0.62f, 0.02f, 0.18f),
                        rect(mood.accent, 0.46f, 0.58f, 0.02f, 0.22f),
                        rect(mood.accent, 0.78f, 0.64f, 0.02f, 0.16f),
                        oval("#E1251B", 0.10f, 0.56f, 0.10f, 0.08f),
                        oval("#FFC730", 0.42f, 0.52f, 0.10f, 0.08f),
                        oval("#7A4FA3", 0.74f, 0.58f, 0.10f, 0.08f)),
                decor(at(R.drawable.face_sun, 0.86f, 0.14f, 0.14f)),
                anchors());
    }

    /** Sand and a low sun: somewhere far from home. */
    static MissionScene desert(Mood mood) {
        return scene(mood.sky, "#E8D9A8", 0.60f,
                shapes(oval("#D8C68C", 0.00f, 0.54f, 0.52f, 0.18f),
                        oval("#D8C68C", 0.56f, 0.56f, 0.50f, 0.18f),
                        oval("#C9B67A", 0.24f, 0.78f, 0.44f, 0.12f),
                        rect("#8A5A2B", 0.82f, 0.44f, 0.03f, 0.18f),
                        oval(mood.shade, 0.76f, 0.40f, 0.16f, 0.06f)),
                decor(at(R.drawable.face_sun, 0.16f, 0.18f, 0.16f)),
                anchors());
    }

    /** The scenes a tale visits, in the order its moments call for them. */
    static List<MissionScene> places(MissionScene... items) {
        return new ArrayList<>(Arrays.asList(items));
    }
}
