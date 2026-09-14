package ir.brandimo.pashmak.mascot;

import java.util.EnumMap;
import java.util.Map;

/**
 * The prototype's CSS @keyframes, transcribed one-for-one. Every track is a list
 * of stops in the same order and with the same percentages as the stylesheet, so
 * the Android character moves exactly like the design.
 */
public final class MascotAnims {

    /** Body parts that carry their own animation track. */
    public enum Part {
        ROOT, HEAD, ARM_L, ARM_R, LID_L, LID_R, PUPILS, MOUTH, SPARK_1, SPARK_2, SPARK_3
    }

    /** One keyframe stop: position 0..1 plus the transform at that stop. */
    static final class Stop {
        final float at;
        final float tx;
        final float ty;
        final float sx;
        final float sy;
        final float rot;
        final float alpha;

        Stop(float at, float tx, float ty, float sx, float sy, float rot, float alpha) {
            this.at = at;
            this.tx = tx;
            this.ty = ty;
            this.sx = sx;
            this.sy = sy;
            this.rot = rot;
            this.alpha = alpha;
        }
    }

    /** A named curve with its duration, easing, delay and loop behavior. */
    static final class Track {
        final Stop[] stops;
        final long periodMs;
        final long delayMs;
        final boolean loop;
        final Easing easing;

        Track(Stop[] stops, long periodMs, long delayMs, boolean loop, Easing easing) {
            this.stops = stops;
            this.periodMs = periodMs;
            this.delayMs = delayMs;
            this.loop = loop;
            this.easing = easing;
        }

        void eval(long elapsedMs, Xform out) {
            out.reset();
            long time = elapsedMs - delayMs;
            if (time < 0) {
                apply(stops[0], out);
                return;
            }
            float u;
            if (loop) {
                u = (time % periodMs) / (float) periodMs;
            } else {
                u = time >= periodMs ? 1f : time / (float) periodMs;
            }
            int i = 0;
            while (i < stops.length - 2 && u > stops[i + 1].at) {
                i++;
            }
            Stop a = stops[i];
            Stop b = stops[i + 1];
            float span = b.at - a.at;
            float t = span <= 0f ? 1f : easing.apply((u - a.at) / span);
            out.translateX = lerp(a.tx, b.tx, t);
            out.translateY = lerp(a.ty, b.ty, t);
            out.scaleX = lerp(a.sx, b.sx, t);
            out.scaleY = lerp(a.sy, b.sy, t);
            out.rotation = lerp(a.rot, b.rot, t);
            out.alpha = lerp(a.alpha, b.alpha, t);
        }

        private static void apply(Stop s, Xform out) {
            out.translateX = s.tx;
            out.translateY = s.ty;
            out.scaleX = s.sx;
            out.scaleY = s.sy;
            out.rotation = s.rot;
            out.alpha = s.alpha;
        }

        private static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }

    // ---- Curve definitions (values are in the 200x224 viewBox coordinate space) ----

    private static Stop stop(float at, float tx, float ty, float sx, float sy, float rot, float alpha) {
        return new Stop(at, tx, ty, sx, sy, rot, alpha);
    }

    private static final Stop[] POP = {
            stop(0f, 0, 26, .4f, .4f, 0, 0),
            stop(.60f, 0, -6, 1.08f, 1.08f, 0, 1),
            stop(.80f, 0, 2, .96f, .96f, 0, 1),
            stop(1f, 0, 0, 1f, 1f, 0, 1)
    };
    private static final Stop[] BREATHE = {
            stop(0f, 0, 0, 1f, 1f, 0, 1),
            stop(.5f, 0, -4, 1.025f, 1.025f, 0, 1),
            stop(1f, 0, 0, 1f, 1f, 0, 1)
    };
    private static final Stop[] JUMP = {
            stop(0f, 0, 0, 1f, 1f, 0, 1),
            stop(.25f, 0, -26, 1.04f, 1.04f, 0, 1),
            stop(.45f, 0, 0, .97f, .97f, 0, 1),
            stop(.65f, 0, -14, 1.02f, 1.02f, 0, 1),
            stop(1f, 0, 0, 1f, 1f, 0, 1)
    };
    private static final Stop[] WOBBLE = {
            stop(0f, 0, 0, 1f, 1f, 0, 1),
            stop(.20f, 0, 0, 1.03f, 1.03f, -5, 1),
            stop(.45f, 0, 0, .98f, .98f, 5, 1),
            stop(.70f, 0, 0, 1.02f, 1.02f, -3, 1),
            stop(1f, 0, 0, 1f, 1f, 0, 1)
    };
    private static final Stop[] BOB = {
            stop(0f, 0, 0, 1f, 1f, -1, 1),
            stop(.5f, 0, -4, 1f, 1f, 1.5f, 1),
            stop(1f, 0, 0, 1f, 1f, -1, 1)
    };
    private static final Stop[] TILT = {
            stop(0f, 0, 0, 1f, 1f, -7, 1),
            stop(.5f, 0, 0, 1f, 1f, 4, 1),
            stop(1f, 0, 0, 1f, 1f, -7, 1)
    };
    private static final Stop[] BLINK = {
            stop(0f, 0, 0, 1f, 1f, 0, 1),
            stop(.91f, 0, 0, 1f, 1f, 0, 1),
            stop(.955f, 0, 0, 1f, .08f, 0, 1),
            stop(1f, 0, 0, 1f, 1f, 0, 1)
    };
    private static final Stop[] WINK = {
            stop(0f, 0, 0, 1f, 1f, 0, 1),
            stop(.12f, 0, 0, 1f, .08f, 0, 1),
            stop(.26f, 0, 0, 1f, .08f, 0, 1),
            stop(.34f, 0, 0, 1f, 1f, 0, 1),
            stop(1f, 0, 0, 1f, 1f, 0, 1)
    };
    private static final Stop[] GLANCE = {
            stop(0f, 0, 0, 1f, 1f, 0, 1),
            stop(.62f, 0, 0, 1f, 1f, 0, 1),
            stop(.70f, 5, 0, 1f, 1f, 0, 1),
            stop(.80f, 5, 0, 1f, 1f, 0, 1),
            stop(.88f, -4, 0, 1f, 1f, 0, 1),
            stop(1f, 0, 0, 1f, 1f, 0, 1)
    };
    private static final Stop[] WAVE = {
            stop(0f, 0, 0, 1f, 1f, -12, 1),
            stop(.5f, 0, 0, 1f, 1f, 22, 1),
            stop(1f, 0, 0, 1f, 1f, -12, 1)
    };
    private static final Stop[] UP_R = {
            stop(0f, 0, 0, 1f, 1f, -8, 1),
            stop(1f, 0, 0, 1f, 1f, -52, 1)
    };
    private static final Stop[] UP_L = {
            stop(0f, 0, 0, 1f, 1f, 8, 1),
            stop(1f, 0, 0, 1f, 1f, 52, 1)
    };
    private static final Stop[] THUMB = {
            stop(0f, 0, 0, 1f, 1f, -14, 1),
            stop(.5f, 0, -6, 1f, 1f, -34, 1),
            stop(1f, 0, 0, 1f, 1f, -14, 1)
    };
    private static final Stop[] TALK = {
            stop(0f, 0, 0, 1f, .45f, 0, 1),
            stop(.5f, 0, 0, 1f, 1.15f, 0, 1),
            stop(1f, 0, 0, 1f, .45f, 0, 1)
    };
    private static final Stop[] SPARK = {
            stop(0f, 0, 0, 0f, 0f, 0, 0),
            stop(.40f, 0, 0, 1.1f, 1.1f, 40, 1),
            stop(1f, 0, 0, .3f, .3f, 90, 0)
    };
    private static final Stop[] STILL = {
            stop(0f, 0, 0, 1f, 1f, 0, 1),
            stop(1f, 0, 0, 1f, 1f, 0, 1)
    };

    static final Track NONE = new Track(STILL, 1000, 0, true, Easing.LINEAR);

    private static Track loop(Stop[] stops, long period, Easing easing) {
        return new Track(stops, period, 0, true, easing);
    }

    private static Track loop(Stop[] stops, long period, long delay, Easing easing) {
        return new Track(stops, period, delay, true, easing);
    }

    private static Track once(Stop[] stops, long period, Easing easing) {
        return new Track(stops, period, 0, false, easing);
    }

    /** Blink shares one 5s cycle across every state, exactly as the prototype does. */
    private static final Track BLINK_TRACK = loop(BLINK, 5000, Easing.EASE_IN_OUT);

    private static final Map<MascotState, Map<Part, Track>> STATES = build();

    private MascotAnims() {
    }

    static Track trackFor(MascotState state, Part part) {
        Map<Part, Track> tracks = STATES.get(state);
        if (tracks == null) {
            tracks = STATES.get(MascotState.IDLE);
        }
        Track track = tracks == null ? null : tracks.get(part);
        return track == null ? NONE : track;
    }

    /** Whether the state shows the open talking mouth instead of the closed smile. */
    static boolean mouthOpen(MascotState state) {
        return state == MascotState.TALK || state == MascotState.CHEER
                || state == MascotState.TICKLE;
    }

    static boolean showsThumb(MascotState state) {
        return state == MascotState.ENCOURAGE;
    }

    static boolean showsSparkles(MascotState state) {
        return state == MascotState.CHEER;
    }

    private static Map<MascotState, Map<Part, Track>> build() {
        Map<MascotState, Map<Part, Track>> map = new EnumMap<>(MascotState.class);

        Map<Part, Track> idle = tracks();
        idle.put(Part.ROOT, loop(BREATHE, 3600, Easing.EASE_IN_OUT));
        idle.put(Part.PUPILS, loop(GLANCE, 7500, Easing.EASE_IN_OUT));
        idle.put(Part.LID_L, BLINK_TRACK);
        idle.put(Part.LID_R, BLINK_TRACK);
        map.put(MascotState.IDLE, idle);

        Map<Part, Track> wave = tracks();
        wave.put(Part.ROOT, loop(BREATHE, 3600, Easing.EASE_IN_OUT));
        wave.put(Part.HEAD, loop(BOB, 2400, Easing.EASE_IN_OUT));
        wave.put(Part.ARM_R, loop(WAVE, 1500, Easing.EASE_IN_OUT));
        wave.put(Part.LID_L, BLINK_TRACK);
        wave.put(Part.LID_R, BLINK_TRACK);
        map.put(MascotState.WAVE, wave);

        Map<Part, Track> talk = tracks();
        talk.put(Part.ROOT, loop(BREATHE, 4200, Easing.EASE_IN_OUT));
        talk.put(Part.HEAD, loop(BOB, 620, Easing.EASE_IN_OUT));
        talk.put(Part.ARM_R, loop(WAVE, 2600, Easing.EASE_IN_OUT));
        talk.put(Part.MOUTH, loop(TALK, 300, Easing.EASE_IN_OUT));
        talk.put(Part.LID_L, BLINK_TRACK);
        talk.put(Part.LID_R, BLINK_TRACK);
        map.put(MascotState.TALK, talk);

        Map<Part, Track> cheer = tracks();
        cheer.put(Part.ROOT, loop(JUMP, 720, Easing.EASE_OUT));
        cheer.put(Part.HEAD, loop(BOB, 500, Easing.EASE_IN_OUT));
        cheer.put(Part.ARM_L, once(UP_L, 340, Easing.EASE_OUT));
        cheer.put(Part.ARM_R, once(UP_R, 340, Easing.EASE_OUT));
        cheer.put(Part.MOUTH, loop(TALK, 340, Easing.EASE_IN_OUT));
        cheer.put(Part.SPARK_1, loop(SPARK, 1100, 0, Easing.EASE_OUT));
        cheer.put(Part.SPARK_2, loop(SPARK, 1100, 240, Easing.EASE_OUT));
        cheer.put(Part.SPARK_3, loop(SPARK, 1100, 480, Easing.EASE_OUT));
        map.put(MascotState.CHEER, cheer);

        Map<Part, Track> encourage = tracks();
        encourage.put(Part.ROOT, loop(BREATHE, 3000, Easing.EASE_IN_OUT));
        encourage.put(Part.HEAD, loop(TILT, 1500, Easing.EASE_IN_OUT));
        encourage.put(Part.ARM_R, loop(THUMB, 1100, Easing.EASE_IN_OUT));
        encourage.put(Part.LID_L, BLINK_TRACK);
        encourage.put(Part.LID_R, BLINK_TRACK);
        map.put(MascotState.ENCOURAGE, encourage);

        Map<Part, Track> tickle = tracks();
        tickle.put(Part.ROOT, loop(WOBBLE, 550, Easing.EASE_IN_OUT));
        tickle.put(Part.HEAD, loop(BOB, 450, Easing.EASE_IN_OUT));
        tickle.put(Part.ARM_R, loop(WAVE, 500, Easing.EASE_IN_OUT));
        tickle.put(Part.MOUTH, loop(TALK, 260, Easing.EASE_IN_OUT));
        tickle.put(Part.LID_L, loop(WINK, 1100, Easing.EASE_IN_OUT));
        tickle.put(Part.LID_R, BLINK_TRACK);
        map.put(MascotState.TICKLE, tickle);

        Map<Part, Track> enter = tracks();
        enter.put(Part.ROOT, once(POP, 820, Easing.SPRING));
        enter.put(Part.LID_L, BLINK_TRACK);
        enter.put(Part.LID_R, BLINK_TRACK);
        map.put(MascotState.ENTER, enter);

        return map;
    }

    private static Map<Part, Track> tracks() {
        return new EnumMap<>(Part.class);
    }
}
