package ir.brandimo.pashmak.ui.common;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import ir.brandimo.pashmak.data.catalog.Palette;

/** The row of color swatches shared by the coloring and free-drawing screens. */
public class SwatchBar extends LinearLayout {

    public interface OnColorPicked {
        void onColorPicked(int color);
    }

    private int[] colors = Palette.SWATCHES;
    private int selected = Palette.RED;
    @Nullable
    private OnColorPicked listener;

    public SwatchBar(Context context) {
        this(context, null);
    }

    public SwatchBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        build();
    }

    public void setColors(int[] next) {
        colors = next;
        build();
    }

    public void setOnColorPicked(@Nullable OnColorPicked listener) {
        this.listener = listener;
    }

    public void setSelected(int color) {
        selected = color;
        refresh();
    }

    public int selectedColor() {
        return selected;
    }

    private void build() {
        removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int size = Math.round(40 * density);
        int margin = Math.round(3 * density);
        for (int i = 0; i < colors.length; i++) {
            final int color = colors[i];
            View swatch = new View(getContext());
            LayoutParams params = new LayoutParams(0, size, 1f);
            params.setMargins(margin, margin, margin, margin);
            swatch.setLayoutParams(params);
            swatch.setTag(color);
            swatch.setOnClickListener(v -> {
                setSelected(color);
                if (listener != null) {
                    listener.onColorPicked(color);
                }
            });
            addView(swatch);
        }
        refresh();
    }

    private void refresh() {
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Object tag = child.getTag();
            if (!(tag instanceof Integer)) {
                continue;
            }
            int color = (Integer) tag;
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(14 * density);
            shape.setColor(color);
            // The selected swatch gets a dark ring, matching the design.
            shape.setStroke(Math.round(3 * density),
                    color == selected ? 0xFF2B3742 : 0xFFFFFFFF);
            child.setBackground(shape);
        }
    }
}
