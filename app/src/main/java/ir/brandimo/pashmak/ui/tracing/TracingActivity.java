package ir.brandimo.pashmak.ui.tracing;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.audio.VoicePlayer;
import ir.brandimo.pashmak.data.catalog.Palette;
import ir.brandimo.pashmak.data.catalog.TraceCatalog;
import ir.brandimo.pashmak.databinding.ActivityTracingBinding;
import ir.brandimo.pashmak.ui.base.GameActivity;
import ir.brandimo.pashmak.util.FaNum;

/**
 * Trace the whole Persian alphabet and the digits. Completion is judged by how
 * much of the letter the child actually covered, measured against the glyph
 * outline taken from the font — so every glyph works without hand-authored
 * stroke data.
 */
public class TracingActivity extends GameActivity {

    private static final float PASS_COVERAGE = 0.55f;
    private static final int STARS_PER_GLYPH = 3;

    private ActivityTracingBinding binding;
    private String[] glyphs;
    private boolean digitsMode;
    private int index;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTracingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.traceHeader.headerTitle.setText(R.string.trace_title);
        binding.traceHeader.headerTitle.setTextColor(
                ContextCompat.getColor(this, R.color.green_shadow));
        binding.traceHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        bindStars(binding.traceHeader.headerStarsValue);
        attachCompanion();

        binding.traceCanvas.setBrushColor(Palette.GREEN);
        binding.traceCanvas.setBrushWidthDp(18f);
        binding.traceCanvas.setOnStrokeListener(this::refreshCoverage);

        binding.traceModeLetters.setOnClickListener(v -> setMode(false));
        binding.traceModeDigits.setOnClickListener(v -> setMode(true));
        binding.traceRestart.setOnClickListener(v -> {
            tap();
            binding.traceCanvas.clear();
            refreshCoverage();
        });
        binding.traceNext.setOnClickListener(v -> {
            tap();
            select(index + 1);
        });
        binding.traceDone.setOnClickListener(v -> finishGlyph());

        setMode(false);
        mascot.help("trace");
    }

    private void setMode(boolean digits) {
        digitsMode = digits;
        glyphs = TraceCatalog.set(this, digits);
        binding.traceModeLetters.setSelected(!digits);
        binding.traceModeDigits.setSelected(digits);
        binding.traceModeLetters.setTextColor(ContextCompat.getColor(this,
                digits ? R.color.ink_secondary : R.color.white));
        binding.traceModeDigits.setTextColor(ContextCompat.getColor(this,
                digits ? R.color.white : R.color.ink_secondary));
        buildGlyphChips();
        select(0);
    }

    private void buildGlyphChips() {
        LinearLayout container = binding.traceGlyphs;
        container.removeAllViews();
        for (int i = 0; i < glyphs.length; i++) {
            final int position = i;
            AppCompatButton chip = new AppCompatButton(this);
            chip.setText(glyphs[i]);
            chip.setAllCaps(false);
            chip.setTextSize(18f);
            chip.setBackgroundResource(R.drawable.bg_glyph_chip);
            chip.setMinWidth(0);
            chip.setMinimumWidth(0);
            chip.setMinHeight(0);
            chip.setMinimumHeight(0);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            chip.setOnClickListener(v -> {
                tap();
                select(position);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(6));
            container.addView(chip, params);
        }
    }

    private void select(int next) {
        if (glyphs.length == 0) {
            return;
        }
        index = Palette.wrap(next, glyphs.length);
        String glyph = glyphs[index];
        binding.traceCanvas.setGhostGlyph(glyph);
        binding.traceHint.setText(getString(R.string.trace_hint, glyph));
        refreshCoverage();
        refreshChips();
        speakGlyph(glyph);
    }

    private void refreshChips() {
        LinearLayout container = binding.traceGlyphs;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            child.setSelected(i == index);
            if (child instanceof AppCompatButton) {
                ((AppCompatButton) child).setTextColor(ContextCompat.getColor(this,
                        i == index ? R.color.white : R.color.green_shadow));
            }
        }
    }

    private void refreshCoverage() {
        int percent = Math.round(binding.traceCanvas.glyphCoverage() * 100f);
        binding.traceCoverage.setText(getString(R.string.trace_coverage, FaNum.of(percent)));
    }

    private void finishGlyph() {
        tap();
        float coverage = binding.traceCanvas.glyphCoverage();
        if (coverage >= PASS_COVERAGE) {
            mascot.addStars(STARS_PER_GLYPH);
            onCorrect(getString(R.string.trace_award));
            binding.traceCanvas.clear();
            binding.getRoot().postDelayed(() -> select(index + 1), 900L);
        } else {
            onWrong(getString(R.string.trace_incomplete));
        }
        refreshCoverage();
    }

    private void speakGlyph(String glyph) {
        String clip = digitsMode
                ? AudioManifest.digitVoice(index)
                : AudioManifest.letterVoice(glyph);
        VoicePlayer.get(this).speak(clip, null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
