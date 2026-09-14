package ir.brandimo.pashmak.ui.coloring;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.ColorPack;
import ir.brandimo.pashmak.data.catalog.ColorRegion;
import ir.brandimo.pashmak.data.catalog.ColoringCatalog;
import ir.brandimo.pashmak.data.catalog.ColoringPage;
import ir.brandimo.pashmak.data.catalog.Palette;
import ir.brandimo.pashmak.data.db.GalleryEntry;
import ir.brandimo.pashmak.data.repo.GalleryRepository;
import ir.brandimo.pashmak.databinding.ActivityColoringBinding;
import ir.brandimo.pashmak.ui.base.GameActivity;
import ir.brandimo.pashmak.util.FaNum;
import ir.brandimo.pashmak.util.ViewCapture;

/** Five pictures, each with a right answer for every region. */
public class ColoringActivity extends GameActivity {

    private static final int STARS_PER_PAGE = 5;

    private ActivityColoringBinding binding;
    private ColoringViewModel viewModel;
    private ChipAdapter packAdapter;
    private ChipAdapter pageAdapter;
    private boolean awardedThisPage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityColoringBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(ColoringViewModel.class);

        binding.paintHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        binding.paintHeader.headerTitle.setTextColor(
                ContextCompat.getColor(this, R.color.purple));
        binding.paintHeader.headerAction.setVisibility(View.VISIBLE);
        binding.paintHeader.headerAction.setText(R.string.paint_clear);
        binding.paintHeader.headerAction.setTextColor(
                ContextCompat.getColor(this, R.color.purple));
        binding.paintHeader.headerAction.setOnClickListener(v -> {
            tap();
            awardedThisPage = false;
            viewModel.clearPage();
        });
        bindStars(binding.paintHeader.headerStarsValue);
        attachCompanion();

        binding.paintPalette.setColors(Palette.SWATCHES);
        binding.paintPalette.setOnColorPicked(color -> viewModel.selectColor(color));
        binding.paintCanvas.setOnRegionTapped(this::onRegionTapped);
        binding.paintSave.setOnClickListener(v -> saveToGallery());

        packAdapter = new ChipAdapter((index, locked) -> {
            tap();
            if (locked) {
                mascot.say(getString(R.string.paint_pack_locked),
                        ir.brandimo.pashmak.mascot.MascotState.TALK,
                        ir.brandimo.pashmak.mascot.MascotController.HOLD_MIN_MS);
            } else {
                viewModel.selectPack(index);
            }
        });
        binding.paintPacks.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.paintPacks.setAdapter(packAdapter);

        pageAdapter = new ChipAdapter((index, locked) -> {
            tap();
            viewModel.selectPage(index);
        });
        binding.paintPages.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.paintPages.setAdapter(pageAdapter);

        viewModel.packIndex().observe(this, index -> refreshChips());
        viewModel.pageIndex().observe(this, index -> {
            awardedThisPage = false;
            ColoringPage page = viewModel.page();
            binding.paintHeader.headerTitle.setText(getString(R.string.paint_title, page.name));
            binding.paintCanvas.setPage(page, viewModel.fills().getValue());
            buildLegend(page, viewModel.fills().getValue());
            refreshChips();
        });
        viewModel.fills().observe(this, fills -> {
            binding.paintCanvas.setFills(fills);
            buildLegend(viewModel.page(), fills);
            refreshBanner();
        });
        viewModel.selectedColor().observe(this, color ->
                binding.paintPalette.setSelected(color == null ? Palette.RED : color));
        viewModel.pageComplete().observe(this, complete -> {
            refreshBanner();
            if (Boolean.TRUE.equals(complete) && !awardedThisPage) {
                awardedThisPage = true;
                prefs.setColoringDone(viewModel.pageKey());
                mascot.addStars(STARS_PER_PAGE);
                onCorrect(getString(R.string.paint_all_right));
                refreshChips();
            }
        });

        viewModel.selectPack(0);
        mascot.help("paint");
    }

    /** A pack opens once the one before it is finished. */
    private boolean isPackUnlocked(int index) {
        if (index == 0) {
            return true;
        }
        ColorPack previous = ColoringCatalog.pack(index - 1);
        return prefs.coloringDoneInPack(previous.id, previous.size()) >= previous.size();
    }

    private void refreshChips() {
        List<ChipAdapter.Entry> packs = new ArrayList<>();
        for (int i = 0; i < ColoringCatalog.packCount(); i++) {
            ColorPack pack = ColoringCatalog.pack(i);
            boolean done = prefs.coloringDoneInPack(pack.id, pack.size()) >= pack.size();
            packs.add(new ChipAdapter.Entry(pack.name, !isPackUnlocked(i), done));
        }
        packAdapter.submit(packs, viewModel.packPosition());

        ColorPack pack = viewModel.pack();
        List<ChipAdapter.Entry> pages = new ArrayList<>();
        for (int i = 0; i < pack.size(); i++) {
            boolean done = prefs.isColoringDone(pack.id + "/" + i);
            pages.add(new ChipAdapter.Entry(pack.page(i).name, false, done));
        }
        pageAdapter.submit(pages, viewModel.pagePosition());
    }

    private void onRegionTapped(ColorRegion region) {
        boolean right = viewModel.paint(region);
        if (right) {
            binding.paintCanvas.flash(region.id);
            if (!Boolean.TRUE.equals(viewModel.pageComplete().getValue())) {
                onCorrect(getString(R.string.paint_right, region.label));
            }
        } else {
            onWrong(getString(R.string.paint_wrong, region.label));
        }
    }



    /** The legend doubles as the answer key: each part with the color it wants. */
    private void buildLegend(ColoringPage page, @Nullable Map<String, Integer> fills) {
        LinearLayout container = binding.paintLegend;
        container.removeAllViews();
        for (int i = 0; i < page.regions.size(); i++) {
            ColorRegion region = page.regions.get(i);
            Integer filled = fills == null ? null : fills.get(region.id);
            boolean correct = filled != null && filled == region.targetColor;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_legend_chip);
            row.setActivated(correct);
            row.setAlpha(correct ? 0.55f : 1f);
            row.setPadding(dp(8), dp(5), dp(10), dp(5));

            View dot = new View(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(region.targetColor);
            dot.setBackground(shape);
            row.addView(dot, new LinearLayout.LayoutParams(dp(16), dp(16)));

            AppCompatTextView label = new AppCompatTextView(this);
            label.setText(region.label);
            label.setTextSize(12f);
            label.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.setMarginStart(dp(6));
            row.addView(label, labelParams);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(6);
            container.addView(row, rowParams);
        }
    }

    private void refreshBanner() {
        ColoringPage page = viewModel.page();
        boolean complete = Boolean.TRUE.equals(viewModel.pageComplete().getValue());
        if (complete) {
            binding.paintBanner.setText(R.string.paint_banner_done);
            binding.paintBanner.setBackgroundResource(R.drawable.bg_banner_green);
            binding.paintBanner.setTextColor(ContextCompat.getColor(this, R.color.green_shadow));
        } else {
            binding.paintBanner.setText(getString(R.string.paint_banner_progress,
                    FaNum.of(viewModel.correctCount()), FaNum.of(page.size())));
            binding.paintBanner.setBackgroundResource(R.drawable.bg_banner_purple);
            binding.paintBanner.setTextColor(ContextCompat.getColor(this, R.color.purple_ink));
        }
        binding.paintLegendTitle.setText(page.name);
    }

    private void saveToGallery() {
        tap();
        Bitmap bitmap = ViewCapture.of(binding.paintCanvas, Color.WHITE);
        if (bitmap == null) {
            return;
        }
        GalleryRepository.get(this).save(GalleryEntry.KIND_COLORING, -1,
                viewModel.page().name, bitmap, success -> Toast.makeText(this,
                        success ? R.string.freedraw_saved : R.string.gallery_export_failed,
                        Toast.LENGTH_SHORT).show());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
