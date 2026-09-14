package ir.brandimo.pashmak.ui.memory;

import android.content.Intent;

import androidx.annotation.ColorRes;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.MemoryCatalog;
import ir.brandimo.pashmak.data.catalog.MemoryDeck;
import ir.brandimo.pashmak.ui.common.Stage;
import ir.brandimo.pashmak.ui.common.StagePickerActivity;
import ir.brandimo.pashmak.util.FaNum;

/** Six decks, five levels each — thirty named boards, unlocked in order. */
public class MemoryPickerActivity extends StagePickerActivity {

    @Override
    protected String screenTitle() {
        return getString(R.string.game_memory_title);
    }

    @Override
    protected String screenHint() {
        return getString(R.string.stage_pick_hint);
    }

    @Override
    @ColorRes
    protected int accentColor() {
        return R.color.red;
    }

    @Override
    protected List<Stage> buildStages() {
        List<Stage> stages = new ArrayList<>();
        for (int deckIndex = 0; deckIndex < MemoryCatalog.deckCount(); deckIndex++) {
            MemoryDeck deck = MemoryCatalog.deck(deckIndex);
            int best = prefs.memoryProgress(deck.id);
            for (int level = 0; level < MemoryCatalog.levelCount(); level++) {
                int pairs = Math.min(MemoryCatalog.pairsForLevel(level), deck.size());
                stages.add(new Stage(deckIndex * 100 + level,
                        deck.name + " — " + getString(R.string.stage_level,
                                FaNum.of(level + 1)),
                        getString(R.string.stage_pairs, FaNum.of(pairs)), "",
                        level <= best, level > best + 1, deck.badge, null));
            }
        }
        return stages;
    }

    @Override
    protected void onStageChosen(Stage stage) {
        Intent intent = new Intent(this, MemoryActivity.class);
        intent.putExtra(MemoryActivity.EXTRA_DECK, stage.index / 100);
        intent.putExtra(MemoryActivity.EXTRA_LEVEL, stage.index % 100);
        startActivity(intent);
    }
}
