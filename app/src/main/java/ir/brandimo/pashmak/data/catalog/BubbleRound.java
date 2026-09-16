package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * One round of bubble pop, and the rule that decides what counts.
 *
 * <p>The game used to give a star for any bubble at all, which asks nothing of the
 * child. A round now names a thing to look for — this letter, this number, letters
 * but not numbers — so popping becomes reading.
 */
public final class BubbleRound {

    public enum Kind {
        /** One named letter: "pop the چ". */
        LETTER,
        /** One named digit: "pop the ۵". */
        DIGIT,
        /** Any letter, no digits. */
        ALL_LETTERS,
        /** Any digit, no letters. */
        ALL_DIGITS
    }

    public final Kind kind;
    /** The glyph to look for, for the two single-target kinds; empty otherwise. */
    @NonNull
    public final String target;
    /** How many correct pops finish the round. */
    public final int goal;

    BubbleRound(Kind kind, @Nullable String target, int goal) {
        this.kind = kind;
        this.target = target == null ? "" : target;
        this.goal = goal;
    }

    /** Whether popping this bubble should score. */
    public boolean accepts(@NonNull String label, boolean isDigit) {
        switch (kind) {
            case LETTER:
            case DIGIT:
                return label.equals(target);
            case ALL_LETTERS:
                return !isDigit;
            case ALL_DIGITS:
                return isDigit;
            default:
                return true;
        }
    }
}
