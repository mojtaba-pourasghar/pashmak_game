package ir.brandimo.pashmak.data.catalog;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.util.FaNum;

/**
 * Builds a session's worth of bubble rounds: a shuffled run of rules so no two
 * sittings are the same, always opening on a single named letter because that is
 * the easiest rule for a three-year-old to hold in their head.
 */
public final class BubbleRoundCatalog {

    private static final int ROUNDS = 8;
    private static final int GOAL = 5;

    private BubbleRoundCatalog() {
    }

    @NonNull
    public static List<BubbleRound> session(@NonNull Context context) {
        return session(context, new Random().nextLong());
    }

    /**
     * The same seed gives back the same run of rounds. Turning the screen rebuilds
     * the screen from scratch, and without this the child would be handed a new set
     * of rules and a score of zero for having tilted the tablet.
     */
    @NonNull
    public static List<BubbleRound> session(@NonNull Context context, long seed) {
        Random random = new Random(seed);
        String[] letters = TraceCatalog.letters(context);
        String[] digits = TraceCatalog.digits(context);

        List<BubbleRound> rounds = new ArrayList<>();
        // Open on one letter — the rule the child can hear and then go and find.
        rounds.add(new BubbleRound(BubbleRound.Kind.LETTER,
                letters[random.nextInt(letters.length)], GOAL));

        List<BubbleRound> rest = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            rest.add(new BubbleRound(BubbleRound.Kind.LETTER,
                    letters[random.nextInt(letters.length)], GOAL));
        }
        for (int i = 0; i < 2; i++) {
            rest.add(new BubbleRound(BubbleRound.Kind.DIGIT,
                    digits[random.nextInt(digits.length)], GOAL));
        }
        rest.add(new BubbleRound(BubbleRound.Kind.ALL_LETTERS, null, GOAL + 2));
        rest.add(new BubbleRound(BubbleRound.Kind.ALL_DIGITS, null, GOAL + 2));
        Collections.shuffle(rest, random);

        for (int i = 0; i < rest.size() && rounds.size() < ROUNDS; i++) {
            rounds.add(rest.get(i));
        }
        return rounds;
    }

    /** What Pashmak announces at the start of the round. */
    @NonNull
    public static String prompt(@NonNull Context context, @NonNull BubbleRound round) {
        switch (round.kind) {
            case LETTER:
                return context.getString(R.string.bubbles_goal_letter, round.target);
            case DIGIT:
                return context.getString(R.string.bubbles_goal_digit, round.target);
            case ALL_LETTERS:
                return context.getString(R.string.bubbles_goal_letters);
            case ALL_DIGITS:
            default:
                return context.getString(R.string.bubbles_goal_digits);
        }
    }

    /** The short reminder kept on screen while the round runs. */
    @NonNull
    public static String banner(@NonNull Context context, @NonNull BubbleRound round,
                                int popped) {
        return context.getString(R.string.bubbles_progress,
                prompt(context, round), FaNum.of(popped), FaNum.of(round.goal));
    }
}
