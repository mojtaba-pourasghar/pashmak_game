package ir.brandimo.pashmak.ui.freedraw;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.catalog.Palette;
import ir.brandimo.pashmak.data.db.GalleryEntry;
import ir.brandimo.pashmak.data.repo.GalleryRepository;
import ir.brandimo.pashmak.databinding.ActivityFreeDrawBinding;
import ir.brandimo.pashmak.ui.base.GameActivity;

/** A blank page and a box of colors. Anything the child makes can be kept. */
public class FreeDrawActivity extends GameActivity {

    private static final float BRUSH_THIN = 8f;
    private static final float BRUSH_THICK = 26f;

    private ActivityFreeDrawBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFreeDrawBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.drawHeader.headerTitle.setText(R.string.freedraw_title);
        binding.drawHeader.headerTitle.setTextColor(ContextCompat.getColor(this, R.color.cyan));
        binding.drawHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        binding.drawHeader.headerAction.setVisibility(View.VISIBLE);
        binding.drawHeader.headerAction.setText(R.string.freedraw_clear);
        binding.drawHeader.headerAction.setTextColor(ContextCompat.getColor(this, R.color.cyan));
        binding.drawHeader.headerAction.setOnClickListener(v -> {
            tap();
            binding.drawCanvas.clear();
        });
        bindStars(binding.drawHeader.headerStarsValue);
        attachCompanion();

        binding.drawPalette.setColors(Palette.BRUSH_COLORS);
        binding.drawPalette.setSelected(Palette.ORANGE);
        binding.drawCanvas.setBrushColor(Palette.ORANGE);
        binding.drawPalette.setOnColorPicked(color -> binding.drawCanvas.setBrushColor(color));

        binding.drawThin.setOnClickListener(v -> {
            tap();
            binding.drawCanvas.setBrushWidthDp(BRUSH_THIN);
        });
        binding.drawThick.setOnClickListener(v -> {
            tap();
            binding.drawCanvas.setBrushWidthDp(BRUSH_THICK);
        });
        binding.drawCanvas.setOnStrokeListener(() -> sounds.play(AudioManifest.SFX_BRUSH));
        binding.drawSave.setOnClickListener(v -> save());
    }

    private void save() {
        tap();
        if (!binding.drawCanvas.isDirty()) {
            mascot.say(getString(R.string.freedraw_empty),
                    ir.brandimo.pashmak.mascot.MascotState.TALK,
                    ir.brandimo.pashmak.mascot.MascotController.HOLD_MIN_MS);
            return;
        }
        Bitmap bitmap = binding.drawCanvas.snapshot(Color.WHITE);
        if (bitmap == null) {
            return;
        }
        GalleryRepository.get(this).save(GalleryEntry.KIND_FREE_DRAW, -1,
                getString(R.string.gallery_freedraw_label), bitmap, success -> {
                    if (success) {
                        Toast.makeText(this, R.string.freedraw_saved, Toast.LENGTH_SHORT).show();
                        onCorrect();
                    } else {
                        Toast.makeText(this, R.string.gallery_export_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
