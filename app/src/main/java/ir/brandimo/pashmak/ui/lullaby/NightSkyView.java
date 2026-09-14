package ir.brandimo.pashmak.ui.lullaby;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.Random;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.util.Motion;

/**
 * The backdrop for the bedtime screen: a field of stars that breathe slowly, and a
 * moon low in the corner. The star positions are drawn from a fixed seed, so the
 * sky looks the same every night — a familiar room, not a new one each time.
 */
public class NightSkyView extends View {

    private static final int STAR_COUNT = 70;
    private static final long CYCLE_MS = 5200L;

    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moonCutPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** x, y as fractions of the view, radius in dp, and a phase offset each. */
    private final float[] starX = new float[STAR_COUNT];
    private final float[] starY = new float[STAR_COUNT];
    private final float[] starR = new float[STAR_COUNT];
    private final float[] starPhase = new float[STAR_COUNT];

    private final float density;
    private final boolean animate;

    public NightSkyView(Context context) {
        this(context, null);
    }

    public NightSkyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        animate = !Motion.reduced(context);

        starPaint.setColor(ContextCompat.getColor(context, R.color.white));
        moonPaint.setColor(ContextCompat.getColor(context, R.color.night_gold));
        // Punched out of this view's own layer, so the sky gradient shows through
        // the bite instead of a flat approximation of it.
        moonCutPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        Random random = new Random(20240914L);
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = random.nextFloat();
            // Keep the sky above the content: stars thin out toward the bottom.
            starY[i] = random.nextFloat() * random.nextFloat();
            starR[i] = 0.7f + random.nextFloat() * 1.6f;
            starPhase[i] = random.nextFloat();
        }
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        float beat = animate
                ? (System.currentTimeMillis() % CYCLE_MS) / (float) CYCLE_MS
                : 0.25f;
        for (int i = 0; i < STAR_COUNT; i++) {
            float phase = (beat + starPhase[i]) % 1f;
            // A soft triangle wave: bright at the middle of the phase, dim at the ends.
            float glow = phase < 0.5f ? phase * 2f : (1f - phase) * 2f;
            starPaint.setAlpha((int) (70 + glow * 150));
            canvas.drawCircle(starX[i] * width, starY[i] * height,
                    starR[i] * density, starPaint);
        }

        // The moon: a disc with a second disc punched out of it, so it reads as a crescent.
        float radius = Math.min(width, height) * 0.13f;
        float cx = width * 0.12f;
        float cy = height * 0.22f;
        canvas.drawCircle(cx, cy, radius, moonPaint);
        canvas.drawCircle(cx - radius * 0.42f, cy - radius * 0.22f, radius * 0.92f, moonCutPaint);

        if (animate) {
            postInvalidateOnAnimation();
        }
    }
}
