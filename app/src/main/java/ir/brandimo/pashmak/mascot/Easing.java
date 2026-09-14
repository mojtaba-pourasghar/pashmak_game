package ir.brandimo.pashmak.mascot;

/** The handful of CSS timing functions the prototype's keyframes rely on. */
public enum Easing {
    LINEAR,
    EASE_IN_OUT,
    EASE_OUT,
    /** cubic-bezier(.28,1.2,.4,1) — the springy pop-in. */
    SPRING;

    public float apply(float t) {
        float x = t < 0f ? 0f : (t > 1f ? 1f : t);
        switch (this) {
            case EASE_IN_OUT:
                return x * x * (3f - 2f * x);
            case EASE_OUT: {
                float inv = 1f - x;
                return 1f - inv * inv * inv;
            }
            case SPRING: {
                // Overshoots just past 1 around three quarters through, then settles.
                float inv = 1f - x;
                float base = 1f - inv * inv * inv;
                return base + 0.18f * (float) Math.sin(Math.PI * x) * inv;
            }
            case LINEAR:
            default:
                return x;
        }
    }
}
