package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A story Pashmak simply tells. No taps, no choices — the child listens while the
 * scene behind him changes with the telling, which is the point: a wind-down, not
 * another thing to be good at.
 */
public final class Tale {

    /**
     * How the subject of a passage moves.
     *
     * <p>A told tale had a place behind it and nothing in the place: the brave
     * snail's scene was an empty meadow. What a three-year-old is looking for is the
     * thing being talked about, so each passage names one and says how it moves.
     */
    public enum Motion {
        /** Stays where it is put. */
        STILL,
        /** Crosses the ground slowly, the way a walked-to place is reached. */
        WALK,
        /** Floats across the sky: clouds, kites, a paper bird. */
        DRIFT,
        /** Stays put and bobs, which is what alive-but-waiting looks like. */
        BOB,
        /** Climbs slowly out of frame: smoke, a balloon, the moon. */
        RISE,
        /** Turns gently on the spot: a wheel, a button, a coin. */
        SPIN
    }

    /** One passage of the telling, the place it happens in, and who is in it. */
    public static final class Moment {
        @NonNull
        public final String text;
        /** Index into this tale's own scene list. */
        public final int scene;
        /** The subject drawn into the scene, or 0 for none. */
        @DrawableRes
        public final int actor;
        @NonNull
        public final Motion motion;

        Moment(@NonNull String text, int scene) {
            this(text, scene, 0, Motion.STILL);
        }

        Moment(@NonNull String text, int scene, @DrawableRes int actor,
               @NonNull Motion motion) {
            this.text = text;
            this.scene = scene;
            this.actor = actor;
            this.motion = motion;
        }

        /** Roughly how long this passage takes to read aloud, in milliseconds. */
        public long durationMs() {
            int words = text.split("\\s+").length;
            // ~110 Persian words a minute, read gently, with a beat at the end.
            return Math.round(words / 110f * 60_000f) + 900L;
        }
    }

    public final String id;
    public final String title;
    @DrawableRes
    public final int badge;
    /** Who the tale is about, drawn into every scene of it. */
    @DrawableRes
    public final int hero;
    /** How that subject moves — a snail walks, a kite drifts, a clock turns. */
    @NonNull
    public final Motion heroMotion;
    public final List<MissionScene> scenes;
    public final List<Moment> moments;

    Tale(String id, String title, @DrawableRes int badge, @DrawableRes int hero,
         @NonNull Motion heroMotion, List<MissionScene> scenes, Moment... moments) {
        this.id = id;
        this.title = title;
        this.badge = badge;
        this.hero = hero;
        this.heroMotion = heroMotion;
        this.scenes = Collections.unmodifiableList(scenes);
        this.moments = Collections.unmodifiableList(Arrays.asList(moments));
    }

    /**
     * What to draw into the scene at this passage: whatever the passage names, or
     * the tale's own subject when it names nothing. Nearly every passage wants the
     * subject of the tale, so it is stated once per tale rather than 418 times.
     */
    @DrawableRes
    public int actorFor(int index) {
        Moment moment = moment(index);
        return moment.actor != 0 ? moment.actor : hero;
    }

    @NonNull
    public Motion motionFor(int index) {
        Moment moment = moment(index);
        return moment.actor != 0 ? moment.motion : heroMotion;
    }

    public int size() {
        return moments.size();
    }

    @NonNull
    public Moment moment(int index) {
        return moments.get(Math.max(0, Math.min(index, moments.size() - 1)));
    }

    @NonNull
    public MissionScene sceneFor(int momentIndex) {
        return scenes.get(Palette.wrap(moment(momentIndex).scene, scenes.size()));
    }

    /** The whole telling, for the running time shown on the list. */
    public long durationMs() {
        long total = 0L;
        for (int i = 0; i < moments.size(); i++) {
            total += moments.get(i).durationMs();
        }
        return total;
    }
}
