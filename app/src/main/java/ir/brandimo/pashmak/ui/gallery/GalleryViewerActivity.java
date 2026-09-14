package ir.brandimo.pashmak.ui.gallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.repo.GalleryRepository;
import ir.brandimo.pashmak.databinding.ActivityGalleryViewerBinding;
import ir.brandimo.pashmak.ui.base.BaseActivity;

/** One picture, full screen, with the three things a parent might want to do. */
public class GalleryViewerActivity extends BaseActivity {

    public static final String EXTRA_ID = "entry_id";
    public static final String EXTRA_PATH = "entry_path";
    public static final String EXTRA_TITLE = "entry_title";

    private ActivityGalleryViewerBinding binding;
    private long entryId;
    @Nullable
    private String path;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGalleryViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        entryId = getIntent().getLongExtra(EXTRA_ID, -1L);
        path = getIntent().getStringExtra(EXTRA_PATH);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        binding.viewerTitle.setText(title == null ? "" : title);
        if (path != null) {
            Glide.with(this).load(new File(path)).fitCenter().into(binding.viewerImage);
        }

        binding.viewerClose.setOnClickListener(v -> {
            tap();
            finish();
        });
        binding.viewerExport.setOnClickListener(v -> exportToDeviceGallery());
        binding.viewerShare.setOnClickListener(v -> share());
        binding.viewerDelete.setOnClickListener(v -> confirmDelete());
    }

    private void exportToDeviceGallery() {
        tap();
        GalleryRepository.get(this).exportToDeviceGallery(entryId, uri ->
                Toast.makeText(this,
                        uri != null ? R.string.gallery_exported : R.string.gallery_export_failed,
                        Toast.LENGTH_LONG).show());
    }

    private void share() {
        tap();
        if (path == null) {
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", new File(path));
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.gallery_share)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.gallery_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        tap();
        new AlertDialog.Builder(this)
                .setMessage(R.string.gallery_delete_confirm)
                .setNegativeButton(R.string.back, null)
                .setPositiveButton(R.string.gallery_delete, (dialog, which) ->
                        GalleryRepository.get(this).delete(entryId, () -> {
                            Toast.makeText(this, R.string.gallery_deleted,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }))
                .show();
    }
}
