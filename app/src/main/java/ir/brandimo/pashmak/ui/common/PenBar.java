package ir.brandimo.pashmak.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import ir.brandimo.pashmak.data.catalog.Palette;

/**
 * The colours, drawn as pencils in a tray. A child who cannot read still knows
 * what a pencil is, and the one they picked lifts out of the row so the choice is
 * obvious at a glance.
 *
 * Everything is drawn by this one view rather than by a child view per colour —
 * eight nested views would cost more and make the "lift" harder to coordinate.
 */
public class PenBar extends View {

    public interface OnPenPicked {
        void onPenPicked(int color);
    }

    private static final int COLUMNS = 4;
    /** How far the chosen pencil rises out of the tray, in dp. */
    private static final float LIFT_DP = 7f;
    private static final float ASPECT = 2.9f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private int[] colors = Palette.SWATCHES;
    private int selected = Palette.RED;
    @Nullable
    private OnPenPicked listener;

    public PenBar(Context context) {
        this(context, null);
    }

    public PenBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        shadow.setColor(0x33000000);
    }

    public void setColors(int[] next) {
        if (next != null && next.length > 0) {
            colors = next;
            requestLayout();
            invalidate();
        }
    }

    public void setSelected(int color) {
        selected = color;
        invalidate();
    }

    public int selectedColor() {
        return selected;
    }

    public void setOnPenPicked(@Nullable OnPenPicked listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int rows = (int) Math.ceil(colors.length / (float) COLUMNS);
        float cellWidth = width / (float) COLUMNS;
        int height = Math.round(cellWidth * ASPECT * rows);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int rows = (int) Math.ceil(colors.length / (float) COLUMNS);
        float cellWidth = getWidth() / (float) COLUMNS;
        float cellHeight = getHeight() / (float) rows;
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < colors.length; i++) {
            int column = i % COLUMNS;
            int row = i / COLUMNS;
            float centerX = (column + 0.5f) * cellWidth;
            float top = row * cellHeight;
            boolean isSelected = colors[i] == selected;
            drawPencil(canvas, colors[i], centerX, top, cellWidth, cellHeight,
                    isSelected ? -LIFT_DP * density : 0f, isSelected, density);
        }
    }

    private void drawPencil(Canvas canvas, int color, float centerX, float top,
                            float cellWidth, float cellHeight, float lift,
                            boolean isSelected, float density) {
        float bodyWidth = cellWidth * 0.46f;
        float padding = cellHeight * 0.08f;
        float bodyTop = top + padding + lift;
        float bodyBottom = top + cellHeight - padding + lift;
        float tipHeight = (bodyBottom - bodyTop) * 0.22f;
        float left = centerX - bodyWidth / 2f;
        float right = centerX + bodyWidth / 2f;
        float shaftBottom = bodyBottom - tipHeight;

        if (isSelected) {
            rect.set(left, bodyTop + 3f * density, right, shaftBottom + 3f * density);
            fill.setColor(0x26000000);
            canvas.drawRoundRect(rect, bodyWidth * 0.3f, bodyWidth * 0.3f, fill);
        }

        // Barrel.
        rect.set(left, bodyTop, right, shaftBottom);
        fill.setColor(color);
        canvas.drawRoundRect(rect, bodyWidth * 0.3f, bodyWidth * 0.3f, fill);

        // A lighter facet down one side, which is what makes it read as a pencil.
        rect.set(left + bodyWidth * 0.12f, bodyTop + bodyWidth * 0.18f,
                left + bodyWidth * 0.34f, shaftBottom - bodyWidth * 0.18f);
        fill.setColor(lighten(color, 0.34f));
        canvas.drawRoundRect(rect, bodyWidth * 0.12f, bodyWidth * 0.12f, fill);

        // Metal ferrule at the top.
        rect.set(left, bodyTop, right, bodyTop + bodyWidth * 0.42f);
        fill.setColor(0xFFC9CDD2);
        canvas.drawRoundRect(rect, bodyWidth * 0.18f, bodyWidth * 0.18f, fill);

        // Sharpened wooden cone.
        path.reset();
        path.moveTo(left, shaftBottom);
        path.lineTo(right, shaftBottom);
        path.lineTo(centerX, bodyBottom);
        path.close();
        fill.setColor(0xFFE8C9A8);
        canvas.drawPath(path, fill);

        // Graphite point, in the pencil's own colour so the child links the two.
        path.reset();
        path.moveTo(centerX - bodyWidth * 0.17f, bodyBottom - tipHeight * 0.36f);
        path.lineTo(centerX + bodyWidth * 0.17f, bodyBottom - tipHeight * 0.36f);
        path.lineTo(centerX, bodyBottom);
        path.close();
        fill.setColor(darken(color, 0.3f));
        canvas.drawPath(path, fill);
    }

    private static int lighten(int color, float amount) {
        return Color.rgb(
                Math.round(Color.red(color) + (255 - Color.red(color)) * amount),
                Math.round(Color.green(color) + (255 - Color.green(color)) * amount),
                Math.round(Color.blue(color) + (255 - Color.blue(color)) * amount));
    }

    private static int darken(int color, float amount) {
        float keep = 1f - amount;
        return Color.rgb(
                Math.round(Color.red(color) * keep),
                Math.round(Color.green(color) * keep),
                Math.round(Color.blue(color) * keep));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        int rows = (int) Math.ceil(colors.length / (float) COLUMNS);
        int column = (int) (event.getX() / (getWidth() / (float) COLUMNS));
        int row = (int) (event.getY() / (getHeight() / (float) rows));
        int index = row * COLUMNS + column;
        if (column >= 0 && column < COLUMNS && index >= 0 && index < colors.length) {
            setSelected(colors[index]);
            performClick();
            if (listener != null) {
                listener.onPenPicked(colors[index]);
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
