package ir.brandimo.pashmak.ui.memory;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.catalog.MemoryCatalog;
import ir.brandimo.pashmak.databinding.ActivityMemoryBinding;
import ir.brandimo.pashmak.databinding.ItemChipBinding;
import ir.brandimo.pashmak.ui.base.GameActivity;
import ir.brandimo.pashmak.util.FaNum;

/**
 * Memory, as a progression: six themed decks and five levels each, from three
 * pairs up to eight. The board is measured and laid out to fit the screen, so the
 * cards stay square instead of stretching.
 */
public class MemoryActivity extends GameActivity {

    private static final long MATCH_PAUSE_MS = 460L;
    private static final long MISS_PAUSE_MS = 800L;

    private ActivityMemoryBinding binding;
    private MemoryViewModel viewModel;
    private MemoryAdapter adapter;
    private DeckAdapter deckAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(MemoryViewModel.class);

        binding.memoryHeader.headerTitle.setText(R.string.memory_title);
        binding.memoryHeader.headerTitle.setTextColor(ContextCompat.getColor(this, R.color.red));
        binding.memoryHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        binding.memoryHeader.headerAction.setVisibility(View.VISIBLE);
        binding.memoryHeader.headerAction.setText(R.string.memory_again);
        binding.memoryHeader.headerAction.setTextColor(ContextCompat.getColor(this, R.color.red));
        binding.memoryHeader.headerAction.setOnClickListener(v -> {
            tap();
            deal(viewModel.deckIndex(), viewModel.level());
        });
        bindStars(binding.memoryHeader.headerStarsValue);
        attachCompanion();

        adapter = new MemoryAdapter(this::onCardClick);
        binding.memoryBoard.setLayoutManager(new GridLayoutManager(this, 4));
        binding.memoryBoard.setAdapter(adapter);
        binding.memoryBoard.setItemAnimator(null);

        deckAdapter = new DeckAdapter(MemoryCatalog.all(), index -> {
            tap();
            deal(index, 0);
        });
        binding.memoryDecks.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.memoryDecks.setAdapter(deckAdapter);

        viewModel.cards().observe(this, cards -> {
            adapter.setDeck(viewModel.deck());
            adapter.setCards(cards);
            layoutBoard();
        });
        viewModel.states().observe(this, adapter::setStates);
        viewModel.matchedPairs().observe(this, pairs -> binding.memoryCounter.setText(
                getString(R.string.memory_counter,
                        FaNum.of(pairs == null ? 0 : pairs),
                        FaNum.of(viewModel.pairCount()))));
        viewModel.finished().observe(this, done -> {
            if (Boolean.TRUE.equals(done)) {
                onLevelFinished();
            }
        });

        if (viewModel.cards().getValue() == null || viewModel.cards().getValue().isEmpty()) {
            deal(0, 0);
        } else {
            renderChips();
        }
    }

    private void deal(int deckIndex, int level) {
        viewModel.deal(deckIndex, level);
        deckAdapter.setSelected(deckIndex);
        renderChips();
    }

    /** Level chips: everything up to one past the child's best is playable. */
    private void renderChips() {
        LinearLayout container = binding.memoryLevels;
        container.removeAllViews();
        int best = prefs.memoryProgress(viewModel.deck().id);
        for (int level = 0; level < MemoryCatalog.levelCount(); level++) {
            final int index = level;
            boolean unlocked = level <= best + 1;
            ItemChipBinding chip = ItemChipBinding.inflate(getLayoutInflater(), container, false);
            chip.chipLabel.setText(FaNum.of(level + 1));
            chip.chipLock.setVisibility(unlocked ? View.GONE : View.VISIBLE);
            chip.chipRoot.setSelected(level == viewModel.level());
            chip.chipLabel.setTextColor(ContextCompat.getColor(this,
                    level == viewModel.level() ? R.color.white : R.color.purple));
            chip.chipRoot.setOnClickListener(v -> {
                tap();
                if (unlocked) {
                    deal(viewModel.deckIndex(), index);
                } else {
                    mascot.say(getString(R.string.memory_locked),
                            ir.brandimo.pashmak.mascot.MascotState.TALK,
                            ir.brandimo.pashmak.mascot.MascotController.HOLD_MIN_MS);
                }
            });
            container.addView(chip.getRoot());
        }
    }

    /**
     * Chooses the column count that gets closest to square cards for this board
     * and this screen, then sizes the rows so everything fits without scrolling.
     */
    private void layoutBoard() {
        binding.memoryBoard.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        binding.memoryBoard.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        binding.memoryBoard.post(MemoryActivity.this::applyBoardMetrics);
                    }
                });
        binding.memoryBoard.requestLayout();
    }

    private void applyBoardMetrics() {
        int count = viewModel.pairCount() * 2;
        int width = binding.memoryBoard.getWidth();
        int height = binding.memoryBoard.getHeight();
        if (count == 0 || width == 0 || height == 0) {
            return;
        }
        int bestColumns = 2;
        float bestScore = Float.MAX_VALUE;
        for (int columns = 2; columns <= count; columns++) {
            int rows = (int) Math.ceil(count / (float) columns);
            float cardWidth = width / (float) columns;
            float cardHeight = height / (float) rows;
            // Prefer the arrangement whose cards are closest to square.
            float score = Math.abs(cardWidth / cardHeight - 1f);
            if (score < bestScore) {
                bestScore = score;
                bestColumns = columns;
            }
        }
        int rows = (int) Math.ceil(count / (float) bestColumns);
        adapter.setCardHeight(height / rows);
        binding.memoryBoard.setLayoutManager(new GridLayoutManager(this, bestColumns));
        adapter.notifyDataSetChanged();
    }

    private void onCardClick(int position) {
        switch (viewModel.flip(position)) {
            case FLIPPED:
                sounds.play(AudioManifest.SFX_FLIP);
                break;
            case MATCH:
                onCorrect(getString(R.string.memory_match));
                binding.getRoot().postDelayed(() -> viewModel.settle(true), MATCH_PAUSE_MS);
                break;
            case MISS:
                onWrong(getString(R.string.memory_miss));
                binding.getRoot().postDelayed(() -> viewModel.settle(false), MISS_PAUSE_MS);
                break;
            case IGNORED:
            default:
                break;
        }
    }

    private void onLevelFinished() {
        sounds.play(AudioManifest.SFX_FANFARE);
        prefs.setMemoryProgress(viewModel.deck().id, viewModel.level());
        mascot.addStars(viewModel.pairCount());
        onCorrect(getString(R.string.memory_win));
        renderChips();
        if (viewModel.hasNextLevel()) {
            int next = viewModel.level() + 1;
            binding.getRoot().postDelayed(() -> {
                mascot.say(getString(R.string.memory_next_level),
                        ir.brandimo.pashmak.mascot.MascotState.CHEER,
                        ir.brandimo.pashmak.mascot.MascotController.HOLD_MIN_MS);
                deal(viewModel.deckIndex(), next);
            }, 2400L);
        }
    }
}
