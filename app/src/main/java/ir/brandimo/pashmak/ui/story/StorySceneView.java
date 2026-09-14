package ir.brandimo.pashmak.ui.story;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.StoryProp;
import ir.brandimo.pashmak.util.Motion;

/**
 * The picture the story happens in. Props drift gently so the scene feels alive,
 * and tapping one makes it jump — which is how the child answers Pashmak.
 */
public class StorySceneView extends View {

    public interface OnPropTapped {
        void onPropTapped(StoryProp prop);
    }

    private static final long FLOAT_PERIOD_MS = 3400L;

    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect bounds = new Rect();
    private final List<StoryProp> props = new ArrayList<>();
    private final List<Drawable> drawables = new ArrayList<>();

    @Nullable
    private OnPropTapped listener;
    private int pressedIndex = -1;
    private long pressedAt;
    private boolean interactive = true;

    public StorySceneView(Context context) {
        this(context, null);
    }

    public StorySceneView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        label.setTextAlign(Paint.Align.CENTER);
        label.setColor(0xFF7A4A02);
        Typeface lalezar = ResourcesCompat.getFont(context, R.font.lalezar_regular);
        if (lalezar != null) {
            label.setTypeface(lalezar);
        }
    }

    public void setProps(@Nullable List<StoryProp> next) {
        props.clear();
        drawables.clear();
        if (next != null) {
            props.addAll(next);
            for (int i = 0; i < props.size(); i++) {
                drawables.add(AppCompatResources.getDrawable(
                        getContext(), props.get(i).icon));
            }
        }
        pressedIndex = -1;
        invalidate();
    }

    /** Taps are ignored while Pashmak is only narrating. */
    public void setInteractive(boolean value) {
        interactive = value;
    }

    public void setOnPropTapped(@Nullable OnPropTapped listener) {
        this.listener = listener;
    }

    /** Makes one prop jump — used to confirm a correct answer. */
    public void bounce(String propId) {
        for (int i = 0; i < props.size(); i++) {
            if (props.get(i).id.equals(propId)) {
                pressedIndex = i;
                pressedAt = AnimationUtils.currentAnimationTimeMillis();
                invalidate();
                return;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (props.isEmpty()) {
            return;
        }
        float shortest = Math.min(getWidth(), getHeight());
        label.setTextSize(shortest * 0.058f);
        long now = AnimationUtils.currentAnimationTimeMillis();
        boolean still = Motion.reduced(getContext());

        for (int i = 0; i < props.size(); i++) {
            StoryProp prop = props.get(i);
            Drawable drawable = drawables.get(i);
            if (drawable == null) {
                continue;
            }
            float size = prop.size * shortest;
            float cx = prop.x * getWidth();
            float cy = prop.y * getHeight();

            // Each prop floats on its own offset phase, so they never move in lockstep.
            float drift = 0f;
            if (!still) {
                float phase = ((now + i * 480L) % FLOAT_PERIOD_MS) / (float) FLOAT_PERIOD_MS;
                drift = (float) Math.sin(phase * 2 * Math.PI) * size * 0.04f;
            }

            float scale = 1f;
            if (i == pressedIndex) {
                float elapsed = (now - pressedAt) / 420f;
                if (elapsed >= 1f) {
                    pressedIndex = -1;
                } else {
                    scale = 1f + (float) Math.sin(elapsed * Math.PI) * 0.22f;
                }
            }

            float half = size * scale / 2f;
            drawable.setBounds(Math.round(cx - half), Math.round(cy - half + drift),
                    Math.round(cx + half), Math.round(cy + half + drift));
            drawable.draw(canvas);
            canvas.drawText(prop.label, cx, cy + half + drift + label.getTextSize(), label);
        }

        if (!still || pressedIndex >= 0) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactive || props.isEmpty()
                || event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        int index = propAt(event.getX(), event.getY());
        if (index >= 0) {
            pressedIndex = index;
            pressedAt = AnimationUtils.currentAnimationTimeMillis();
            invalidate();
            performClick();
            if (listener != null) {
                listener.onPropTapped(props.get(index));
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int propAt(float x, float y) {
        float shortest = Math.min(getWidth(), getHeight());
        for (int i = props.size() - 1; i >= 0; i--) {
            StoryProp prop = props.get(i);
            // A little generous, because small fingers aim roughly.
            float half = prop.size * shortest * 0.62f;
            float cx = prop.x * getWidth();
            float cy = prop.y * getHeight();
            if (x >= cx - half && x <= cx + half && y >= cy - half && y <= cy + half) {
                return i;
            }
        }
        return -1;
    }
}
