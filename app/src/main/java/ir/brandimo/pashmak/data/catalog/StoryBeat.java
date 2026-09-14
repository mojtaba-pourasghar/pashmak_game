package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * One moment in a story. Most beats simply narrate; some ask the child to find
 * something in the picture, and some let them decide what happens next.
 */
public final class StoryBeat {

    public enum Type {
        /** Pashmak says a line and the child taps to continue. */
        NARRATE,
        /** Pashmak asks for help and waits for the right prop to be tapped. */
        ASK_TAP,
        /** Two ways the story could go. */
        CHOICE,
        /** The happy ending: stars and confetti. */
        CELEBRATE
    }

    public final Type type;
    public final String text;
    public final List<StoryProp> props;
    @Nullable
    public final String answerPropId;
    @Nullable
    public final String praise;
    @Nullable
    public final String optionA;
    @Nullable
    public final String optionB;
    public final int targetA;
    public final int targetB;
    /** Where to go next; -1 simply means the following beat. */
    public final int next;

    private StoryBeat(Type type, String text, List<StoryProp> props,
                      @Nullable String answerPropId, @Nullable String praise,
                      @Nullable String optionA, @Nullable String optionB,
                      int targetA, int targetB, int next) {
        this.type = type;
        this.text = text;
        this.props = Collections.unmodifiableList(props);
        this.answerPropId = answerPropId;
        this.praise = praise;
        this.optionA = optionA;
        this.optionB = optionB;
        this.targetA = targetA;
        this.targetB = targetB;
        this.next = next;
    }

    public static StoryBeat narrate(String text, StoryProp... props) {
        return new StoryBeat(Type.NARRATE, text, Arrays.asList(props),
                null, null, null, null, -1, -1, -1);
    }

    /** A narration that jumps somewhere specific — used to rejoin after a branch. */
    public static StoryBeat narrateTo(String text, int next, StoryProp... props) {
        return new StoryBeat(Type.NARRATE, text, Arrays.asList(props),
                null, null, null, null, -1, -1, next);
    }

    public static StoryBeat ask(String text, String answerPropId, String praise,
                                StoryProp... props) {
        return new StoryBeat(Type.ASK_TAP, text, Arrays.asList(props),
                answerPropId, praise, null, null, -1, -1, -1);
    }

    public static StoryBeat choice(String text, String optionA, int targetA,
                                   String optionB, int targetB, StoryProp... props) {
        return new StoryBeat(Type.CHOICE, text, Arrays.asList(props),
                null, null, optionA, optionB, targetA, targetB, -1);
    }

    public static StoryBeat celebrate(String text, StoryProp... props) {
        return new StoryBeat(Type.CELEBRATE, text, Arrays.asList(props),
                null, null, null, null, -1, -1, -1);
    }
}
