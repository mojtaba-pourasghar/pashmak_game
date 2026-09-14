package ir.brandimo.pashmak.ui.story;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.Story;
import ir.brandimo.pashmak.databinding.ItemChipBinding;

/** The picker strip: one chip per story. */
public class StoryListAdapter extends RecyclerView.Adapter<StoryListAdapter.StoryHolder> {

    public interface OnStoryClick {
        void onStoryClick(int index);
    }

    private final List<Story> stories;
    private final OnStoryClick listener;
    private int selected;

    public StoryListAdapter(List<Story> stories, OnStoryClick listener) {
        this.stories = stories;
        this.listener = listener;
    }

    public void setSelected(int index) {
        selected = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StoryHolder(ItemChipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull StoryHolder holder, int position) {
        holder.bind(stories.get(position), position == selected, position, listener);
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    static class StoryHolder extends RecyclerView.ViewHolder {

        private final ItemChipBinding binding;

        StoryHolder(ItemChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Story story, boolean isSelected, int position, OnStoryClick listener) {
            binding.chipLabel.setText(story.title);
            binding.chipIcon.setVisibility(View.VISIBLE);
            binding.chipIcon.setImageResource(story.badge);
            binding.chipLock.setVisibility(View.GONE);
            binding.chipRoot.setSelected(isSelected);
            binding.chipLabel.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    isSelected ? R.color.white : R.color.purple));
            binding.chipRoot.setOnClickListener(v -> listener.onStoryClick(position));
        }
    }
}
