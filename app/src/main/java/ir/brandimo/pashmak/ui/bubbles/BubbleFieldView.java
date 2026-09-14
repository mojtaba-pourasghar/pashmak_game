package ir.brandimo.pashmak.ui.bubbles;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ir.brandimo.pashmak.R;

/**
 * A field of rising bubbles with a letter or number inside each. Positions live
 * in the view and advance on the frame clock — pushing them through LiveData
 * would fight the 60fps loop for no benefit.
 */
public class BubbleFieldView extends View {

    public interface OnBubblePopped {
        void onBubblePopped(String label);
    }

    private static final int MAX_BUBBLES = 14;
    private static final long SPAWN_INTERVAL_MS = 620L;

    private static final class Bubble {
        float x;
        float y;
        float radius;
        float speed;
        float drift;
        float phase;
        int color;
        String label;
        boolean popping;
        float popProgress;
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Bubble> bubbles = new ArrayList<>();
    private final Random random = new Random();

    private String[] labels = new String[]{"ا", "ب", "پ", "۱", "۲", "۳"};
    private long lastFrameMs;
    private long lastSpawnMs;
    private boolean running;
    private float speedScale = 1f;
    @Nullable
    private OnBubblePopped listener;

    public BubbleFieldView(Context context) {
        this(context, null);
    }

    public BubbleFieldView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        rim.setStyle(Paint.Style.STROKE);
        rim.setColor(0xBFFFFFFF);
        rim.setStrokeWidth(3f * getResources().getDisplayMetrics().density);
        gloss.setColor(0x80FFFFFF);
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        Typeface lalezar = ResourcesCompat.getFont(context, R.font.lalezar_regular);
        if (lalezar != null) {
            text.setTypeface(lalezar);
        }
    }

    public void setLabels(String[] next) {
        if (next != null && next.length > 0) {
            labels = next;
        }
    }

    /** Difficulty makes the bubbles drift up faster. */
    public void setSpeedScale(float scale) {
        speedScale = scale;
    }

    public void setOnBubblePopped(@Nullable OnBubblePopped listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        lastFrameMs = AnimationUtils.currentAnimationTimeMillis();
        postInvalidateOnAnimation();
    }

    public void stop() {
        running = false;
    }

    public void reset() {
        bubbles.clear();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void spawn() {
        if (getWidth() == 0 || bubbles.size() >= MAX_BUBBLES) {
            return;
        }
        Bubble bubble = new Bubble();
        float density = getResources().getDisplayMetrics().density;
        bubble.radius = (26f + random.nextFloat() * 24f) * density;
        bubble.x = bubble.radius + random.nextFloat() * (getWidth() - 2 * bubble.radius);
        bubble.y = getHeight() + bubble.radius;
        bubble.speed = (40f + random.nextFloat() * 55f) * density * speedScale;
        bubble.drift = (random.nextFloat() - 0.5f) * 26f * density;
        bubble.phase = random.nextFloat() * 6.28f;
        bubble.color = BUBBLE_COLORS[random.nextInt(BUBBLE_COLORS.length)];
        bubble.label = labels[random.nextInt(labels.length)];
        bubbles.add(bubble);
    }

    private static final int[] BUBBLE_COLORS = {
            0x66E1251B, 0x66F7941D, 0x66FFC730, 0x6643B02A, 0x661FA6D6, 0x667A4FA3
    };

    @Override
    protected void onDraw(Canvas canvas) {
        long now = AnimationUtils.currentAnimationTimeMillis();
        float delta = Math.min(0.05f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;

        if (running && now - lastSpawnMs > SPAWN_INTERVAL_MS) {
            lastSpawnMs = now;
            spawn();
        }

        for (int i = bubbles.size() - 1; i >= 0; i--) {
            Bubble bubble = bubbles.get(i);
            if (bubble.popping) {
                bubble.popProgress += delta * 4f;
                if (bubble.popProgress >= 1f) {
                    bubbles.remove(i);
                    continue;
                }
            } else if (running) {
                bubble.y -= bubble.speed * delta;
                bubble.phase += delta * 2f;
                bubble.x += (float) Math.sin(bubble.phase) * bubble.drift * delta;
                if (bubble.y + bubble.radius < 0) {
                    bubbles.remove(i);
                    continue;
                }
            }
            draw(canvas, bubble);
        }

        if (running || !bubbles.isEmpty()) {
            postInvalidateOnAnimation();
        }
    }

    private void draw(Canvas canvas, Bubble bubble) {
        float scale = bubble.popping ? 1f + bubble.popProgress * 0.5f : 1f;
        int alpha = bubble.popping ? Math.round((1f - bubble.popProgress) * 255f) : 255;
        float radius = bubble.radius * scale;

        fill.setColor(bubble.color);
        fill.setAlpha(Math.min(alpha, Color.alpha(bubble.color)));
        canvas.drawCircle(bubble.x, bubble.y, radius, fill);

        rim.setAlpha(alpha);
        canvas.drawCircle(bubble.x, bubble.y, radius, rim);

        gloss.setAlpha(Math.round(alpha * 0.5f));
        canvas.drawCircle(bubble.x - radius * 0.32f, bubble.y - radius * 0.34f,
                radius * 0.18f, gloss);

        text.setAlpha(alpha);
        text.setTextSize(radius * 0.9f);
        canvas.drawText(bubble.label, bubble.x, bubble.y + radius * 0.32f, text);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        for (int i = bubbles.size() - 1; i >= 0; i--) {
            Bubble bubble = bubbles.get(i);
            if (bubble.popping) {
                continue;
            }
            float dx = x - bubble.x;
            float dy = y - bubble.y;
            // A little forgiveness on the hit radius; small fingers miss.
            float reach = bubble.radius * 1.15f;
            if (dx * dx + dy * dy <= reach * reach) {
                bubble.popping = true;
                performClick();
                if (listener != null) {
                    listener.onBubblePopped(bubble.label);
                }
                invalidate();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
