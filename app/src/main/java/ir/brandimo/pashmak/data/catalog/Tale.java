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

    /** One passage of the telling, and the place it happens in. */
    public static final class Moment {
        @NonNull
        public final String text;
        /** Index into this tale's own scene list. */
        public final int scene;

        Moment(@NonNull String text, int scene) {
            this.text = text;
            this.scene = scene;
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
    public final List<MissionScene> scenes;
    public final List<Moment> moments;

    Tale(String id, String title, @DrawableRes int badge,
         List<MissionScene> scenes, Moment... moments) {
        this.id = id;
        this.title = title;
        this.badge = badge;
        this.scenes = Collections.unmodifiableList(scenes);
        this.moments = Collections.unmodifiableList(Arrays.asList(moments));
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
