package ir.brandimo.pashmak.ui.livedrawing;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.MissionScene;
import ir.brandimo.pashmak.util.Motion;

/**
 * The mission's world, with the child's scanned drawings living in it.
 *
 * A newly scanned drawing flies in along an arc from where the camera was, lands
 * on its anchor with a bounce, and then breathes gently with the rest of the
 * scene — so the scan visibly becomes part of the picture.
 */
public class MissionSceneView extends View {

    private static final long ARRIVAL_MS = 950L;
    private static final long BOB_PERIOD_MS = 3200L;

    private static final class Item {
        final Bitmap bitmap;
        final int slot;
        float arrival = 1f;

        Item(Bitmap bitmap, int slot) {
            this.bitmap = bitmap;
            this.slot = slot;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path shapePath = new android.graphics.Path();
    private final RectF rect = new RectF();
    private final Rect source = new Rect();
    private final List<Item> items = new ArrayList<>();
    private final OvershootInterpolator overshoot = new OvershootInterpolator(1.6f);

    @Nullable
    private MissionScene scene;
    @Nullable
    private ValueAnimator arrivalAnimator;
    private String[] slotLabels = new String[0];

    public MissionSceneView(Context context) {
        this(context, null);
    }

    public MissionSceneView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        slotStroke.setStyle(Paint.Style.STROKE);
        slotStroke.setColor(0x8C2B3742);
        slotStroke.setStrokeWidth(2.5f * density);
        slotStroke.setPathEffect(new DashPathEffect(
                new float[]{9f * density, 7f * density}, 0f));
        slotLabel.setTextAlign(Paint.Align.CENTER);
        slotLabel.setColor(0xCC2B3742);
        Typeface lalezar = ResourcesCompat.getFont(context, R.font.lalezar_regular);
        if (lalezar != null) {
            slotLabel.setTypeface(lalezar);
        }
    }

    /**
     * The names of the items this mission expects. Slots still to be drawn show as
     * a dashed outline with the name, so the child can see what the room is missing.
     */
    public void setSlotLabels(@Nullable String[] labels) {
        slotLabels = labels == null ? new String[0] : labels;
        invalidate();
    }

    public void setScene(@Nullable MissionScene next) {
        scene = next;
        invalidate();
    }

    /** Replaces everything on stage — used when the mission's items are reloaded. */
    public void setItems(List<Bitmap> bitmaps, List<Integer> slots) {
        items.clear();
        if (bitmaps != null && slots != null) {
            int count = Math.min(bitmaps.size(), slots.size());
            for (int i = 0; i < count; i++) {
                if (bitmaps.get(i) != null) {
                    items.add(new Item(bitmaps.get(i), slots.get(i)));
                }
            }
        }
        invalidate();
    }

    /** Flies the most recently added drawing into place. */
    public void playArrival(int slot) {
        Item target = null;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).slot == slot) {
                target = items.get(i);
            }
        }
        if (target == null) {
            return;
        }
        if (Motion.reduced(getContext())) {
            target.arrival = 1f;
            invalidate();
            return;
        }
        final Item item = target;
        item.arrival = 0f;
        cancelArrival();
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ARRIVAL_MS);
        animator.addUpdateListener(value -> {
            item.arrival = (Float) value.getAnimatedValue();
            invalidate();
        });
        arrivalAnimator = animator;
        animator.start();
    }

    private void cancelArrival() {
        if (arrivalAnimator != null) {
            arrivalAnimator.cancel();
            arrivalAnimator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelArrival();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (scene == null || getWidth() == 0) {
            return;
        }
        drawBackdrop(canvas);
        drawShapes(canvas);
        drawDecor(canvas);
        drawEmptySlots(canvas);
        drawItems(canvas);

        if (!Motion.reduced(getContext())) {
            postInvalidateOnAnimation();
        }
    }

    private void drawBackdrop(Canvas canvas) {
        float horizon = getHeight() * scene.horizon;
        band.setColor(scene.skyColor);
        canvas.drawRect(0f, 0f, getWidth(), horizon, band);
        band.setColor(scene.groundColor);
        canvas.drawRect(0f, horizon, getWidth(), getHeight(), band);
    }

    /** The room itself: walls, counters, rugs, rails. */
    private void drawShapes(Canvas canvas) {
        for (int i = 0; i < scene.shapes.size(); i++) {
            MissionScene.Shape shape = scene.shapes.get(i);
            rect.set(shape.x * getWidth(), shape.y * getHeight(),
                    (shape.x + shape.width) * getWidth(),
                    (shape.y + shape.height) * getHeight());
            band.setColor(shape.color);
            switch (shape.kind) {
                case OVAL:
                    canvas.drawOval(rect, band);
                    break;
                case ROUND: {
                    float radius = Math.min(rect.width(), rect.height()) * shape.radius;
                    canvas.drawRoundRect(rect, radius, radius, band);
                    break;
                }
                case TRIANGLE_UP:
                    shapePath.reset();
                    shapePath.moveTo(rect.centerX(), rect.top);
                    shapePath.lineTo(rect.right, rect.bottom);
                    shapePath.lineTo(rect.left, rect.bottom);
                    shapePath.close();
                    canvas.drawPath(shapePath, band);
                    break;
                case TRIANGLE_DOWN:
                    shapePath.reset();
                    shapePath.moveTo(rect.left, rect.top);
                    shapePath.lineTo(rect.right, rect.top);
                    shapePath.lineTo(rect.centerX(), rect.bottom);
                    shapePath.close();
                    canvas.drawPath(shapePath, band);
                    break;
                case RECT:
                default:
                    canvas.drawRect(rect, band);
                    break;
            }
        }
    }

    /** Dashed outlines marking where the missing drawings will go. */
    private void drawEmptySlots(Canvas canvas) {
        if (slotLabels.length == 0) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float shortest = Math.min(getWidth(), getHeight());
        slotLabel.setTextSize(shortest * 0.045f);

        for (int slot = 0; slot < slotLabels.length; slot++) {
            if (hasItemInSlot(slot)) {
                continue;
            }
            MissionScene.Anchor anchor = scene.anchor(slot);
            float size = anchor.scale * shortest;
            float cx = anchor.x * getWidth();
            float cy = anchor.y * getHeight();
            rect.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);
            canvas.drawRoundRect(rect, 14f * density, 14f * density, slotStroke);
            canvas.drawText(slotLabels[slot], cx, cy + slotLabel.getTextSize() / 3f, slotLabel);
        }
    }

    private boolean hasItemInSlot(int slot) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).slot == slot) {
                return true;
            }
        }
        return false;
    }

    private void drawDecor(Canvas canvas) {
        float shortest = Math.min(getWidth(), getHeight());
        for (int i = 0; i < scene.decor.size(); i++) {
            MissionScene.Decor decor = scene.decor.get(i);
            Drawable drawable = AppCompatResources.getDrawable(getContext(), decor.icon);
            if (drawable == null) {
                continue;
            }
            float half = decor.size * shortest / 2f;
            float cx = decor.x * getWidth();
            float cy = decor.y * getHeight();
            drawable.setBounds(Math.round(cx - half), Math.round(cy - half),
                    Math.round(cx + half), Math.round(cy + half));
            drawable.draw(canvas);
        }
    }

    private void drawItems(Canvas canvas) {
        long now = AnimationUtils.currentAnimationTimeMillis();
        boolean still = Motion.reduced(getContext());
        float shortest = Math.min(getWidth(), getHeight());

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item.bitmap.isRecycled()) {
                continue;
            }
            MissionScene.Anchor anchor = scene.anchor(item.slot);

            float targetX = anchor.x * getWidth();
            float targetY = anchor.y * getHeight();
            float targetSize = anchor.scale * shortest;

            float x = targetX;
            float y = targetY;
            float scale = 1f;
            float alpha = 1f;

            if (item.arrival < 1f) {
                float t = overshoot.getInterpolation(item.arrival);
                // Comes in from below, where the camera was, and arcs up into place.
                float startX = getWidth() * 0.5f;
                float startY = getHeight() * 1.15f;
                x = startX + (targetX - startX) * t;
                y = startY + (targetY - startY) * t;
                y -= (float) Math.sin(Math.min(1f, item.arrival) * Math.PI) * getHeight() * 0.18f;
                scale = 0.4f + 0.6f * t;
                alpha = Math.min(1f, item.arrival * 3f);
            } else if (!still) {
                float phase = ((now + i * 620L) % BOB_PERIOD_MS) / (float) BOB_PERIOD_MS;
                y += (float) Math.sin(phase * 2 * Math.PI) * shortest * 0.012f;
            }

            float ratio = item.bitmap.getHeight() / (float) item.bitmap.getWidth();
            float drawWidth = targetSize * scale;
            float drawHeight = drawWidth * ratio;
            rect.set(x - drawWidth / 2f, y - drawHeight / 2f,
                    x + drawWidth / 2f, y + drawHeight / 2f);
            source.set(0, 0, item.bitmap.getWidth(), item.bitmap.getHeight());

            paint.setAlpha(Math.round(alpha * 255f));
            int save = canvas.save();
            canvas.rotate(anchor.rotation * item.arrival, x, y);
            canvas.drawBitmap(item.bitmap, source, rect, paint);
            canvas.restoreToCount(save);
        }
    }
}
