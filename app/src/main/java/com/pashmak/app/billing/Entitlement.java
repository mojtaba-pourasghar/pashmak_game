package com.pashmak.app.billing;

import com.pashmak.app.BuildConfig;

/**
 * What the free version opens, in one place.
 *
 * <p>Every game is a list of stages and the first {@link #FREE_STAGES} of each list
 * are free; the bedtime playlist gives away its first {@link #FREE_LULLABIES}. The
 * rest is the full version, bought once through Cafe Bazaar ({@link BazaarBilling}).
 * Positions are the order the child sees things in — the row in the stage list,
 * the round in a session, the song in the playlist — counted from zero.
 *
 * <p>A debug build opens everything ({@link #DEBUG_UNLOCK_ALL}), so every stage and
 * song can be tried without a purchase. Only the paid content is affected: the
 * "finish the last stage first" locks and the parent gate work as they always do.
 * A release build still needs the purchase.
 */
public final class Entitlement {

    /** Stages at the top of every game's list that need no purchase. */
    public static final int FREE_STAGES = 2;

    /** Lullabies at the top of the playlist that need no purchase. */
    public static final int FREE_LULLABIES = 1;

    /** Debug builds treat the full version as owned; release builds never do. */
    public static final boolean DEBUG_UNLOCK_ALL = BuildConfig.DEBUG;

    private Entitlement() {
    }

    /** Whether the paid content is open: bought, or a debug build. */
    private static boolean full(boolean fullVersion) {
        return fullVersion || DEBUG_UNLOCK_ALL;
    }

    public static boolean stageOpen(int position, boolean fullVersion) {
        return full(fullVersion) || position < FREE_STAGES;
    }

    public static boolean lullabyOpen(int index, boolean fullVersion) {
        return full(fullVersion) || index < FREE_LULLABIES;
    }

    /** How many songs from the top of the playlist may play. */
    public static int playableLullabies(boolean fullVersion) {
        return full(fullVersion) ? Integer.MAX_VALUE : FREE_LULLABIES;
    }
}
