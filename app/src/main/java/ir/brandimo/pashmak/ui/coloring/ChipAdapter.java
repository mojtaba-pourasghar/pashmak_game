package ir.brandimo.pashmak.ui.coloring;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.databinding.ItemChipBinding;

/** A simple row of labelled chips that can be selected, locked, or ticked off. */
public class ChipAdapter extends RecyclerView.Adapter<ChipAdapter.ChipHolder> {

    /** One entry in the row. */
    public static final class Entry {
        public final String label;
        public final boolean locked;
        public final boolean done;

        public Entry(String label, boolean locked, boolean done) {
            this.label = label;
            this.locked = locked;
            this.done = done;
        }
    }

    public interface OnChipClick {
        void onChipClick(int index, boolean locked);
    }

    private final OnChipClick listener;
    private List<Entry> entries = new ArrayList<>();
    private int selected;

    public ChipAdapter(OnChipClick listener) {
        this.listener = listener;
    }

    public void submit(List<Entry> next, int selectedIndex) {
        entries = next == null ? new ArrayList<>() : next;
        selected = selectedIndex;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChipHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChipHolder(ItemChipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChipHolder holder, int position) {
        holder.bind(entries.get(position), position == selected, position, listener);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ChipHolder extends RecyclerView.ViewHolder {

        private final ItemChipBinding binding;

        ChipHolder(ItemChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Entry entry, boolean isSelected, int position, OnChipClick listener) {
            binding.chipLabel.setText(entry.label);
            binding.chipRoot.setSelected(isSelected);
            binding.chipLock.setVisibility(entry.locked ? View.VISIBLE : View.GONE);
            // A finished page keeps a green tick so progress is visible at a glance.
            binding.chipIcon.setVisibility(entry.done ? View.VISIBLE : View.GONE);
            if (entry.done) {
                binding.chipIcon.setImageResource(R.drawable.ic_check);
                binding.chipIcon.setColorFilter(ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.green));
            }
            binding.chipRoot.setAlpha(entry.locked ? 0.55f : 1f);
            binding.chipLabel.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    isSelected ? R.color.white : R.color.purple));
            binding.chipRoot.setOnClickListener(
                    v -> listener.onChipClick(position, entry.locked));
        }
    }
}
