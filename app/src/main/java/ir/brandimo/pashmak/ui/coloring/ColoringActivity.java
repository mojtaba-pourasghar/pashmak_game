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
    public static final String EXTRA_PACK = "pack_index";
    public static final String EXTRA_PAGE = "page_index";

    private ColoringViewModel viewModel;
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

        viewModel.pageIndex().observe(this, index -> {
            awardedThisPage = false;
            ColoringPage page = viewModel.page();
            binding.paintHeader.headerTitle.setText(getString(R.string.paint_title, page.name));
            binding.paintCanvas.setPage(page, viewModel.fills().getValue());
            buildLegend(page, viewModel.fills().getValue());
            binding.paintNext.setVisibility(View.GONE);
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
                onCorrect(getString(R.string.paint_all_right), AudioManifest.VOICE_PAINT_DONE);
                offerNextPage();
            }
        });

        binding.paintNext.setOnClickListener(v -> {
            tap();
            viewModel.selectPage(viewModel.pagePosition() + 1);
        });

        viewModel.selectPack(getIntent().getIntExtra(EXTRA_PACK, 0));
        viewModel.selectPage(getIntent().getIntExtra(EXTRA_PAGE, 0));
        mascot.help("paint");
    }

    /** Once a picture is finished, the rest of its pack is one tap away. */
    private void offerNextPage() {
        boolean hasNext = viewModel.pagePosition() + 1 < viewModel.pack().size();
        binding.paintNext.setVisibility(hasNext ? View.VISIBLE : View.GONE);
    }


    private void onRegionTapped(ColorRegion region) {
        boolean right = viewModel.paint(region);
        if (right) {
            binding.paintCanvas.flash(region.id);
            if (!Boolean.TRUE.equals(viewModel.pageComplete().getValue())) {
                onCorrect(getString(R.string.paint_right, region.label),
                        AudioManifest.VOICE_PAINT_RIGHT);
            }
        } else {
            onWrong(getString(R.string.paint_wrong, region.label),
                    AudioManifest.VOICE_PAINT_WRONG);
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
