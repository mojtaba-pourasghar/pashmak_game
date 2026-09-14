package ir.brandimo.pashmak.ui.tracing;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
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
 * Trace one glyph chosen on the stage picker. Completion is judged by how much
 * of the letter the child actually covered, measured against the glyph outline
 * taken from the font — so every glyph works without hand-authored stroke data.
 */
public class TracingActivity extends GameActivity {

    public static final String EXTRA_DIGITS = "digits_mode";
    public static final String EXTRA_INDEX = "glyph_index";

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

        digitsMode = getIntent().getBooleanExtra(EXTRA_DIGITS, false);
        glyphs = TraceCatalog.set(this, digitsMode);
        select(getIntent().getIntExtra(EXTRA_INDEX, 0));
        mascot.help("trace");
    }

    private void select(int next) {
        if (glyphs.length == 0) {
            return;
        }
        index = Palette.wrap(next, glyphs.length);
        String glyph = glyphs[index];
        binding.traceCanvas.setGhostGlyph(glyph);
        binding.traceCanvas.clear();
        binding.traceHint.setText(getString(R.string.trace_hint, glyph));
        binding.traceNext.setVisibility(glyphs.length > 1 ? View.VISIBLE : View.GONE);
        refreshCoverage();
        speakGlyph(glyph);
    }

    private void refreshCoverage() {
        int percent = Math.round(binding.traceCanvas.glyphCoverage() * 100f);
        binding.traceCoverage.setText(getString(R.string.trace_coverage, FaNum.of(percent)));
    }

    private void finishGlyph() {
        tap();
        float coverage = binding.traceCanvas.glyphCoverage();
        if (coverage >= PASS_COVERAGE) {
            prefs.setTraceDone(glyphs[index]);
            mascot.addStars(STARS_PER_GLYPH);
            onCorrect(getString(R.string.trace_award), AudioManifest.VOICE_TRACE_DONE);
            binding.traceCanvas.clear();
            binding.getRoot().postDelayed(() -> select(index + 1), 900L);
        } else {
            onWrong(getString(R.string.trace_incomplete), AudioManifest.VOICE_TRACE_MORE);
        }
        refreshCoverage();
    }

    private void speakGlyph(String glyph) {
        String clip = digitsMode
                ? AudioManifest.digitVoice(index)
                : AudioManifest.letterVoice(glyph);
        VoicePlayer.get(this).speak(clip, null);
    }
}
