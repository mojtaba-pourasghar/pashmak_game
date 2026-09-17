package ir.brandimo.pashmak.ui.gallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.db.GalleryEntry;
import ir.brandimo.pashmak.data.repo.GalleryRepository;
import ir.brandimo.pashmak.databinding.ActivityGalleryBinding;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.util.FaNum;

/** Everything the child has finished, newest first. */
public class GalleryActivity extends BaseActivity {

    /** A gallery cell needs this much for a square thumbnail and its caption. */
    private static final int COLUMN_MIN_DP = 96;


    private ActivityGalleryBinding binding;
    private GalleryAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGalleryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.galleryHeader.headerTitle.setText(R.string.gallery_title);
        binding.galleryHeader.headerTitle.setTextColor(
                ContextCompat.getColor(this, R.color.purple));
        binding.galleryHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        bindStars(binding.galleryHeader.headerStarsValue);
        attachCompanion();

        adapter = new GalleryAdapter(this::openEntry);
        gridColumns(binding.galleryGrid,
                getResources().getInteger(R.integer.gallery_span), COLUMN_MIN_DP);
        binding.galleryGrid.setAdapter(adapter);

        GalleryRepository.get(this).observeAll().observe(this, entries -> {
            adapter.setEntries(entries);
            boolean empty = entries == null || entries.isEmpty();
            binding.galleryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.galleryCount.setText(getString(R.string.gallery_count,
                    FaNum.of(entries == null ? 0 : entries.size())));
        });
    }

    private void openEntry(GalleryEntry entry) {
        tap();
        Intent intent = new Intent(this, GalleryViewerActivity.class);
        intent.putExtra(GalleryViewerActivity.EXTRA_ID, entry.id);
        intent.putExtra(GalleryViewerActivity.EXTRA_PATH, entry.filePath);
        intent.putExtra(GalleryViewerActivity.EXTRA_TITLE, entry.title);
        startActivity(intent);
    }
}
