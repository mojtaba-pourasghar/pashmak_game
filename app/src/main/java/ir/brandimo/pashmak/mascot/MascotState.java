package ir.brandimo.pashmak.mascot;

/** The seven poses the character can hold, matching the prototype's state map. */
public enum MascotState {
    /** Springs in on the splash screen, then hands off to WAVE. */
    ENTER,
    /** Greeting: breathing, head bob, one arm waving. */
    WAVE,
    /** Resting: breathing, blinking, occasional curious glance. */
    IDLE,
    /** Speaking: open mouth on the beat, quicker head bob, gesturing hand. */
    TALK,
    /** Celebrating: jumping with both arms up and sparkles. */
    CHEER,
    /** Reassuring: gentle head tilt and a thumbs-up. Never a sad face. */
    ENCOURAGE,
    /** Tickled: wobble, wink and a giggle. */
    TICKLE;

    public static MascotState from(String name) {
        if (name == null) {
            return IDLE;
        }
        for (MascotState state : values()) {
            if (state.name().equalsIgnoreCase(name)) {
                return state;
            }
        }
        return IDLE;
    }
}
