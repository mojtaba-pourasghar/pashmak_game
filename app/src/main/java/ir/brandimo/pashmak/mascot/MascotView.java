package ir.brandimo.pashmak.mascot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.mascot.MascotAnims.Part;
import ir.brandimo.pashmak.util.Motion;

/**
 * Pashmak, drawn on a Canvas. The geometry is the prototype's 200x224 SVG
 * viewBox, so one shape table serves every size the app uses — from the 330dp
 * splash hero down to the 104dp docked companion.
 *
 * All parts read one clock, which is what lets each of them run at its own
 * period the way independent CSS animations do.
 */
public class MascotView extends View {

    private static final float VB_WIDTH = 200f;
    private static final float VB_HEIGHT = 224f;
    /** The pop and raised-arm poses reach outside the box, so leave a margin. */
    private static final float FIT_INSET = 0.92f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Shader furBody;
    private Shader furHead;
    private Shader bellyShader;
    private Shader shadowShader;

    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final Matrix shaderMatrix = new Matrix();

    private final Xform root = new Xform();
    private final Xform head = new Xform();
    private final Xform armL = new Xform();
    private final Xform armR = new Xform();
    private final Xform lidL = new Xform();
    private final Xform lidR = new Xform();
    private final Xform pupils = new Xform();
    private final Xform mouth = new Xform();
    private final Xform spark1 = new Xform();
    private final Xform spark2 = new Xform();
    private final Xform spark3 = new Xform();

    private MascotState state = MascotState.IDLE;
    private long stateStartMs;
    private boolean animating;
    private boolean reducedMotion;
    private boolean speaking;

    public MascotView(Context context) {
        this(context, null);
    }

    public MascotView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MascotView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        buildShaders();
        reducedMotion = Motion.reduced(context);
        stateStartMs = AnimationUtils.currentAnimationTimeMillis();
        setContentDescription(context.getString(R.string.cd_mascot,
                context.getString(R.string.mascot_name)));
    }

    private void buildShaders() {
        int[] fur = {
                Color.parseColor("#FFBE85"), Color.parseColor("#FF7E1F"), Color.parseColor("#EB6200")
        };
        float[] furStops = {0f, 0.5f, 1f};
        // Gradient endpoints follow each shape's own box, as SVG objectBoundingBox does.
        furBody = new LinearGradient(69f, 82f, 131f, 202f, fur, furStops, Shader.TileMode.CLAMP);
        furHead = new LinearGradient(70f, 28f, 130f, 136f, fur, furStops, Shader.TileMode.CLAMP);
        bellyShader = new LinearGradient(83.2f, 107f, 116.8f, 197f,
                Color.parseColor("#FFF3E2"), Color.parseColor("#FFDDBC"), Shader.TileMode.CLAMP);

        RadialGradient shadow = new RadialGradient(100f, 213f, 66f,
                new int[]{0x38D96400, 0x00D96400}, new float[]{0f, 1f}, Shader.TileMode.CLAMP);
        shaderMatrix.reset();
        shaderMatrix.setScale(1f, 11f / 66f, 100f, 213f);
        shadow.setLocalMatrix(shaderMatrix);
        shadowShader = shadow;
    }

    /** Switches pose and restarts the clock, so once-through poses replay. */
    public void setState(@NonNull MascotState next) {
        if (next == state) {
            return;
        }
        state = next;
        stateStartMs = AnimationUtils.currentAnimationTimeMillis();
        invalidate();
    }

    @NonNull
    public MascotState getState() {
        return state;
    }

    /**
     * Drives the mouth directly from the speech bubble's typewriter, so the lips
     * move for exactly as long as words are appearing — whatever pose is held.
     */
    public void setSpeaking(boolean value) {
        if (speaking == value) {
            return;
        }
        speaking = value;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;
        if (hMode == MeasureSpec.EXACTLY && wMode != MeasureSpec.EXACTLY) {
            height = hSize;
            width = Math.round(height * VB_WIDTH / VB_HEIGHT);
            if (wMode == MeasureSpec.AT_MOST) {
                width = Math.min(width, wSize);
            }
        } else if (wMode == MeasureSpec.EXACTLY && hMode != MeasureSpec.EXACTLY) {
            width = wSize;
            height = Math.round(width * VB_HEIGHT / VB_WIDTH);
            if (hMode == MeasureSpec.AT_MOST) {
                height = Math.min(height, hSize);
            }
        } else if (wMode == MeasureSpec.EXACTLY) {
            width = wSize;
            height = hSize;
        } else {
            height = hMode == MeasureSpec.UNSPECIFIED ? Math.round(VB_HEIGHT) : hSize;
            width = Math.round(height * VB_WIDTH / VB_HEIGHT);
        }
        setMeasuredDimension(Math.max(1, width), Math.max(1, height));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        reducedMotion = Motion.reduced(getContext());
        startClock();
    }

    @Override
    protected void onDetachedFromWindow() {
        animating = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            startClock();
        } else {
            animating = false;
        }
    }

    private void startClock() {
        if (!animating && isShown()) {
            animating = true;
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        sample(AnimationUtils.currentAnimationTimeMillis() - stateStartMs);

        float scale = Math.min(getWidth() / VB_WIDTH, getHeight() / VB_HEIGHT) * FIT_INSET;
        float dx = (getWidth() - VB_WIDTH * scale) / 2f;
        float dy = (getHeight() - VB_HEIGHT * scale) / 2f;

        int base = canvas.save();
        canvas.translate(dx, dy);
        canvas.scale(scale, scale);

        drawGroundShadow(canvas);

        int rootSave = canvas.save();
        applyXform(canvas, root, 100f, 210f);
        drawFeet(canvas);
        drawBody(canvas);
        drawArmLeft(canvas);
        drawArmRight(canvas);
        drawHead(canvas);
        if (MascotAnims.showsSparkles(state) && !reducedMotion) {
            drawSparkles(canvas);
        }
        canvas.restoreToCount(rootSave);
        canvas.restoreToCount(base);

        if (animating) {
            postInvalidateOnAnimation();
        }
    }

    private void sample(long elapsed) {
        // Reduced motion tones down the sweeping poses but never stops the
        // breathing, blinking and mouth that make the character feel present.
        boolean bigPose = state == MascotState.CHEER || state == MascotState.TICKLE
                || state == MascotState.ENTER;
        if (reducedMotion && bigPose) {
            MascotAnims.calmRoot().eval(elapsed, root);
        } else {
            MascotAnims.trackFor(state, Part.ROOT).eval(elapsed, root);
        }
        MascotAnims.trackFor(state, Part.HEAD).eval(elapsed, head);
        MascotAnims.trackFor(state, Part.ARM_L).eval(elapsed, armL);
        MascotAnims.trackFor(state, Part.ARM_R).eval(elapsed, armR);
        MascotAnims.trackFor(state, Part.LID_L).eval(elapsed, lidL);
        MascotAnims.trackFor(state, Part.LID_R).eval(elapsed, lidR);
        MascotAnims.trackFor(state, Part.PUPILS).eval(elapsed, pupils);
        if (speaking) {
            MascotAnims.talkMouth().eval(elapsed, mouth);
        } else {
            MascotAnims.trackFor(state, Part.MOUTH).eval(elapsed, mouth);
        }
        MascotAnims.trackFor(state, Part.SPARK_1).eval(elapsed, spark1);
        MascotAnims.trackFor(state, Part.SPARK_2).eval(elapsed, spark2);
        MascotAnims.trackFor(state, Part.SPARK_3).eval(elapsed, spark3);
    }

    private void applyXform(Canvas canvas, Xform x, float pivotX, float pivotY) {
        canvas.translate(x.translateX, x.translateY);
        canvas.rotate(x.rotation, pivotX, pivotY);
        canvas.scale(x.scaleX, x.scaleY, pivotX, pivotY);
    }

    private void drawGroundShadow(Canvas canvas) {
        fill.setShader(shadowShader);
        fill.setAlpha(255);
        oval(canvas, 100f, 213f, 66f, 11f);
        fill.setShader(null);
    }

    private void drawFeet(Canvas canvas) {
        solid(0xFFEB6200);
        oval(canvas, 74f, 192f, 23f, 16f);
        oval(canvas, 126f, 192f, 23f, 16f);
        solid(0xFFFFD3AE);
        oval(canvas, 74f, 196f, 15f, 9f);
        oval(canvas, 126f, 196f, 15f, 9f);
    }

    private void drawBody(Canvas canvas) {
        fill.setShader(furBody);
        fill.setAlpha(255);
        oval(canvas, 100f, 142f, 62f, 60f);
        fill.setShader(bellyShader);
        oval(canvas, 100f, 152f, 42f, 45f);
        fill.setShader(null);
    }

    private void drawArmLeft(Canvas canvas) {
        int save = canvas.save();
        applyXform(canvas, armL, 44f, 122f);
        solid(0xFFF97316);
        int tilt = canvas.save();
        canvas.rotate(12f, 34f, 150f);
        oval(canvas, 34f, 150f, 17f, 28f);
        canvas.restoreToCount(tilt);
        solid(0xFFFFA857);
        oval(canvas, 30f, 174f, 14f, 14f);
        canvas.restoreToCount(save);
    }

    private void drawArmRight(Canvas canvas) {
        int save = canvas.save();
        applyXform(canvas, armR, 156f, 122f);
        solid(0xFFF97316);
        int tilt = canvas.save();
        canvas.rotate(-12f, 166f, 150f);
        oval(canvas, 166f, 150f, 17f, 28f);
        canvas.restoreToCount(tilt);
        solid(0xFFFFA857);
        oval(canvas, 170f, 174f, 14f, 14f);
        if (MascotAnims.showsThumb(state)) {
            rect.set(165f, 150f, 175f, 170f);
            canvas.drawRoundRect(rect, 5f, 5f, fill);
        }
        canvas.restoreToCount(save);
    }

    private void drawHead(Canvas canvas) {
        int save = canvas.save();
        applyXform(canvas, head, 100f, 118f);

        solid(0xFFF97316);
        oval(canvas, 52f, 48f, 19f, 19f);
        oval(canvas, 148f, 48f, 19f, 19f);
        solid(0xFFFFD3AE);
        oval(canvas, 52f, 48f, 10f, 10f);
        oval(canvas, 148f, 48f, 10f, 10f);

        fill.setShader(furHead);
        fill.setAlpha(255);
        oval(canvas, 100f, 82f, 60f, 54f);
        fill.setShader(null);

        solid(0xFFFFC48E);
        fill.setAlpha(115);
        oval(canvas, 98f, 66f, 44f, 30f);
        fill.setAlpha(255);

        drawEye(canvas, 81f, 82f, 82f, 84f, 79f, 80f, lidL);
        drawEye(canvas, 119f, 82f, 120f, 84f, 117f, 80f, lidR);

        solid(0xFFFF9E8A);
        fill.setAlpha(153);
        oval(canvas, 60f, 104f, 11f, 6.5f);
        oval(canvas, 140f, 104f, 11f, 6.5f);
        fill.setAlpha(255);

        drawMouth(canvas);
        canvas.restoreToCount(save);
    }

    private void drawEye(Canvas canvas, float cx, float cy,
                         float pupilX, float pupilY, float glintX, float glintY, Xform lid) {
        int save = canvas.save();
        // The whole eye squashes vertically; that is how the prototype blinks.
        canvas.scale(lid.scaleX, lid.scaleY, cx, cy);
        solid(0xFFFFFDF8);
        oval(canvas, cx, cy, 14f, 15f);
        int pupilSave = canvas.save();
        canvas.translate(pupils.translateX, pupils.translateY);
        solid(0xFF26324E);
        oval(canvas, pupilX, pupilY, 7.5f, 7.5f);
        solid(0xFFFFFFFF);
        oval(canvas, glintX, glintY, 2.6f, 2.6f);
        canvas.restoreToCount(pupilSave);
        canvas.restoreToCount(save);
    }

    private void drawMouth(Canvas canvas) {
        if (speaking || MascotAnims.mouthOpen(state)) {
            int save = canvas.save();
            canvas.scale(mouth.scaleX, mouth.scaleY, 100f, 106f);
            solid(0xFF26324E);
            oval(canvas, 100f, 110f, 13f, 10f);
            canvas.restoreToCount(save);
        } else {
            stroke.setColor(0xFF26324E);
            stroke.setStrokeWidth(5f);
            path.reset();
            path.moveTo(87f, 104f);
            path.quadTo(100f, 116f, 113f, 104f);
            canvas.drawPath(path, stroke);
        }
    }

    private void drawSparkles(Canvas canvas) {
        solid(0xFFFFC730);
        drawSparkle(canvas, spark1, 28f, 74f, 8f);
        drawSparkle(canvas, spark2, 170f, 56f, 6.6f);
        drawSparkle(canvas, spark3, 152f, 112f, 4f);
    }

    private void drawSparkle(Canvas canvas, Xform x, float cx, float cy, float radius) {
        int save = canvas.save();
        canvas.rotate(x.rotation, cx, cy);
        canvas.scale(x.scaleX, x.scaleY, cx, cy);
        fill.setAlpha(Math.round(Math.max(0f, Math.min(1f, x.alpha)) * 255f));
        star(canvas, cx, cy, radius, radius * 0.42f);
        fill.setAlpha(255);
        canvas.restoreToCount(save);
    }

    private void star(Canvas canvas, float cx, float cy, float outer, float inner) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            float r = (i % 2 == 0) ? outer : inner;
            double angle = Math.toRadians(-90 + i * 36);
            float px = cx + (float) Math.cos(angle) * r;
            float py = cy + (float) Math.sin(angle) * r;
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        path.close();
        canvas.drawPath(path, fill);
    }

    private void solid(int color) {
        fill.setShader(null);
        fill.setColor(color);
    }

    private void oval(Canvas canvas, float cx, float cy, float rx, float ry) {
        rect.set(cx - rx, cy - ry, cx + rx, cy + ry);
        canvas.drawOval(rect, fill);
    }
}
