package ir.brandimo.pashmak.ui.tale;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ir.brandimo.pashmak.data.catalog.MissionScene;
import ir.brandimo.pashmak.data.catalog.Tale;
import ir.brandimo.pashmak.ui.common.ScenePainter;
import ir.brandimo.pashmak.util.Motion;

/**
 * The picture behind a told tale. When the story moves somewhere else the new place
 * fades up over the old one rather than cutting, because a cut in the middle of a
 * sentence reads as a glitch and a dissolve reads as time passing.
 *
 * <p>On top of the place goes the subject of the passage — the snail, the boat, the
 * sheep — moving in the way that thing moves. A place on its own was the wrong
 * picture: a tale about a brave snail showed an empty meadow, and the child looking
 * for the snail did not find one.
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

    @Nullable
    private Drawable actor;
    private Tale.Motion motion = Tale.Motion.STILL;
    /** Where the place's ground begins, so a walker walks on it. */
    private float horizon = 0.62f;
    private long actorSince;
    private boolean stillness;

    public TaleSceneView(Context context) {
        this(context, null);
    }

    public TaleSceneView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        painter = new ScenePainter(context);
    }

    /**
     * Puts the subject of this passage into the scene.
     *
     * <p>The clock only restarts when the subject itself changes, so a snail crossing
     * a meadow keeps crossing it while the telling moves from sentence to sentence,
     * instead of jumping back to the edge every time a new line begins.
     */
    public void setActor(@DrawableRes int drawable, @Nullable Tale.Motion how) {
        Tale.Motion next = how == null ? Tale.Motion.STILL : how;
        Drawable art = drawable == 0
                ? null : ContextCompat.getDrawable(getContext(), drawable);
        boolean same = (art == null) == (actor == null)
                && (art == null || actor.getConstantState() == null
                    || art.getConstantState() == null
                    || actor.getConstantState().equals(art.getConstantState()));
        if (!same || next != motion) {
            actor = art;
            motion = next;
            actorSince = SystemClock.uptimeMillis();
        }
        stillness = Motion.reduced(getContext());
        invalidate();
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
        if (current != null) {
            horizon = current.horizon;
        }
        drawActor(canvas, w, h);
    }

    /**
     * Draws the subject over the place, moving. Everything here is worked out from
     * elapsed time rather than kept as state, so a frame dropped or a screen turned
     * changes nothing: the snail is wherever the clock says it should be.
     */
    private void drawActor(Canvas canvas, int w, int h) {
        if (actor == null) {
            return;
        }
        float seconds = stillness ? 0f : (SystemClock.uptimeMillis() - actorSince) / 1000f;
        // Big enough to be what the picture is about. At a fifth of the frame it read
        // as a sticker stuck on a landscape rather than the subject of the story.
        float size = Math.min(w, h) * 0.42f;
        float groundY = h * horizon;
        // Standing on its feet just above the bottom edge, measured from the bottom
        // rather than from the horizon: some places have a thin strip of ground and
        // some a deep one, and the subject should sit the same way in both.
        float standing = h - size * 0.60f;
        float x;
        float y;
        float turn = 0f;

        switch (motion) {
            case WALK: {
                // Twenty seconds to cross and twenty back, so it is always going
                // somewhere — and it turns round inside the frame, never off it.
                float cycle = (seconds % 40f) / 20f;
                float along = cycle <= 1f ? cycle : 2f - cycle;
                x = w * 0.18f + (w * 0.64f) * along;
                y = standing + (float) Math.sin(seconds * 3.1f) * size * 0.03f;
                break;
            }
            case DRIFT: {
                // Crosses the sky from one side to the other and back. It starts
                // inside the picture: beginning off the edge meant the first passage
                // of a tale showed an empty sky for four seconds.
                float cycle = (seconds % 48f) / 24f;
                float along = cycle <= 1f ? cycle : 2f - cycle;
                x = w * 0.80f - (w * 0.60f) * along;
                y = Math.max(size * 0.55f,
                        groundY * 0.42f + (float) Math.sin(seconds * 0.8f) * size * 0.12f);
                break;
            }
            case RISE: {
                // A slow float up and down. It used to climb out of the top and
                // reappear at the bottom, which read as a glitch, not as rising.
                x = w * 0.5f + w * 0.05f * (float) Math.sin(seconds * 0.5f);
                float lift = (1f - (float) Math.cos(seconds * 0.45f)) / 2f;
                y = standing - (standing - groundY * 0.5f) * lift;
                break;
            }
            case SPIN: {
                x = w * 0.5f;
                y = standing;
                turn = (seconds * 42f) % 360f;
                break;
            }
            case BOB:
            case STILL:
            default: {
                x = w * 0.5f;
                y = standing;
                if (motion == Tale.Motion.BOB) {
                    y -= (float) Math.abs(Math.sin(seconds * 1.9f)) * size * 0.16f;
                }
                break;
            }
        }

        int save = canvas.save();
        if (turn != 0f) {
            canvas.rotate(turn, x, y);
        }
        int half = Math.round(size / 2f);
        actor.setBounds(Math.round(x) - half, Math.round(y) - half,
                Math.round(x) + half, Math.round(y) + half);
        actor.draw(canvas);
        canvas.restoreToCount(save);

        if (!stillness && motion != Tale.Motion.STILL) {
            postInvalidateOnAnimation();
        }
    }
}
