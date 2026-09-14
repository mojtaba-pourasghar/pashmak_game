package ir.brandimo.pashmak.mascot;

/** A sampled transform for one body part on one frame. */
public final class Xform {

    public float translateX;
    public float translateY;
    public float scaleX = 1f;
    public float scaleY = 1f;
    public float rotation;
    public float alpha = 1f;

    public void reset() {
        translateX = 0f;
        translateY = 0f;
        scaleX = 1f;
        scaleY = 1f;
        rotation = 0f;
        alpha = 1f;
    }
}
