package ir.brandimo.pashmak.ui.games;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.databinding.ViewGameCardBinding;

/** Lays out the game tiles; the grid does the placing, this just fills them in. */
public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameHolder> {

    public interface OnGameClick {
        void onGameClick(GameEntry entry);
    }

    private final List<GameEntry> entries;
    private final OnGameClick listener;

    public GameAdapter(List<GameEntry> entries, OnGameClick listener) {
        this.entries = entries;
        this.listener = listener;
    }

    public GameEntry entryAt(int position) {
        return entries.get(position);
    }

    @NonNull
    @Override
    public GameHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GameHolder(ViewGameCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GameHolder holder, int position) {
        holder.bind(entries.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class GameHolder extends RecyclerView.ViewHolder {

        private final ViewGameCardBinding binding;

        GameHolder(ViewGameCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GameEntry entry, OnGameClick listener) {
            binding.cardRoot.setBackgroundResource(entry.background);
            binding.cardRoot.setMinimumHeight(binding.getRoot().getResources()
                    .getDimensionPixelSize(R.dimen.card_min_height));
            binding.cardIcon.setImageResource(entry.icon);
            binding.cardIcon.setColorFilter(ContextCompat.getColor(
                    binding.getRoot().getContext(), entry.iconTint));
            binding.cardTitle.setText(entry.title);
            binding.cardSub.setText(entry.subtitle);

            int ink = ContextCompat.getColor(binding.getRoot().getContext(),
                    entry.darkText ? R.color.gold_ink : R.color.white);
            binding.cardTitle.setTextColor(ink);
            binding.cardSub.setTextColor(ink);
            binding.cardSub.setAlpha(entry.darkText ? 0.8f : 0.9f);

            binding.cardRoot.setOnClickListener(v -> listener.onGameClick(entry));
        }
    }
}
