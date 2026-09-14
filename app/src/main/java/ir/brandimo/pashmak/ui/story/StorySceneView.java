package ir.brandimo.pashmak.ui.story;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.StoryScene;

/** A tappable story scene: poke a prop, it wobbles and says something. */
public class StorySceneView extends View {

    public interface OnPropTapped {
        void onPropTapped(StoryScene.Prop prop);
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    @Nullable
    private StoryScene scene;
    @Nullable
    private OnPropTapped listener;
    private int pressedIndex = -1;

    public StorySceneView(Context context) {
        this(context, null);
    }

    public StorySceneView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        outline.setStyle(Paint.Style.STROKE);
        outline.setColor(0xFF2B3742);
        outline.setStrokeWidth(3f * getResources().getDisplayMetrics().density);
        label.setColor(Color.WHITE);
        label.setTextAlign(Paint.Align.CENTER);
        Typeface lalezar = ResourcesCompat.getFont(context, R.font.lalezar_regular);
        if (lalezar != null) {
            label.setTypeface(lalezar);
        }
    }

    public void setScene(@Nullable StoryScene scene) {
        this.scene = scene;
        pressedIndex = -1;
        invalidate();
    }

    public void setOnPropTapped(@Nullable OnPropTapped listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (scene == null) {
            return;
        }
        float shortest = Math.min(getWidth(), getHeight());
        label.setTextSize(shortest * 0.055f);
        for (int i = 0; i < scene.props.size(); i++) {
            StoryScene.Prop prop = scene.props.get(i);
            float size = prop.size * shortest;
            float scale = i == pressedIndex ? 1.12f : 1f;
            float half = size * scale / 2f;
            float cx = prop.x * getWidth();
            float cy = prop.y * getHeight();
            box.set(cx - half, cy - half, cx + half, cy + half);

            fill.setColor(prop.color);
            if (prop.round) {
                canvas.drawOval(box, fill);
                canvas.drawOval(box, outline);
            } else {
                float radius = size * 0.18f;
                canvas.drawRoundRect(box, radius, radius, fill);
                canvas.drawRoundRect(box, radius, radius, outline);
            }
            canvas.drawText(prop.label, cx, box.bottom + label.getTextSize() * 1.1f, labelPaint());
        }
    }

    private Paint labelPaint() {
        label.setColor(0xFF7A4A02);
        return label;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scene == null) {
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int index = propAt(event.getX(), event.getY());
            if (index >= 0) {
                pressedIndex = index;
                invalidate();
                performClick();
                if (listener != null) {
                    listener.onPropTapped(scene.props.get(index));
                }
                postDelayed(() -> {
                    pressedIndex = -1;
                    invalidate();
                }, 260L);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int propAt(float x, float y) {
        float shortest = Math.min(getWidth(), getHeight());
        for (int i = scene.props.size() - 1; i >= 0; i--) {
            StoryScene.Prop prop = scene.props.get(i);
            float half = prop.size * shortest / 2f;
            float cx = prop.x * getWidth();
            float cy = prop.y * getHeight();
            if (x >= cx - half && x <= cx + half && y >= cy - half && y <= cy + half) {
                return i;
            }
        }
        return -1;
    }
}
