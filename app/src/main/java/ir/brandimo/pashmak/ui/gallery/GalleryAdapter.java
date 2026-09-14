package ir.brandimo.pashmak.ui.gallery;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.db.GalleryEntry;
import ir.brandimo.pashmak.databinding.ItemGalleryBinding;
import ir.brandimo.pashmak.util.FaNum;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.EntryHolder> {

    public interface OnEntryClick {
        void onEntryClick(GalleryEntry entry);
    }

    private final OnEntryClick listener;
    private List<GalleryEntry> entries = new ArrayList<>();

    public GalleryAdapter(OnEntryClick listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setEntries(List<GalleryEntry> next) {
        entries = next == null ? new ArrayList<>() : next;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return entries.get(position).id;
    }

    @NonNull
    @Override
    public EntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EntryHolder(ItemGalleryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EntryHolder holder, int position) {
        holder.bind(entries.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class EntryHolder extends RecyclerView.ViewHolder {

        private final ItemGalleryBinding binding;

        EntryHolder(ItemGalleryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GalleryEntry entry, OnEntryClick listener) {
            binding.galleryItemTitle.setText(entry.title);
            binding.galleryItemSub.setText(subtitleFor(entry));
            Glide.with(binding.galleryThumb)
                    .load(new File(entry.filePath))
                    .centerCrop()
                    .into(binding.galleryThumb);
            binding.galleryCard.setOnClickListener(v -> listener.onEntryClick(entry));
        }

        private String subtitleFor(GalleryEntry entry) {
            switch (entry.kind) {
                case GalleryEntry.KIND_FREE_DRAW:
                    return binding.getRoot().getContext()
                            .getString(R.string.gallery_freedraw_label);
                case GalleryEntry.KIND_COLORING:
                    return binding.getRoot().getContext()
                            .getString(R.string.gallery_coloring_label);
                case GalleryEntry.KIND_MISSION_SCENE:
                default:
                    return binding.getRoot().getContext().getString(
                            R.string.gallery_mission_label,
                            FaNum.of(entry.missionIndex + 1));
            }
        }
    }
}
