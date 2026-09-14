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
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.GameActivity;
import ir.brandimo.pashmak.ui.common.PaintCanvasView;

/**
 * A blank page and a box of pencils. Nothing in the tool column is written down —
 * the colours are pencils, the tools are drawn, and each thickness shows the line
 * it will actually make.
 */
public class FreeDrawActivity extends GameActivity {

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
        shrinkCompanion();

        binding.drawPens.setColors(Palette.BRUSH_COLORS);
        binding.drawPens.setSelected(Palette.ORANGE);
        binding.drawCanvas.setBrushColor(Palette.ORANGE);
        binding.drawPens.setOnPenPicked(color -> {
            tap();
            binding.drawCanvas.setBrushColor(color);
            // Picking a pencil means the child is drawing again, not erasing.
            if (binding.drawCanvas.tool().erases()) {
                selectTool(PaintCanvasView.Tool.PENCIL);
            } else {
                binding.drawWidths.refresh();
            }
        });

        binding.drawWidths.mirror(binding.drawCanvas);
        binding.drawWidths.setOnWidthPicked(widthDp -> {
            tap();
            binding.drawCanvas.setBrushWidthDp(widthDp);
            binding.drawWidths.refresh();
        });

        wireTools();
        binding.drawCanvas.setOnStrokeListener(() -> sounds.play(AudioManifest.SFX_BRUSH));
        binding.drawSave.setOnClickListener(v -> save());
    }

    private void wireTools() {
        binding.drawToolPencil.setOnClickListener(
                v -> selectTool(PaintCanvasView.Tool.PENCIL));
        binding.drawToolMarker.setOnClickListener(
                v -> selectTool(PaintCanvasView.Tool.MARKER));
        binding.drawToolCrayon.setOnClickListener(
                v -> selectTool(PaintCanvasView.Tool.CRAYON));
        binding.drawToolEraser.setOnClickListener(
                v -> selectTool(PaintCanvasView.Tool.ERASER));
        selectTool(PaintCanvasView.Tool.PENCIL);
    }

    private void selectTool(PaintCanvasView.Tool tool) {
        tap();
        binding.drawCanvas.setTool(tool);
        binding.drawToolPencil.setSelected(tool == PaintCanvasView.Tool.PENCIL);
        binding.drawToolMarker.setSelected(tool == PaintCanvasView.Tool.MARKER);
        binding.drawToolCrayon.setSelected(tool == PaintCanvasView.Tool.CRAYON);
        binding.drawToolEraser.setSelected(tool == PaintCanvasView.Tool.ERASER);
        // The thickness samples show the held tool, so they change with it.
        binding.drawWidths.refresh();
        binding.drawPens.setAlpha(tool.erases() ? 0.5f : 1f);
    }

    private void save() {
        tap();
        if (!binding.drawCanvas.isDirty()) {
            mascot.say(getString(R.string.freedraw_empty), MascotState.TALK,
                    MascotController.HOLD_MIN_MS);
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
