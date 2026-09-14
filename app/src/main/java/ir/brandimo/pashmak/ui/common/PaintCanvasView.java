package ir.brandimo.pashmak.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import ir.brandimo.pashmak.R;

/**
 * Finger painting onto an offscreen bitmap. Used on its own for free drawing, and
 * with a ghost glyph behind it for letter and number tracing — in which case it
 * can also report how much of the glyph the child has actually covered.
 */
public class PaintCanvasView extends View {

    public interface OnStrokeListener {
        void onStrokeFinished();
    }

    /** The drawing tools, each with its own feel rather than just a width. */
    public enum Tool {
        /** Fine line for careful work. */
        PENCIL_THIN(6f, 255, Paint.Cap.ROUND),
        /** Everyday thick pencil. */
        PENCIL_THICK(16f, 255, Paint.Cap.ROUND),
        /** Broad and slightly see-through, so overlaps build up colour. */
        MARKER(34f, 150, Paint.Cap.SQUARE),
        /** Waxy: drawn as offset passes so the edge breaks up like real crayon. */
        CRAYON(22f, 90, Paint.Cap.ROUND),
        /** Clears back to the paper. */
        ERASER(44f, 255, Paint.Cap.ROUND);

        public final float widthDp;
        public final int alpha;
        public final Paint.Cap cap;

        Tool(float widthDp, int alpha, Paint.Cap cap) {
            this.widthDp = widthDp;
            this.alpha = alpha;
            this.cap = cap;
        }

        public boolean erases() {
            return this == ERASER;
        }
    }

    private static final float SMOOTHING = 0.5f;

    private final Paint brush = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);
    private final Path stroke = new Path();
    private final Path ghostPath = new Path();
    private final Rect textBounds = new Rect();

    @Nullable
    private Bitmap layer;
    @Nullable
    private Canvas layerCanvas;
    private Tool tool = Tool.PENCIL_THICK;
    private int brushColor = Color.parseColor("#F26522");

    @Nullable
    private String ghostGlyph;
    @Nullable
    private OnStrokeListener strokeListener;

    private float lastX;
    private float lastY;
    private boolean dirty;

    public PaintCanvasView(Context context) {
        this(context, null);
    }

    public PaintCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeJoin(Paint.Join.ROUND);
        brush.setStrokeCap(Paint.Cap.ROUND);
        applyTool();

        ghostPaint.setStyle(Paint.Style.FILL);
        ghostPaint.setColor(0xFFE8F4E2);
        Typeface lalezar = ResourcesCompat.getFont(context, R.font.lalezar_regular);
        if (lalezar != null) {
            ghostPaint.setTypeface(lalezar);
        }
        ghostPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setBrushColor(int color) {
        brushColor = color;
        applyTool();
    }

    public void setBrushWidthDp(float dp) {
        brush.setStrokeWidth(dp * getResources().getDisplayMetrics().density);
    }

    public void setTool(Tool next) {
        tool = next == null ? Tool.PENCIL_THICK : next;
        applyTool();
    }

    public Tool tool() {
        return tool;
    }

    private void applyTool() {
        float density = getResources().getDisplayMetrics().density;
        brush.setStrokeWidth(tool.widthDp * density);
        brush.setStrokeCap(tool.cap);
        brush.setStrokeJoin(tool.cap == Paint.Cap.SQUARE ? Paint.Join.MITER : Paint.Join.ROUND);
        if (tool.erases()) {
            brush.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            brush.setColor(Color.TRANSPARENT);
            brush.setAlpha(255);
        } else {
            brush.setXfermode(null);
            brush.setColor(brushColor);
            brush.setAlpha(tool.alpha);
        }
    }

    public void setOnStrokeListener(@Nullable OnStrokeListener listener) {
        strokeListener = listener;
    }

    /** Shows a big pale letter behind the canvas for the child to trace over. */
    public void setGhostGlyph(@Nullable String glyph) {
        ghostGlyph = glyph;
        clear();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clear() {
        if (layer != null && layerCanvas != null) {
            layerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        stroke.reset();
        dirty = false;
        invalidate();
    }

    @Nullable
    public Bitmap snapshot(int backgroundColor) {
        if (layer == null) {
            return null;
        }
        Bitmap out = Bitmap.createBitmap(layer.getWidth(), layer.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(backgroundColor);
        if (ghostGlyph != null) {
            drawGhost(canvas);
        }
        canvas.drawBitmap(layer, 0f, 0f, null);
        return out;
    }

    /**
     * How much of the ghost glyph the strokes cover, 0..1. Sampled on a small grid
     * because the child only needs a fair judgement, not a precise one.
     */
    public float glyphCoverage() {
        if (layer == null || ghostGlyph == null) {
            return 0f;
        }
        buildGhostPath();
        int sample = 160;
        Bitmap mask = Bitmap.createBitmap(sample, sample, Bitmap.Config.ARGB_8888);
        Canvas maskCanvas = new Canvas(mask);
        float scaleX = sample / (float) getWidth();
        float scaleY = sample / (float) getHeight();
        maskCanvas.scale(scaleX, scaleY);
        Paint solid = new Paint(Paint.ANTI_ALIAS_FLAG);
        solid.setColor(Color.BLACK);
        maskCanvas.drawPath(ghostPath, solid);

        Bitmap strokes = Bitmap.createScaledBitmap(layer, sample, sample, true);

        int[] maskPixels = new int[sample * sample];
        int[] strokePixels = new int[sample * sample];
        mask.getPixels(maskPixels, 0, sample, 0, 0, sample, sample);
        strokes.getPixels(strokePixels, 0, sample, 0, 0, sample, sample);
        mask.recycle();
        if (strokes != layer) {
            strokes.recycle();
        }

        int total = 0;
        int covered = 0;
        for (int i = 0; i < maskPixels.length; i++) {
            if ((maskPixels[i] >>> 24) > 40) {
                total++;
                if ((strokePixels[i] >>> 24) > 40) {
                    covered++;
                }
            }
        }
        return total == 0 ? 0f : covered / (float) total;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        Bitmap next = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(next);
        if (layer != null) {
            canvas.drawBitmap(layer, 0f, 0f, null);
            layer.recycle();
        }
        layer = next;
        layerCanvas = canvas;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (ghostGlyph != null) {
            drawGhost(canvas);
        }
        if (layer != null) {
            canvas.drawBitmap(layer, 0f, 0f, bitmapPaint);
        }
        if (!tool.erases()) {
            canvas.drawPath(stroke, brush);
        }
    }

    /** Crayon lays three slightly offset passes, which reads as waxy texture. */
    private void commitStroke() {
        if (layerCanvas == null) {
            return;
        }
        if (tool == Tool.CRAYON) {
            float density = getResources().getDisplayMetrics().density;
            float[][] offsets = {{0f, 0f}, {1.6f * density, -1.1f * density},
                    {-1.3f * density, 1.4f * density}};
            for (float[] offset : offsets) {
                layerCanvas.save();
                layerCanvas.translate(offset[0], offset[1]);
                layerCanvas.drawPath(stroke, brush);
                layerCanvas.restore();
            }
        } else {
            layerCanvas.drawPath(stroke, brush);
        }
    }

    private void drawGhost(Canvas canvas) {
        buildGhostPath();
        canvas.drawPath(ghostPath, ghostPaint);
    }

    private void buildGhostPath() {
        if (ghostGlyph == null || getWidth() == 0) {
            return;
        }
        float size = Math.min(getWidth(), getHeight()) * 0.78f;
        ghostPaint.setTextSize(size);
        ghostPaint.getTextBounds(ghostGlyph, 0, ghostGlyph.length(), textBounds);
        float x = getWidth() / 2f;
        float y = getHeight() / 2f + textBounds.height() / 2f;
        ghostPath.reset();
        ghostPaint.getTextPath(ghostGlyph, 0, ghostGlyph.length(), x, y, ghostPath);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                stroke.reset();
                stroke.moveTo(x, y);
                // A tap with no movement should still leave a dot.
                stroke.lineTo(x + 0.1f, y + 0.1f);
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (tool.erases()) {
                    // Erase as the finger moves, so the child sees it happening.
                    stroke.lineTo(x, y);
                    commitStroke();
                    stroke.reset();
                    stroke.moveTo(x, y);
                    lastX = x;
                    lastY = y;
                    dirty = true;
                    invalidate();
                    return true;
                }
                // Quadratic smoothing keeps fast finger drags from looking angular.
                float midX = lastX + (x - lastX) * SMOOTHING;
                float midY = lastY + (y - lastY) * SMOOTHING;
                stroke.quadTo(lastX, lastY, midX, midY);
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stroke.lineTo(x, y);
                commitStroke();
                stroke.reset();
                dirty = true;
                performClick();
                if (strokeListener != null) {
                    strokeListener.onStrokeFinished();
                }
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
