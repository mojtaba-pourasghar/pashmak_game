package ir.brandimo.pashmak.data.catalog;

import android.graphics.Path;
import android.graphics.RectF;

/**
 * One tappable area of a coloring page. Geometry is stored as fractions of the
 * page box, exactly as the prototype expressed it in CSS percentages, so it
 * scales to any canvas size.
 */
public final class ColorRegion {

    public enum Shape {
        OVAL,
        /** Corner radii given as a fraction of the shorter side. */
        ROUND_FRACTION,
        /** Corner radii given in density-independent pixels. */
        ROUND_DP,
        PILL,
        /** polygon(50% 0, 100% 100%, 0 100%) — apex at top centre. */
        TRIANGLE_UP,
        /** polygon(100% 0, 100% 100%, 0 50%) — apex at the leading edge. */
        TRIANGLE_LEAD
    }

    public final String id;
    public final String label;
    public final int targetColor;
    public final float left;
    public final float top;
    public final float width;
    public final float height;
    public final Shape shape;
    /** topLeft, topRight, bottomRight, bottomLeft. */
    public final float[] corners;

    ColorRegion(String id, String label, int targetColor,
                float left, float top, float width, float height,
                Shape shape, float[] corners) {
        this.id = id;
        this.label = label;
        this.targetColor = targetColor;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.shape = shape;
        this.corners = corners;
    }

    public void bounds(RectF out, float canvasWidth, float canvasHeight) {
        float x = left * canvasWidth;
        float y = top * canvasHeight;
        out.set(x, y, x + width * canvasWidth, y + height * canvasHeight);
    }

    /** Builds the region outline for the given canvas size. */
    public void buildPath(Path out, RectF box, float density) {
        out.reset();
        float w = box.width();
        float h = box.height();
        switch (shape) {
            case OVAL:
                out.addOval(box, Path.Direction.CW);
                break;
            case PILL:
                float pill = Math.min(w, h) / 2f;
                out.addRoundRect(box, pill, pill, Path.Direction.CW);
                break;
            case TRIANGLE_UP:
                out.moveTo(box.centerX(), box.top);
                out.lineTo(box.right, box.bottom);
                out.lineTo(box.left, box.bottom);
                out.close();
                break;
            case TRIANGLE_LEAD:
                out.moveTo(box.right, box.top);
                out.lineTo(box.right, box.bottom);
                out.lineTo(box.left, box.centerY());
                out.close();
                break;
            case ROUND_FRACTION: {
                float base = Math.min(w, h);
                out.addRoundRect(box, radii(base), Path.Direction.CW);
                break;
            }
            case ROUND_DP:
            default: {
                out.addRoundRect(box, radii(density), Path.Direction.CW);
                break;
            }
        }
    }

    /** Expands the 4 corner values into the 8 floats addRoundRect expects. */
    private float[] radii(float scale) {
        float tl = corners[0] * scale;
        float tr = corners[1] * scale;
        float br = corners[2] * scale;
        float bl = corners[3] * scale;
        return new float[]{tl, tl, tr, tr, br, br, bl, bl};
    }
}
