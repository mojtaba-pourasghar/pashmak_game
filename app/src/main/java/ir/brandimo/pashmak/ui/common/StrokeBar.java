package ir.brandimo.pashmak.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import ir.brandimo.pashmak.ui.common.PaintCanvasView.Tool;

/**
 * The thickness picker. Each cell paints a short stroke at the width it will
 * actually draw, in the colour and texture of the tool currently held — so the
 * marker sample is broad and translucent and the crayon sample is waxy. The child
 * chooses by seeing the line, not by reading a label.
 */
public class StrokeBar extends View {

    public interface OnWidthPicked {
        void onWidthPicked(float widthDp);
    }

    private final Paint sample = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cell = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path stroke = new Path();
    private final RectF rect = new RectF();

    private float[] steps = PaintCanvasView.WIDTH_STEPS;
    private float selected = PaintCanvasView.WIDTH_STEPS[1];
    private Tool tool = Tool.PENCIL;
    private int color = 0xFFF26522;
    @Nullable
    private OnWidthPicked listener;
    @Nullable
    private PaintCanvasView canvas;

    public StrokeBar(Context context) {
        this(context, null);
    }

    public StrokeBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        cell.setStyle(Paint.Style.FILL);
    }

    /** The canvas whose stroke settings these samples should mirror. */
    public void mirror(@Nullable PaintCanvasView target) {
        canvas = target;
        refresh();
    }

    public void setOnWidthPicked(@Nullable OnWidthPicked listener) {
        this.listener = listener;
    }

    public void setSelected(float widthDp) {
        selected = widthDp;
        invalidate();
    }

    /** Called whenever the pen or tool changes, so the preview stays truthful. */
    public void refresh() {
        if (canvas != null) {
            tool = canvas.tool();
            color = canvas.brushColor();
            selected = canvas.strokeWidthDp();
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width / (float) steps.length * 1.15f);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas target) {
        float cellWidth = getWidth() / (float) steps.length;
        float density = getResources().getDisplayMetrics().density;
        float inset = 3f * density;

        for (int i = 0; i < steps.length; i++) {
            float left = i * cellWidth;
            rect.set(left + inset, inset, left + cellWidth - inset, getHeight() - inset);
            boolean isSelected = Math.abs(steps[i] - selected) < 0.01f;

            cell.setColor(0xFFFFFFFF);
            target.drawRoundRect(rect, 12f * density, 12f * density, cell);
            cell.setColor(isSelected ? 0xFF2B3742 : 0xFFDCEEF7);
            cell.setStyle(Paint.Style.STROKE);
            cell.setStrokeWidth((isSelected ? 3f : 2f) * density);
            target.drawRoundRect(rect, 12f * density, 12f * density, cell);
            cell.setStyle(Paint.Style.FILL);

            drawSample(target, rect, steps[i], density);
        }
    }

    /** A short wave, so the sample shows the cap and the body of the stroke. */
    private void drawSample(Canvas target, RectF box, float widthDp, float density) {
        if (canvas != null) {
            canvas.describeStroke(sample, tool, widthDp, color);
        } else {
            sample.setStyle(Paint.Style.STROKE);
            sample.setStrokeWidth(widthDp * density);
            sample.setColor(color);
        }
        // Keep the fattest sample inside its cell.
        float maxWidth = box.height() * 0.62f;
        if (sample.getStrokeWidth() > maxWidth) {
            sample.setStrokeWidth(maxWidth);
        }

        float midY = box.centerY();
        float amplitude = Math.min(box.height() * 0.16f, 6f * density);
        stroke.reset();
        stroke.moveTo(box.left + box.width() * 0.22f, midY + amplitude);
        stroke.quadTo(box.centerX(), midY - amplitude * 2.2f,
                box.right - box.width() * 0.22f, midY + amplitude);

        if (tool == Tool.CRAYON) {
            // Same offset passes the canvas uses, so crayon looks waxy here too.
            float[][] offsets = {{0f, 0f}, {1.4f * density, -1f * density},
                    {-1.1f * density, 1.2f * density}};
            for (float[] offset : offsets) {
                target.save();
                target.translate(offset[0], offset[1]);
                target.drawPath(stroke, sample);
                target.restore();
            }
        } else {
            target.drawPath(stroke, sample);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        int index = (int) (event.getX() / (getWidth() / (float) steps.length));
        if (index >= 0 && index < steps.length) {
            setSelected(steps[index]);
            performClick();
            if (listener != null) {
                listener.onWidthPicked(steps[index]);
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
