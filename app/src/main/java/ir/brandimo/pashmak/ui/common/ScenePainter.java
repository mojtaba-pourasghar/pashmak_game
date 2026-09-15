package ir.brandimo.pashmak.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import ir.brandimo.pashmak.data.catalog.MissionScene;

/**
 * Paints a drawn place — the sky and ground bands, the shapes that make up the room,
 * and the scenery sitting in it.
 *
 * <p>Shared by the mission scenes and the story scenes so the two look like the same
 * world: one is where the child's scanned drawings land, the other is where Pashmak's
 * stories happen, but a kitchen should be the same kitchen in both.
 */
public final class ScenePainter {

    private final Context context;
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    /** Decor drawables are reused every frame, so they are resolved once. */
    private final SparseArray<Drawable> decorCache = new SparseArray<>();

    public ScenePainter(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /** Everything behind whatever the screen puts on top. */
    public void paint(@NonNull Canvas canvas, @Nullable MissionScene scene,
                      int width, int height) {
        if (scene == null || width == 0 || height == 0) {
            return;
        }
        drawBackdrop(canvas, scene, width, height);
        drawShapes(canvas, scene, width, height);
        drawDecor(canvas, scene, width, height);
    }

    private void drawBackdrop(Canvas canvas, MissionScene scene, int width, int height) {
        float horizon = height * scene.horizon;
        band.setColor(scene.skyColor);
        canvas.drawRect(0f, 0f, width, horizon, band);
        band.setColor(scene.groundColor);
        canvas.drawRect(0f, horizon, width, height, band);
    }

    /** The place itself: walls, counters, rugs, rails, treetops, waves. */
    private void drawShapes(Canvas canvas, MissionScene scene, int width, int height) {
        for (int i = 0; i < scene.shapes.size(); i++) {
            MissionScene.Shape shape = scene.shapes.get(i);
            rect.set(shape.x * width, shape.y * height,
                    (shape.x + shape.width) * width,
                    (shape.y + shape.height) * height);
            band.setColor(shape.color);
            switch (shape.kind) {
                case OVAL:
                    canvas.drawOval(rect, band);
                    break;
                case ROUND: {
                    float radius = Math.min(rect.width(), rect.height()) * shape.radius;
                    canvas.drawRoundRect(rect, radius, radius, band);
                    break;
                }
                case TRIANGLE_UP:
                    path.reset();
                    path.moveTo(rect.centerX(), rect.top);
                    path.lineTo(rect.right, rect.bottom);
                    path.lineTo(rect.left, rect.bottom);
                    path.close();
                    canvas.drawPath(path, band);
                    break;
                case TRIANGLE_DOWN:
                    path.reset();
                    path.moveTo(rect.left, rect.top);
                    path.lineTo(rect.right, rect.top);
                    path.lineTo(rect.centerX(), rect.bottom);
                    path.close();
                    canvas.drawPath(path, band);
                    break;
                case RECT:
                default:
                    canvas.drawRect(rect, band);
                    break;
            }
        }
    }

    private void drawDecor(Canvas canvas, MissionScene scene, int width, int height) {
        float shortest = Math.min(width, height);
        for (int i = 0; i < scene.decor.size(); i++) {
            MissionScene.Decor decor = scene.decor.get(i);
            Drawable drawable = decorCache.get(decor.icon);
            if (drawable == null) {
                drawable = AppCompatResources.getDrawable(context, decor.icon);
                if (drawable == null) {
                    continue;
                }
                decorCache.put(decor.icon, drawable);
            }
            float half = decor.size * shortest / 2f;
            float cx = decor.x * width;
            float cy = decor.y * height;
            drawable.setBounds(Math.round(cx - half), Math.round(cy - half),
                    Math.round(cx + half), Math.round(cy + half));
            drawable.draw(canvas);
        }
    }
}
