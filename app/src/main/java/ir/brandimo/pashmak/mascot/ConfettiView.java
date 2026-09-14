package ir.brandimo.pashmak.mascot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;

import java.util.Random;

import ir.brandimo.pashmak.data.catalog.Palette;
import ir.brandimo.pashmak.util.Motion;

/**
 * A short burst of falling paper for mission completions. Draws nothing and costs
 * nothing until {@link #burst()} is called, and stops itself when the pieces land.
 */
public class ConfettiView extends View {

    private static final int PIECES = 90;
    private static final long LIFETIME_MS = 2600L;

    private static final class Piece {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float spin;
        float angle;
        int color;
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random();
    private final Piece[] pieces = new Piece[PIECES];

    private long startMs;
    private boolean running;

    public ConfettiView(Context context) {
        this(context, null);
    }

    public ConfettiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        for (int i = 0; i < PIECES; i++) {
            pieces[i] = new Piece();
        }
        setWillNotDraw(false);
    }

    public void burst() {
        if (getWidth() == 0 || Motion.reduced(getContext())) {
            return;
        }
        for (int i = 0; i < PIECES; i++) {
            Piece p = pieces[i];
            p.x = random.nextFloat() * getWidth();
            p.y = -random.nextFloat() * getHeight() * 0.4f;
            p.vx = (random.nextFloat() - 0.5f) * 90f;
            p.vy = 220f + random.nextFloat() * 320f;
            p.size = 8f + random.nextFloat() * 12f;
            p.spin = (random.nextFloat() - 0.5f) * 520f;
            p.angle = random.nextFloat() * 360f;
            p.color = Palette.SWATCHES[random.nextInt(Palette.SWATCHES.length)];
        }
        startMs = AnimationUtils.currentAnimationTimeMillis();
        running = true;
        setVisibility(VISIBLE);
        postInvalidateOnAnimation();
    }

    public void stop() {
        running = false;
        setVisibility(GONE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!running) {
            return;
        }
        long now = AnimationUtils.currentAnimationTimeMillis();
        float elapsed = (now - startMs) / 1000f;
        if (now - startMs > LIFETIME_MS) {
            stop();
            return;
        }
        float fade = 1f - (now - startMs) / (float) LIFETIME_MS;
        for (int i = 0; i < PIECES; i++) {
            Piece p = pieces[i];
            float x = p.x + p.vx * elapsed;
            float y = p.y + p.vy * elapsed + 90f * elapsed * elapsed;
            if (y > getHeight() + p.size) {
                continue;
            }
            paint.setColor(p.color);
            paint.setAlpha(Math.round(Math.max(0f, Math.min(1f, fade)) * 255f));
            int save = canvas.save();
            canvas.rotate(p.angle + p.spin * elapsed, x, y);
            rect.set(x - p.size / 2f, y - p.size / 3f, x + p.size / 2f, y + p.size / 3f);
            canvas.drawRoundRect(rect, 2f, 2f, paint);
            canvas.restoreToCount(save);
        }
        postInvalidateOnAnimation();
    }
}
