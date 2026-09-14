package ir.brandimo.pashmak.ui.lullaby;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import ir.brandimo.pashmak.data.catalog.Lullaby;
import ir.brandimo.pashmak.databinding.ItemLullabyBinding;
import ir.brandimo.pashmak.util.FaNum;

/** The bedtime playlist. The row that is sounding keeps a lit border and a mark. */
public class LullabyAdapter extends RecyclerView.Adapter<LullabyAdapter.LullabyHolder> {

    public interface OnLullabyPicked {
        void onLullabyPicked(int index);
    }

    private final List<Lullaby> items;
    private final OnLullabyPicked callback;

    private int current = -1;
    private boolean playing;

    public LullabyAdapter(@NonNull List<Lullaby> items, @NonNull OnLullabyPicked callback) {
        this.items = items;
        this.callback = callback;
    }

    /** Moves the highlight, repainting only the two rows that changed. */
    public void setCurrent(int index, boolean isPlaying) {
        int previous = current;
        current = index;
        playing = isPlaying;
        if (previous >= 0 && previous < items.size()) {
            notifyItemChanged(previous);
        }
        if (current >= 0 && current < items.size()) {
            notifyItemChanged(current);
        }
    }

    @NonNull
    @Override
    public LullabyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LullabyHolder(ItemLullabyBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull LullabyHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class LullabyHolder extends RecyclerView.ViewHolder {

        private final ItemLullabyBinding binding;

        LullabyHolder(ItemLullabyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Lullaby lullaby, int position) {
            binding.lullabyTitle.setText(lullaby.title);
            binding.lullabyLine.setText(lullaby.line);
            binding.lullabyIcon.setImageResource(lullaby.icon);
            binding.lullabyLength.setText(clock(lullaby.seconds));
            boolean isCurrent = position == current;
            binding.lullabyRow.setSelected(isCurrent);
            binding.lullabyPlaying.setVisibility(
                    isCurrent && playing ? View.VISIBLE : View.INVISIBLE);
            binding.lullabyRow.setOnClickListener(v -> callback.onLullabyPicked(position));
        }

        private String clock(int seconds) {
            return FaNum.of(seconds / 60) + ":" + FaNum.of(String.format(Locale.US, "%02d", seconds % 60));
        }
    }
}
