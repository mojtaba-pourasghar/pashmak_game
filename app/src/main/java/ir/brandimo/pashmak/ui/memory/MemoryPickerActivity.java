package ir.brandimo.pashmak.ui.memory;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.MemoryCatalog;
import ir.brandimo.pashmak.data.catalog.MemoryDeck;
import ir.brandimo.pashmak.data.repo.MissionRepository;
import ir.brandimo.pashmak.ui.common.Stage;
import ir.brandimo.pashmak.ui.common.StagePickerActivity;
import ir.brandimo.pashmak.util.FaNum;

/**
 * Six authored decks, five levels each — plus a seventh made of the child's own
 * scanned drawings, which appears once there are enough of them to deal a board.
 */
public class MemoryPickerActivity extends StagePickerActivity {

    private MissionRepository missions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        missions = MissionRepository.get(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The child may have scanned a drawing since the last look; rebuild once the
        // rows come back off disk. super.onResume() has already drawn the rest.
        missions.loadDrawingDeck(available -> refreshStages());
    }

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
        for (int deckIndex = 0; deckIndex < MemoryCatalog.STATIC_COUNT; deckIndex++) {
            addDeck(stages, deckIndex, MemoryCatalog.deck(deckIndex));
        }
        addDrawings(stages);
        return stages;
    }

    /**
     * The drawings deck always has a row, even before there is anything in it — a
     * locked stage the child can ask about is friendlier than a deck that silently
     * appears one day.
     */
    private void addDrawings(List<Stage> stages) {
        int index = MemoryCatalog.STATIC_COUNT;
        if (MemoryCatalog.hasDrawings()) {
            addDeck(stages, index, MemoryCatalog.deck(index));
            return;
        }
        for (int level = 0; level < MemoryCatalog.levelCount(); level++) {
            stages.add(new Stage(index * 100 + level,
                    getString(R.string.memory_deck_drawings) + " — "
                            + getString(R.string.stage_level, FaNum.of(level + 1)),
                    getString(R.string.memory_drawings_empty_sub), "",
                    false, true, R.drawable.ic_gallery, null));
        }
    }

    private void addDeck(List<Stage> stages, int deckIndex, MemoryDeck deck) {
        int best = prefs.memoryProgress(deck.id);
        for (int level = 0; level < MemoryCatalog.levelCount(); level++) {
            int pairs = MemoryCatalog.pairsForLevel(level);
            // The drawings deck can be smaller than the biggest levels want, so a
            // level the child has not drawn enough for stays shut.
            boolean tooFewCards = pairs > deck.size();
            stages.add(new Stage(deckIndex * 100 + level,
                    deck.name + " — " + getString(R.string.stage_level, FaNum.of(level + 1)),
                    getString(R.string.stage_pairs, FaNum.of(pairs)), "",
                    level <= best,
                    tooFewCards || level > best + 1,
                    deck.badge, null));
        }
    }

    @Override
    protected String lockedLine(Stage stage) {
        if (MemoryCatalog.isDrawings(stage.index / 100) && !MemoryCatalog.hasDrawings()) {
            return getString(R.string.memory_drawings_locked);
        }
        return super.lockedLine(stage);
    }

    @Override
    protected void onStageChosen(Stage stage) {
        Intent intent = new Intent(this, MemoryActivity.class);
        intent.putExtra(MemoryActivity.EXTRA_DECK, stage.index / 100);
        intent.putExtra(MemoryActivity.EXTRA_LEVEL, stage.index % 100);
        startActivity(intent);
    }
}
