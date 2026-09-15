package ir.brandimo.pashmak.ui.memory;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.catalog.MemoryCatalog;
import ir.brandimo.pashmak.data.repo.MissionRepository;
import ir.brandimo.pashmak.databinding.ActivityMemoryBinding;
import ir.brandimo.pashmak.ui.base.GameActivity;
import ir.brandimo.pashmak.util.FaNum;

/**
 * One memory board, chosen on the stage picker: a themed deck at one level, from
 * three pairs up to eight. The board is measured and laid out to fit the screen,
 * so the cards stay square instead of stretching.
 */
public class MemoryActivity extends GameActivity {

    public static final String EXTRA_DECK = "deck_index";
    public static final String EXTRA_LEVEL = "level_index";

    private static final long MATCH_PAUSE_MS = 460L;
    private static final long MISS_PAUSE_MS = 800L;

    private ActivityMemoryBinding binding;
    private MemoryViewModel viewModel;
    private MemoryAdapter adapter;

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
            dealFromIntent();
        } else {
            showStageName();
        }
    }

    /**
     * The drawings deck is built at runtime, so after the process has been killed and
     * restored it may not be registered yet. Load it before dealing, and fall back to
     * the first deck if the drawings are gone.
     */
    private void dealFromIntent() {
        int deckIndex = getIntent().getIntExtra(EXTRA_DECK, 0);
        int level = getIntent().getIntExtra(EXTRA_LEVEL, 0);
        if (!MemoryCatalog.isDrawings(deckIndex) || MemoryCatalog.hasDrawings()) {
            deal(deckIndex, level);
            return;
        }
        MissionRepository.get(this).loadDrawingDeck(available ->
                deal(available ? deckIndex : 0, level));
    }

    private void deal(int deckIndex, int level) {
        viewModel.deal(deckIndex, level);
        showStageName();
    }

    private void showStageName() {
        binding.memoryStage.setText(getString(R.string.memory_stage_name,
                viewModel.deck().name, FaNum.of(viewModel.level() + 1)));
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
                onCorrect(getString(R.string.memory_match), AudioManifest.VOICE_MEMORY_MATCH);
                binding.getRoot().postDelayed(() -> viewModel.settle(true), MATCH_PAUSE_MS);
                break;
            case MISS:
                onWrong(getString(R.string.memory_miss), AudioManifest.VOICE_MEMORY_MISS);
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
        onCorrect(getString(R.string.memory_win), AudioManifest.VOICE_MEMORY_WIN);
        if (viewModel.hasNextLevel()) {
            int next = viewModel.level() + 1;
            binding.getRoot().postDelayed(() -> {
                mascot.say(getString(R.string.memory_next_level),
                        ir.brandimo.pashmak.mascot.MascotState.CHEER,
                        ir.brandimo.pashmak.mascot.MascotController.HOLD_MIN_MS,
                        AudioManifest.VOICE_MEMORY_NEXT);
                deal(viewModel.deckIndex(), next);
            }, 2400L);
        }
    }
}
