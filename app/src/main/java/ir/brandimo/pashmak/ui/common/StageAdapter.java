package ir.brandimo.pashmak.ui.common;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.databinding.ItemStageBinding;

/** The stage list every game is chosen from. */
public class StageAdapter extends RecyclerView.Adapter<StageAdapter.StageHolder> {

    public interface OnStageClick {
        void onStageClick(Stage stage);
    }

    private final OnStageClick listener;
    @ColorRes
    private final int accent;
    private List<Stage> stages = new ArrayList<>();

    public StageAdapter(@ColorRes int accent, OnStageClick listener) {
        this.accent = accent;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(List<Stage> next) {
        stages = next == null ? new ArrayList<>() : next;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return stages.get(position).index;
    }

    @NonNull
    @Override
    public StageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StageHolder(ItemStageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull StageHolder holder, int position) {
        holder.bind(stages.get(position), accent, listener);
    }

    @Override
    public int getItemCount() {
        return stages.size();
    }

    static class StageHolder extends RecyclerView.ViewHolder {

        private final ItemStageBinding binding;

        StageHolder(ItemStageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Stage stage, @ColorRes int accent, OnStageClick listener) {
            binding.stageTitle.setText(stage.title);
            binding.stageSubtitle.setText(stage.subtitle);
            binding.stageSubtitle.setVisibility(
                    stage.subtitle.isEmpty() ? View.GONE : View.VISIBLE);
            binding.stageProgress.setText(stage.progress);
            binding.stageProgress.setVisibility(
                    stage.progress.isEmpty() ? View.GONE : View.VISIBLE);
            binding.stageLock.setVisibility(stage.locked ? View.VISIBLE : View.GONE);

            boolean usesIcon = stage.icon != 0;
            binding.stageIcon.setVisibility(usesIcon ? View.VISIBLE : View.GONE);
            binding.stageBadge.setVisibility(usesIcon ? View.GONE : View.VISIBLE);
            if (usesIcon) {
                binding.stageIcon.setImageResource(stage.icon);
            } else {
                binding.stageBadge.setText(stage.badge == null ? "" : stage.badge);
            }

            int chipColor = stage.complete ? R.color.green
                    : (stage.locked ? R.color.blue_chip_idle : accent);
            GradientDrawable chip = new GradientDrawable();
            float density = binding.getRoot().getResources().getDisplayMetrics().density;
            chip.setCornerRadius(14f * density);
            chip.setColor(ContextCompat.getColor(binding.getRoot().getContext(), chipColor));
            binding.stageChip.setBackground(chip);

            binding.stageProgress.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    stage.complete ? R.color.green : R.color.blue_progress_idle));

            // A locked stage still reads clearly; it is just visibly out of reach.
            binding.stageRow.setAlpha(stage.locked ? 0.55f : 1f);
            binding.stageRow.setOnClickListener(v -> listener.onStageClick(stage));
        }
    }
}
