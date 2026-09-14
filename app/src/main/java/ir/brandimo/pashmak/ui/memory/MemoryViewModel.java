package ir.brandimo.pashmak.ui.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Six pairs by default; difficulty widens or narrows the board. */
public class MemoryViewModel extends ViewModel {

    public static final int STATE_FACE_DOWN = 0;
    public static final int STATE_FACE_UP = 1;
    public static final int STATE_MATCHED = 2;

    private final MutableLiveData<List<Integer>> cards = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<int[]> states = new MutableLiveData<>(new int[0]);
    private final MutableLiveData<Integer> matchedPairs = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>(false);

    private final List<Integer> faceUp = new ArrayList<>();
    private final Random random = new Random();
    private int pairCount = 6;
    private boolean locked;

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

    public int pairCount() {
        return pairCount;
    }

    public void reset(int pairs) {
        pairCount = Math.max(2, pairs);
        List<Integer> deck = new ArrayList<>(pairCount * 2);
        for (int i = 0; i < pairCount; i++) {
            deck.add(i);
            deck.add(i);
        }
        // Fisher-Yates, same as the prototype.
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = deck.get(i);
            deck.set(i, deck.get(j));
            deck.set(j, tmp);
        }
        faceUp.clear();
        locked = false;
        cards.setValue(deck);
        states.setValue(new int[deck.size()]);
        matchedPairs.setValue(0);
        finished.setValue(false);
    }

    public boolean canFlip(int position) {
        int[] current = states.getValue();
        return !locked && current != null && position < current.length
                && current[position] == STATE_FACE_DOWN && faceUp.size() < 2;
    }

    /** Flips a card and reports whether that completed a pair, or missed. */
    public Outcome flip(int position) {
        if (!canFlip(position)) {
            return Outcome.IGNORED;
        }
        int[] current = states.getValue();
        List<Integer> deck = cards.getValue();
        if (current == null || deck == null) {
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
        int first = faceUp.get(0);
        int second = faceUp.get(1);
        return deck.get(first).equals(deck.get(second)) ? Outcome.MATCH : Outcome.MISS;
    }

    /** Called after the reveal pause, to settle the two face-up cards. */
    public void settle(boolean matched) {
        int[] current = states.getValue();
        if (current == null) {
            locked = false;
            faceUp.clear();
            return;
        }
        int[] next = current.clone();
        for (int i = 0; i < faceUp.size(); i++) {
            int position = faceUp.get(i);
            next[position] = matched ? STATE_MATCHED : STATE_FACE_DOWN;
        }
        faceUp.clear();
        locked = false;
        states.setValue(next);

        if (matched) {
            int pairs = (matchedPairs.getValue() == null ? 0 : matchedPairs.getValue()) + 1;
            matchedPairs.setValue(pairs);
            if (pairs >= pairCount) {
                finished.setValue(true);
            }
        }
    }

    public enum Outcome {
        IGNORED, FLIPPED, MATCH, MISS
    }
}
