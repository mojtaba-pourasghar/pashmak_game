package ir.brandimo.pashmak.data.catalog;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ir.brandimo.pashmak.data.catalog.MissionScene.Anchor;
import ir.brandimo.pashmak.data.catalog.MissionScene.Decor;
import ir.brandimo.pashmak.data.catalog.MissionScene.Shape;

/**
 * The shorthand both scene catalogs are written in. Colours are hex strings and
 * every coordinate is a fraction of the scene, so a place reads almost like a
 * drawing instruction and scales to any screen.
 */
final class SceneBuilder {

    static MissionScene scene(String sky, String ground, float horizon,
                              List<Shape> shapes, List<Decor> decor,
                              List<Anchor> anchors) {
        return new MissionScene(Color.parseColor(sky), Color.parseColor(ground),
                horizon, shapes, decor, anchors);
    }

    static List<Shape> shapes(Shape... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    static List<Decor> decor(Decor... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    static List<Anchor> anchors(Anchor... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    static Decor at(int icon, float x, float y, float size) {
        return new Decor(icon, x, y, size);
    }

    static Shape rect(String color, float x, float y, float w, float h) {
        return new Shape(Shape.Kind.RECT, Color.parseColor(color), x, y, w, h, 0f);
    }

    static Shape box(String color, float x, float y, float w, float h, float radius) {
        return new Shape(Shape.Kind.ROUND, Color.parseColor(color), x, y, w, h, radius);
    }

    static Shape oval(String color, float x, float y, float w, float h) {
        return new Shape(Shape.Kind.OVAL, Color.parseColor(color), x, y, w, h, 0f);
    }

    static Shape triUp(String color, float x, float y, float w, float h) {
        return new Shape(Shape.Kind.TRIANGLE_UP, Color.parseColor(color), x, y, w, h, 0f);
    }

    static Shape triDown(String color, float x, float y, float w, float h) {
        return new Shape(Shape.Kind.TRIANGLE_DOWN, Color.parseColor(color), x, y, w, h, 0f);
    }

    private SceneBuilder() {
    }
}
