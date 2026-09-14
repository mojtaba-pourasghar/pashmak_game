package ir.brandimo.pashmak.ui.livedrawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.brandimo.pashmak.util.Motion;

/**
 * The dashed frame the child lines their paper up with. It pulses gently so it
 * reads as "put it here" rather than as a static border, and it reports its own
 * rectangle so the extractor crops exactly what was framed.
 */
public class RoiOverlayView extends View {

    private static final float MARGIN_FRACTION = 0.08f;
    private static final long PULSE_MS = 2200L;

    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF frame = new RectF();
    private final Path scrimPath = new Path();
    private final Path framePath = new Path();

    private boolean pulsing = true;

    public RoiOverlayView(Context context) {
        this(context, null);
    }

    public RoiOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(dp(4f));
        framePaint.setColor(Color.WHITE);
        framePaint.setPathEffect(new DashPathEffect(new float[]{dp(14f), dp(10f)}, 0f));
        scrimPaint.setColor(0x73000000);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /** Freezes the pulse, e.g. once the frame is considered locked on. */
    public void setPulsing(boolean value) {
        pulsing = value;
        invalidate();
    }

    /** The scanning rectangle in this view's pixel coordinates. */
    @NonNull
    public RectF roiRect() {
        return new RectF(frame);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float marginX = w * MARGIN_FRACTION;
        float marginY = h * MARGIN_FRACTION;
        frame.set(marginX, marginY, w - marginX, h - marginY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (frame.isEmpty()) {
            return;
        }
        float radius = dp(22f);
        float inset = 0f;
        if (pulsing && !Motion.reduced(getContext())) {
            long now = AnimationUtils.currentAnimationTimeMillis();
            float phase = (now % PULSE_MS) / (float) PULSE_MS;
            float wave = (float) Math.sin(phase * 2 * Math.PI);
            inset = wave * dp(4f);
            framePaint.setAlpha(Math.round(200 + 55 * Math.abs(wave)));
        } else {
            framePaint.setAlpha(230);
        }

        RectF pulsed = new RectF(
                frame.left + inset, frame.top + inset,
                frame.right - inset, frame.bottom - inset);

        // Dim everything outside the frame so the paper is the only bright thing.
        scrimPath.reset();
        scrimPath.addRect(0f, 0f, getWidth(), getHeight(), Path.Direction.CW);
        framePath.reset();
        framePath.addRoundRect(pulsed, radius, radius, Path.Direction.CCW);
        scrimPath.addPath(framePath);
        scrimPath.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(scrimPath, scrimPaint);
        canvas.drawRoundRect(pulsed, radius, radius, framePaint);

        if (pulsing) {
            postInvalidateOnAnimation();
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
