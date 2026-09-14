package ir.brandimo.pashmak.ui.memory;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.prefs.GamePrefs;
import ir.brandimo.pashmak.databinding.ActivityMemoryBinding;
import ir.brandimo.pashmak.ui.base.GameActivity;
import ir.brandimo.pashmak.util.FaNum;

/** Find the pairs. Matching earns a star; missing earns encouragement. */
public class MemoryActivity extends GameActivity {

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
            viewModel.reset(pairsForDifficulty());
        });
        bindStars(binding.memoryHeader.headerStarsValue);
        attachCompanion();

        adapter = new MemoryAdapter(this::onCardClick);
        binding.memoryBoard.setLayoutManager(new GridLayoutManager(
                this, getResources().getInteger(R.integer.memory_span)));
        binding.memoryBoard.setAdapter(adapter);
        binding.memoryBoard.setItemAnimator(null);

        viewModel.cards().observe(this, adapter::setCards);
        viewModel.states().observe(this, adapter::setStates);
        viewModel.matchedPairs().observe(this, pairs -> binding.memoryCounter.setText(
                getString(R.string.memory_counter,
                        FaNum.of(pairs == null ? 0 : pairs),
                        FaNum.of(viewModel.pairCount()))));
        viewModel.finished().observe(this, done -> {
            if (Boolean.TRUE.equals(done)) {
                sounds.play(AudioManifest.SFX_FANFARE);
                onCorrect(getString(R.string.memory_win));
            }
        });

        if (viewModel.cards().getValue() == null || viewModel.cards().getValue().isEmpty()) {
            viewModel.reset(pairsForDifficulty());
        }
    }

    private int pairsForDifficulty() {
        switch (prefs.difficulty()) {
            case GamePrefs.DIFFICULTY_EASY:
                return 4;
            case GamePrefs.DIFFICULTY_HARD:
                return 8;
            case GamePrefs.DIFFICULTY_MEDIUM:
            default:
                return 6;
        }
    }

    private void onCardClick(int position) {
        MemoryViewModel.Outcome outcome = viewModel.flip(position);
        switch (outcome) {
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
}
