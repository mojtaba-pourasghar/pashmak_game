package ir.brandimo.pashmak.ui.coloring;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import ir.brandimo.pashmak.data.catalog.ColorRegion;
import ir.brandimo.pashmak.data.catalog.ColoringPage;

/**
 * A coloring page: tap a region, it takes the selected color. Regions are defined
 * as fractions of the page, so the same artwork fills a phone or a tablet.
 */
public class ColoringCanvasView extends View {

    public interface OnRegionTapped {
        void onRegionTapped(ColorRegion region);
    }

    private static final int OUTLINE = 0xFF2B3742;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF box = new RectF();
    private final Region hitRegion = new Region();
    private final Region clipRegion = new Region();

    private ColoringPage page;
    private Map<String, Integer> fills = new HashMap<>();
    @Nullable
    private OnRegionTapped listener;
    @Nullable
    private String flashRegionId;

    public ColoringCanvasView(Context context) {
        this(context, null);
    }

    public ColoringCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        outline.setStyle(Paint.Style.STROKE);
        outline.setColor(OUTLINE);
        outline.setStrokeJoin(Paint.Join.ROUND);
        outline.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setPage(ColoringPage page, Map<String, Integer> fills) {
        this.page = page;
        this.fills = fills == null ? new HashMap<>() : fills;
        invalidate();
    }

    public void setFills(Map<String, Integer> fills) {
        this.fills = fills == null ? new HashMap<>() : fills;
        invalidate();
    }

    public void setOnRegionTapped(@Nullable OnRegionTapped listener) {
        this.listener = listener;
    }

    /** Briefly outlines a region in green so a correct tap is visibly rewarded. */
    public void flash(String regionId) {
        flashRegionId = regionId;
        invalidate();
        postDelayed(() -> {
            flashRegionId = null;
            invalidate();
        }, 600L);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (page == null) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        outline.setStrokeWidth(Math.max(3f, 3f * density));

        for (int i = 0; i < page.regions.size(); i++) {
            ColorRegion region = page.regions.get(i);
            region.bounds(box, getWidth(), getHeight());
            region.buildPath(path, box, density);

            Integer filled = fills.get(region.id);
            fill.setColor(filled == null ? Color.WHITE : filled);
            canvas.drawPath(path, fill);

            boolean isFlashing = region.id.equals(flashRegionId);
            outline.setColor(isFlashing ? 0xFF43B02A : OUTLINE);
            outline.setStrokeWidth(isFlashing ? 5f * density : 3f * density);
            canvas.drawPath(path, outline);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (page == null || event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        ColorRegion hit = regionAt(event.getX(), event.getY());
        if (hit != null) {
            performClick();
            if (listener != null) {
                listener.onRegionTapped(hit);
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Walks the list backwards so the region drawn on top wins the tap. */
    @Nullable
    private ColorRegion regionAt(float x, float y) {
        float density = getResources().getDisplayMetrics().density;
        clipRegion.set(0, 0, getWidth(), getHeight());
        for (int i = page.regions.size() - 1; i >= 0; i--) {
            ColorRegion region = page.regions.get(i);
            region.bounds(box, getWidth(), getHeight());
            region.buildPath(path, box, density);
            hitRegion.setPath(path, clipRegion);
            if (hitRegion.contains(Math.round(x), Math.round(y))) {
                return region;
            }
        }
        return null;
    }
}
