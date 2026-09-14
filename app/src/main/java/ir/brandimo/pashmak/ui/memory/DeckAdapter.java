package ir.brandimo.pashmak.ui.memory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.MemoryDeck;
import ir.brandimo.pashmak.databinding.ItemChipBinding;

/** The row of deck chips beside the board. */
public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckHolder> {

    public interface OnDeckClick {
        void onDeckClick(int index);
    }

    private final List<MemoryDeck> decks;
    private final OnDeckClick listener;
    private int selected;

    public DeckAdapter(List<MemoryDeck> decks, OnDeckClick listener) {
        this.decks = decks;
        this.listener = listener;
    }

    public void setSelected(int index) {
        selected = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeckHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DeckHolder(ItemChipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DeckHolder holder, int position) {
        holder.bind(decks.get(position), position == selected, position, listener);
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    static class DeckHolder extends RecyclerView.ViewHolder {

        private final ItemChipBinding binding;

        DeckHolder(ItemChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MemoryDeck deck, boolean isSelected, int position, OnDeckClick listener) {
            binding.chipLabel.setText(deck.name);
            binding.chipIcon.setVisibility(View.VISIBLE);
            binding.chipIcon.setImageResource(deck.badge);
            binding.chipLock.setVisibility(View.GONE);
            binding.chipRoot.setSelected(isSelected);
            binding.chipLabel.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    isSelected ? R.color.white : R.color.purple));
            binding.chipRoot.setOnClickListener(v -> listener.onDeckClick(position));
        }
    }
}
