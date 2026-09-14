package ir.brandimo.pashmak.data.catalog;

import java.util.Collections;
import java.util.List;

/** A story beat: a line of narration plus the things the child can poke. */
public final class StoryScene {

    public static final class Prop {
        public final String label;
        public final int color;
        public final float x;
        public final float y;
        public final float size;
        public final boolean round;
        public final String line;

        Prop(String label, int color, float x, float y, float size, boolean round, String line) {
            this.label = label;
            this.color = color;
            this.x = x;
            this.y = y;
            this.size = size;
            this.round = round;
            this.line = line;
        }
    }

    public final List<Prop> props;

    StoryScene(List<Prop> props) {
        this.props = Collections.unmodifiableList(props);
    }
}
