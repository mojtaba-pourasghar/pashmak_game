package ir.brandimo.pashmak.ui.lullaby;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import java.util.Locale;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.LullabyPlayer;
import ir.brandimo.pashmak.data.catalog.Lullaby;
import ir.brandimo.pashmak.data.catalog.LullabyCatalog;
import ir.brandimo.pashmak.databinding.ActivityLullabyBinding;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.util.FaNum;

/**
 * Bedtime: the lullaby playlist, a player that can repeat one song or roll on to
 * the next by itself, and a sleep timer that fades the last minute out.
 *
 * <p>The background music is silenced here — this screen is the music — and
 * playback deliberately survives {@code onPause} so the song keeps going when the
 * screen dims in the child's hands.
 */
public class LullabyActivity extends BaseActivity implements LullabyPlayer.Listener {

    /** Sleep-timer choices, in minutes; 0 is "off". */
    private static final int[] TIMER_MINUTES = {0, 15, 30, 60};

    private ActivityLullabyBinding binding;
    private LullabyAdapter adapter;
    private LullabyPlayer player;

    private boolean seekDragging;
    private boolean warnedMissing;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLullabyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.lullabyHeader.headerTitle.setText(R.string.lullaby_title);
        binding.lullabyHeader.headerTitle.setTextColor(
                ContextCompat.getColor(this, R.color.night_gold));
        binding.lullabyHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        bindStars(binding.lullabyHeader.headerStarsValue);
        attachCompanion();
        shrinkCompanion();

        player = new LullabyPlayer(this);
        player.setRepeatOne(prefs.lullabyRepeat());
        player.setAutoNext(prefs.lullabyAutoNext());
        player.setListener(this);

        adapter = new LullabyAdapter(LullabyCatalog.all(), this::pick);
        binding.lullabyList.setAdapter(adapter);

        binding.lullabyPlay.setOnClickListener(v -> {
            tap();
            player.toggle();
        });
        binding.lullabyNext.setOnClickListener(v -> {
            tap();
            player.next();
        });
        binding.lullabyPrev.setOnClickListener(v -> {
            tap();
            player.previous();
        });
        binding.lullabyRepeat.setOnClickListener(v -> {
            tap();
            boolean value = !player.repeatOne();
            player.setRepeatOne(value);
            prefs.setLullabyRepeat(value);
            refreshToggles();
        });
        binding.lullabyAuto.setOnClickListener(v -> {
            tap();
            boolean value = !player.autoNext();
            player.setAutoNext(value);
            prefs.setLullabyAutoNext(value);
            refreshToggles();
        });

        binding.lullabySeek.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(android.widget.SeekBar bar,
                                                  int progress, boolean fromUser) {
                    }

                    @Override
                    public void onStartTrackingTouch(android.widget.SeekBar bar) {
                        seekDragging = true;
                    }

                    @Override
                    public void onStopTrackingTouch(android.widget.SeekBar bar) {
                        seekDragging = false;
                        Object duration = bar.getTag(R.id.lullaby_seek_duration);
                        int total = duration instanceof Integer ? (Integer) duration : 0;
                        player.seekTo(Math.round(total * bar.getProgress() / 1000f));
                    }
                });
        binding.lullabySeek.setTag(R.id.lullaby_seek_duration, 0);

        buildTimerChips();
        refreshToggles();
        showTrack(0);
        mascot.say(getString(R.string.lullaby_greeting), MascotState.IDLE,
                MascotController.HOLD_MIN_MS);
    }

    /** No background loop here: the lullaby is the sound on this screen. */
    @Override
    @Nullable
    protected String musicTrack() {
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.shutdown();
        }
    }

    private void pick(int index) {
        tap();
        warnedMissing = false;
        player.play(index);
    }

    /** The sleep-timer row: off, then a quarter hour, half an hour, an hour. */
    private void buildTimerChips() {
        LinearLayout container = binding.lullabyTimers;
        container.removeAllViews();
        int chosen = prefs.lullabySleepMinutes();
        for (int minutes : TIMER_MINUTES) {
            final int value = minutes;
            AppCompatButton chip = new AppCompatButton(this);
            chip.setAllCaps(false);
            chip.setText(minutes == 0
                    ? getString(R.string.lullaby_timer_off)
                    : FaNum.of(minutes));
            chip.setTextSize(13f);
            chip.setTextColor(ContextCompat.getColor(this, R.color.night_ink));
            chip.setBackgroundResource(R.drawable.bg_night_toggle);
            chip.setMinWidth(0);
            chip.setMinimumWidth(0);
            chip.setMinHeight(0);
            chip.setMinimumHeight(0);
            chip.setPadding(dp(10), dp(4), dp(10), dp(4));
            chip.setSelected(minutes == chosen);
            chip.setOnClickListener(v -> {
                tap();
                prefs.setLullabySleepMinutes(value);
                player.setSleepMinutes(value);
                buildTimerChips();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMarginEnd(dp(4));
            container.addView(chip, params);
        }
        if (chosen > 0) {
            player.setSleepMinutes(chosen);
        }
    }

    private void refreshToggles() {
        binding.lullabyRepeat.setSelected(player.repeatOne());
        binding.lullabyAuto.setSelected(player.autoNext());
        binding.lullabyRepeat.setTextColor(ContextCompat.getColor(this,
                player.repeatOne() ? R.color.night_top : R.color.night_ink));
        binding.lullabyAuto.setTextColor(ContextCompat.getColor(this,
                player.autoNext() ? R.color.night_top : R.color.night_ink));
    }

    private void showTrack(int index) {
        Lullaby lullaby = LullabyCatalog.at(index);
        binding.lullabyNowTitle.setText(lullaby.title);
        binding.lullabyNowLine.setText(lullaby.line);
        adapter.setCurrent(index, player.isPlaying());
        binding.lullabyList.scrollToPosition(index);
    }

    /* LullabyPlayer.Listener */

    @Override
    public void onTrackChanged(int index) {
        showTrack(index);
    }

    @Override
    public void onPlayingChanged(boolean playing) {
        binding.lullabyPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        binding.lullabyPlay.setContentDescription(getString(
                playing ? R.string.lullaby_pause : R.string.lullaby_play));
        adapter.setCurrent(player.index(), playing);
    }

    @Override
    public void onProgress(int positionMs, int durationMs) {
        binding.lullabySeek.setTag(R.id.lullaby_seek_duration, durationMs);
        binding.lullabySeek.setEnabled(durationMs > 0);
        if (!seekDragging) {
            binding.lullabySeek.setProgress(durationMs > 0
                    ? Math.round(positionMs * 1000f / durationMs) : 0);
        }
        binding.lullabyTime.setText(getString(R.string.lullaby_time,
                clock(positionMs), clock(durationMs)));
    }

    @Override
    public void onClipMissing(@NonNull Lullaby lullaby) {
        // Recordings are dropped in by hand; say so once instead of on every skip.
        if (warnedMissing) {
            return;
        }
        warnedMissing = true;
        mascot.say(getString(R.string.lullaby_missing, lullaby.title),
                MascotState.IDLE, MascotController.HOLD_MIN_MS);
    }

    @Override
    public void onTimerChanged(long remainingMs) {
        if (remainingMs <= 0L) {
            binding.lullabyTimerNote.setVisibility(View.GONE);
            return;
        }
        binding.lullabyTimerNote.setVisibility(View.VISIBLE);
        binding.lullabyTimerNote.setText(getString(R.string.lullaby_timer_left,
                clock((int) remainingMs)));
    }

    @Override
    public void onPlaylistFinished() {
        mascot.say(getString(R.string.lullaby_goodnight), MascotState.IDLE,
                MascotController.HOLD_MIN_MS);
    }

    private String clock(int milliseconds) {
        int total = Math.max(0, milliseconds) / 1000;
        return FaNum.of(total / 60) + ":"
                + FaNum.of(String.format(Locale.US, "%02d", total % 60));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
