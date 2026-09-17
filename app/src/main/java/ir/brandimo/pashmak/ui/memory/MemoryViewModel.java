package ir.brandimo.pashmak.ui.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ir.brandimo.pashmak.data.catalog.MemoryCatalog;
import ir.brandimo.pashmak.data.catalog.MemoryDeck;

/** One deck, one level, one board. */
public class MemoryViewModel extends ViewModel {

    public static final int STATE_FACE_DOWN = 0;
    public static final int STATE_FACE_UP = 1;
    public static final int STATE_MATCHED = 2;

    public enum Outcome {
        IGNORED, FLIPPED, MATCH, MISS
    }

    private final MutableLiveData<List<Integer>> cards = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<int[]> states = new MutableLiveData<>(new int[0]);
    private final MutableLiveData<Integer> matchedPairs = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>(false);

    private final List<Integer> faceUp = new ArrayList<>();
    private final Random random = new Random();

    private int deckIndex;
    private int level;
    private int pairCount;
    private boolean locked;
    private boolean awarded;

    public LiveData<List<Integer>> cards() {
        return cards;
    }

    public LiveData<int[]> states() {
        return states;
    }

    public LiveData<Integer> matchedPairs() {
        return matchedPairs;
    }

    public LiveData<Boolean> finished() {
        return finished;
    }

    public int deckIndex() {
        return deckIndex;
    }

    public int level() {
        return level;
    }

    public int pairCount() {
        return pairCount;
    }

    public MemoryDeck deck() {
        return MemoryCatalog.deck(deckIndex);
    }

    /** Deals a fresh board for the chosen deck and level. */
    /**
     * True the first time this board is finished and false ever after.
     *
     * <p>The screen learns the board is finished from LiveData, and a new Activity
     * after a rotation is handed the "finished" it was already showing — which
     * without this handed out the stars, the fanfare and the jump to the next level
     * a second time.
     */
    public boolean claimLevelAward() {
        if (awarded) {
            return false;
        }
        awarded = true;
        return true;
    }

    public void deal(int deckIndex, int level) {
        awarded = false;
        this.deckIndex = deckIndex;
        this.level = level;
        MemoryDeck deck = MemoryCatalog.deck(deckIndex);
        pairCount = Math.min(MemoryCatalog.pairsForLevel(level), deck.size());

        List<Integer> board = new ArrayList<>(pairCount * 2);
        for (int i = 0; i < pairCount; i++) {
            board.add(i);
            board.add(i);
        }
        for (int i = board.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = board.get(i);
            board.set(i, board.get(j));
            board.set(j, tmp);
        }

        faceUp.clear();
        locked = false;
        cards.setValue(board);
        states.setValue(new int[board.size()]);
        matchedPairs.setValue(0);
        finished.setValue(false);
    }

    public Outcome flip(int position) {
        int[] current = states.getValue();
        List<Integer> board = cards.getValue();
        if (locked || current == null || board == null
                || position >= current.length
                || current[position] != STATE_FACE_DOWN
                || faceUp.size() >= 2) {
            return Outcome.IGNORED;
        }
        int[] next = current.clone();
        next[position] = STATE_FACE_UP;
        faceUp.add(position);
        states.setValue(next);

        if (faceUp.size() < 2) {
            return Outcome.FLIPPED;
        }
        locked = true;
        return board.get(faceUp.get(0)).equals(board.get(faceUp.get(1)))
                ? Outcome.MATCH : Outcome.MISS;
    }

    /** Settles the two face-up cards once the child has had time to see them. */
    public void settle(boolean matched) {
        int[] current = states.getValue();
        if (current != null) {
            int[] next = current.clone();
            for (int i = 0; i < faceUp.size(); i++) {
                next[faceUp.get(i)] = matched ? STATE_MATCHED : STATE_FACE_DOWN;
            }
            states.setValue(next);
        }
        faceUp.clear();
        locked = false;

        if (matched) {
            int pairs = (matchedPairs.getValue() == null ? 0 : matchedPairs.getValue()) + 1;
            matchedPairs.setValue(pairs);
            if (pairs >= pairCount) {
                finished.setValue(true);
            }
        }
    }

    /**
     * The next level has to exist *and* fit in this deck — the drawings deck can be
     * smaller than the later levels want, and the stage list locks those, so rolling
     * on to one would contradict it.
     */
    public boolean hasNextLevel() {
        int next = level + 1;
        return next < MemoryCatalog.levelCount()
                && MemoryCatalog.pairsForLevel(next) <= deck().size();
    }
}
