package ir.brandimo.pashmak.ui.tale;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import ir.brandimo.pashmak.data.catalog.MissionScene;
import ir.brandimo.pashmak.ui.common.ScenePainter;
import ir.brandimo.pashmak.util.Motion;

/**
 * The picture behind a told tale. When the story moves somewhere else the new place
 * fades up over the old one rather than cutting, because a cut in the middle of a
 * sentence reads as a glitch and a dissolve reads as time passing.
 */
public class TaleSceneView extends View {

    private static final long FADE_MS = 700L;

    private final ScenePainter painter;
    private final RectF bounds = new RectF();

    @Nullable
    private MissionScene current;
    @Nullable
    private MissionScene previous;
    @Nullable
    private ValueAnimator fade;
    private float progress = 1f;

    public TaleSceneView(Context context) {
        this(context, null);
    }

    public TaleSceneView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        painter = new ScenePainter(context);
    }

    /** Moves to a new place, dissolving from the old one. */
    public void show(@Nullable MissionScene scene) {
        if (scene == current) {
            return;
        }
        cancelFade();
        previous = current;
        current = scene;
        if (previous == null || Motion.reduced(getContext())) {
            progress = 1f;
            invalidate();
            return;
        }
        progress = 0f;
        fade = ValueAnimator.ofFloat(0f, 1f);
        fade.setDuration(FADE_MS);
        fade.addUpdateListener(value -> {
            progress = (float) value.getAnimatedValue();
            invalidate();
        });
        fade.start();
    }

    /** Drops straight to a place with no dissolve — used when a tale restarts. */
    public void reset(@Nullable MissionScene scene) {
        cancelFade();
        previous = null;
        current = scene;
        progress = 1f;
        invalidate();
    }

    private void cancelFade() {
        if (fade != null) {
            fade.cancel();
            fade = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelFade();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        if (previous != null && progress < 1f) {
            painter.paint(canvas, previous, w, h);
            bounds.set(0f, 0f, w, h);
            int layer = canvas.saveLayerAlpha(bounds, Math.round(progress * 255));
            painter.paint(canvas, current, w, h);
            canvas.restoreToCount(layer);
        } else {
            previous = null;
            painter.paint(canvas, current, w, h);
        }
    }
}
