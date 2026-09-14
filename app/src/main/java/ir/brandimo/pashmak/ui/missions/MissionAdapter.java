package ir.brandimo.pashmak.ui.missions;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.Mission;
import ir.brandimo.pashmak.databinding.ItemMissionBinding;
import ir.brandimo.pashmak.util.FaNum;

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.MissionHolder> {

    public interface OnMissionClick {
        void onMissionClick(Mission mission, int captured);
    }

    private final List<Mission> missions;
    private final OnMissionClick listener;
    private Map<Integer, Integer> progress = new HashMap<>();
    private int currentMission;

    public MissionAdapter(List<Mission> missions, OnMissionClick listener) {
        this.missions = missions;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setProgress(Map<Integer, Integer> next, int current) {
        progress = next == null ? new HashMap<>() : next;
        currentMission = current;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return missions.get(position).index;
    }

    @NonNull
    @Override
    public MissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMissionBinding binding = ItemMissionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MissionHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MissionHolder holder, int position) {
        Mission mission = missions.get(position);
        int captured = MissionsViewModel.countFor(progress, mission.index);
        holder.bind(mission, captured, mission.index == currentMission, listener);
    }

    @Override
    public int getItemCount() {
        return missions.size();
    }

    static class MissionHolder extends RecyclerView.ViewHolder {

        private final ItemMissionBinding binding;

        MissionHolder(ItemMissionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Mission mission, int captured, boolean isCurrent, OnMissionClick listener) {
            boolean complete = mission.isComplete(captured);
            binding.missionNumber.setText(FaNum.of(mission.index + 1));
            binding.missionTitle.setText(mission.title);
            binding.missionItems.setText(mission.itemsLine());
            binding.missionProgress.setText(
                    FaNum.of(captured) + "/" + FaNum.of(mission.size()));

            int chipColor = complete ? R.color.green
                    : (isCurrent ? R.color.blue_light : R.color.blue_chip_idle);
            tintChip(chipColor);

            binding.missionProgress.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    complete ? R.color.green : R.color.blue_progress_idle));

            binding.missionRow.setBackgroundResource(
                    isCurrent ? R.drawable.bg_card_current : R.drawable.bg_card_white);

            binding.missionRow.setOnClickListener(v -> listener.onMissionClick(mission, captured));
        }

        private void tintChip(int colorRes) {
            GradientDrawable chip = new GradientDrawable();
            chip.setCornerRadius(14f * binding.getRoot().getResources().getDisplayMetrics().density);
            chip.setColor(ContextCompat.getColor(binding.getRoot().getContext(), colorRes));
            binding.missionNumber.setBackground(chip);
        }
    }
}
