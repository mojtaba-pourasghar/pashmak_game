package com.pashmak.app.ui.tracing;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.pashmak.app.R;
import com.pashmak.app.audio.AudioManifest;
import com.pashmak.app.audio.VoicePlayer;
import com.pashmak.app.data.catalog.Palette;
import com.pashmak.app.data.catalog.TraceCatalog;
import com.pashmak.app.databinding.ActivityTracingBinding;
import com.pashmak.app.ui.base.GameActivity;
import com.pashmak.app.ui.common.CanvasKeeper;
import com.pashmak.app.util.FaNum;

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
    /** The breath between the praise for one letter and the next one being named. */
    private static final long GLYPH_PAUSE_MS = 400L;

    private ActivityTracingBinding binding;
    private CanvasKeeper keeper;
    private static final String STATE_GLYPH = "glyph_index";

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
            goToNext();
        });
        binding.traceDone.setOnClickListener(v -> finishGlyph());

        digitsMode = getIntent().getBooleanExtra(EXTRA_DIGITS, false);
        glyphs = TraceCatalog.set(this, digitsMode);
        select(savedInstanceState == null
                ? getIntent().getIntExtra(EXTRA_INDEX, 0)
                : savedInstanceState.getInt(STATE_GLYPH));
        // select() clears the sheet, so the strokes come back after it, not before.
        keeper = new ViewModelProvider(this).get(CanvasKeeper.class);
        binding.traceCanvas.adoptLayer(keeper.take());
        refreshCoverage();
        mascot.help("trace");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_GLYPH, index);
        keeper.hold(binding.traceCanvas.detachLayer());
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

    /**
     * On to the next glyph, unless it belongs to the full version. The trace list
     * puts every letter first and the digits after them, and the free stages are
     * counted along that list, so a glyph's row depends on which set it is in.
     */
    private void goToNext() {
        if (isFinishing() || glyphs.length == 0) {
            return;
        }
        int next = Palette.wrap(index + 1, glyphs.length);
        int row = digitsMode ? TraceCatalog.letters(this).length + next : next;
        if (!stageOpen(row)) {
            pendingNext = true;
            offerFullVersion();
            return;
        }
        select(next);
    }

    /** "Next" reached a locked glyph; it opens once the purchase lands. */
    private boolean pendingNext;

    @Override
    protected void onFullVersionBought() {
        if (pendingNext) {
            pendingNext = false;
            select(index + 1);
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
            prefs.setTraceDone(glyphs[index]);
            mascot.addStars(STARS_PER_GLYPH);
            long said = onCorrect(getString(R.string.trace_award),
                    AudioManifest.VOICE_TRACE_DONE);
            binding.traceCanvas.clear();
            // The next letter is spoken as soon as it is selected, and that stops
            // whatever is sounding — so wait for the praise to finish first.
            binding.getRoot().postDelayed(this::goToNext, said + GLYPH_PAUSE_MS);
        } else {
            onWrong(getString(R.string.trace_incomplete), AudioManifest.VOICE_TRACE_MORE);
        }
        refreshCoverage();
    }

    private void speakGlyph(String glyph) {
        String clip = digitsMode
                ? AudioManifest.digitVoice(index)
                : AudioManifest.letterVoice(glyph);
        // The glyph itself is the line — "ب" read aloud.
        VoicePlayer.get(this).speak(clip, glyph, null);
    }
}
