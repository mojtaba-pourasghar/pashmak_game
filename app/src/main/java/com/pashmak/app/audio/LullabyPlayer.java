package com.pashmak.app.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pashmak.app.data.catalog.Lullaby;
import com.pashmak.app.data.catalog.LullabyCatalog;
import com.pashmak.app.data.catalog.Palette;

/**
 * The bedtime player: one lullaby at a time, with repeat, automatic advance and a
 * sleep timer that fades the last minute out rather than cutting it off.
 *
 * <p>The recordings are dropped into res/raw by hand, so every lookup goes through
 * {@code Resources#getIdentifier} and a missing file is reported to the caller
 * instead of throwing — an empty res/raw leaves the screen usable.
 */
public final class LullabyPlayer {

    /** How long before the sleep timer expires the music starts fading. */
    private static final long FADE_MS = 60_000L;
    private static final long TICK_MS = 500L;

    public interface Listener {
        /** A new lullaby became current, playing or not. */
        void onTrackChanged(int index);

        void onPlayingChanged(boolean playing);

        /** Both values in milliseconds; duration is 0 while nothing is loaded. */
        void onProgress(int positionMs, int durationMs);

        /** The recording for this lullaby has not been added to res/raw yet. */
        void onClipMissing(@NonNull Lullaby lullaby);

        /** Remaining sleep-timer time, or 0 when the timer is off. */
        void onTimerChanged(long remainingMs);

        /** Reached the end of the playlist with auto-advance off, or the timer ran out. */
        void onPlaylistFinished();
    }

    private final Context appContext;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    private Listener listener;
    /**
     * A second, screen-independent listener: the lock-screen / notification controls
     * ({@link LullabyMedia}). It outlives any one Activity, unlike {@link #listener}.
     */
    @Nullable
    private Listener mediaListener;
    @Nullable
    private MediaPlayer player;

    private int index;
    private boolean repeatOne;
    private boolean autoNext = true;
    /**
     * How many songs from the top of the playlist may sound. The free version opens
     * only the first; next, previous, auto-advance and the lock-screen buttons all
     * stay inside this, so none of them can reach a song that has not been bought.
     */
    private int playable = 1;

    /** Wall-clock deadline for the sleep timer, or 0 when it is off. */
    private long sleepDeadline;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            pump();
            handler.postDelayed(this, TICK_MS);
        }
    };

    public LullabyPlayer(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    public void setListener(@Nullable Listener value) {
        listener = value;
    }

    void setMediaListener(@Nullable Listener value) {
        mediaListener = value;
    }

    public int index() {
        return index;
    }

    /** Current position in milliseconds, or 0 while nothing is loaded. */
    public int positionMs() {
        try {
            return player != null ? player.getCurrentPosition() : 0;
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    /** Length of the loaded recording in milliseconds, or 0 while nothing is loaded. */
    public int durationMs() {
        try {
            return player != null ? Math.max(0, player.getDuration()) : 0;
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    public boolean isPlaying() {
        try {
            return player != null && player.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public boolean repeatOne() {
        return repeatOne;
    }

    public boolean autoNext() {
        return autoNext;
    }

    public void setRepeatOne(boolean value) {
        repeatOne = value;
    }

    public void setAutoNext(boolean value) {
        autoNext = value;
    }

    /**
     * Sets how many songs from the top may play. If the one that is current falls
     * outside — a refund while it was playing — it stops and the playlist goes back
     * to the first song.
     */
    public void setPlayable(int count) {
        playable = Math.max(1, count);
        if (index >= limit()) {
            release();
            stopTicking();
            index = 0;
            notifyTrack();
            notifyPlaying(false);
            notifyProgress();
        }
    }

    public boolean isPlayable(int position) {
        return position >= 0 && position < limit();
    }

    /** The part of the playlist that may sound: never more than the songs there are. */
    private int limit() {
        return Math.min(playable, LullabyCatalog.count());
    }

    /** Keeps an index inside the playable part, wrapping in both directions. */
    private int wrap(int value) {
        return Palette.wrap(value, limit());
    }

    /**
     * Starts the lullaby at this position, replacing whatever is sounding. A song
     * outside the playable part is refused; the screen offers the purchase instead.
     */
    public void play(int position) {
        if (!isPlayable(position)) {
            return;
        }
        index = position;
        notifyTrack();
        openCurrent(0);
    }

    /**
     * Opens the current lullaby. When the recording is missing and advancing is
     * allowed, walks forward looking for one that exists; {@code attempts} keeps
     * that walk from looping forever over an empty res/raw.
     */
    private void openCurrent(int attempts) {
        release();
        Lullaby lullaby = LullabyCatalog.at(index);
        int resId = resolve(lullaby.clip);
        if (resId == 0) {
            notifyMissing(lullaby);
            if (attempts + 1 < limit()) {
                index = wrap(index + 1);
                notifyTrack();
                openCurrent(attempts + 1);
            } else {
                notifyPlaying(false);
                notifyProgress();
            }
            return;
        }
        try {
            player = MediaPlayer.create(appContext, resId);
            if (player == null) {
                notifyMissing(lullaby);
                notifyPlaying(false);
                return;
            }
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // Keeps the CPU awake while a song sounds, so with the screen locked the
            // next lullaby still starts and the sleep timer still fades on time.
            player.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK);
            player.setOnCompletionListener(mp -> onTrackFinished());
            player.setOnErrorListener((mp, what, extra) -> {
                release();
                notifyPlaying(false);
                return true;
            });
            applyVolume();
            player.start();
            notifyPlaying(true);
            startTicking();
        } catch (Exception e) {
            release();
            notifyPlaying(false);
        }
    }

    private void onTrackFinished() {
        if (repeatOne) {
            try {
                if (player != null) {
                    player.seekTo(0);
                    player.start();
                    return;
                }
            } catch (IllegalStateException ignored) {
            }
            openCurrent(0);
            return;
        }
        if (autoNext) {
            boolean lastOfList = index == limit() - 1;
            index = wrap(index + 1);
            notifyTrack();
            openCurrent(0);
            if (lastOfList && listener != null) {
                listener.onPlaylistFinished();
            }
            return;
        }
        release();
        stopTicking();
        notifyPlaying(false);
        notifyProgress();
        if (listener != null) {
            listener.onPlaylistFinished();
        }
    }

    /** Play/pause without losing the position. */
    public void toggle() {
        if (player == null) {
            play(index);
            return;
        }
        try {
            if (player.isPlaying()) {
                player.pause();
                notifyPlaying(false);
                stopTicking();
            } else {
                player.start();
                notifyPlaying(true);
                startTicking();
            }
        } catch (IllegalStateException e) {
            openCurrent(0);
        }
    }

    public void next() {
        index = wrap(index + 1);
        notifyTrack();
        openCurrent(0);
    }

    public void previous() {
        index = wrap(index - 1);
        notifyTrack();
        openCurrent(0);
    }

    public void seekTo(int positionMs) {
        try {
            if (player != null) {
                player.seekTo(positionMs);
                notifyProgress();
            }
        } catch (IllegalStateException ignored) {
        }
    }

    /** Minutes of sleep timer, or 0 to switch it off. */
    public void setSleepMinutes(int minutes) {
        if (minutes <= 0) {
            sleepDeadline = 0L;
            applyVolume();
            notifyTimer();
            return;
        }
        sleepDeadline = System.currentTimeMillis() + minutes * 60_000L;
        applyVolume();
        notifyTimer();
        startTicking();
    }

    public long sleepRemainingMs() {
        return sleepDeadline == 0L ? 0L : Math.max(0L, sleepDeadline - System.currentTimeMillis());
    }

    /** Called every tick: drives the progress bar, the fade and the timer. */
    private void pump() {
        notifyProgress();
        if (sleepDeadline != 0L) {
            long remaining = sleepRemainingMs();
            notifyTimer();
            applyVolume();
            if (remaining == 0L) {
                sleepDeadline = 0L;
                release();
                stopTicking();
                notifyPlaying(false);
                notifyTimer();
                if (listener != null) {
                    listener.onPlaylistFinished();
                }
            }
        }
    }

    /** Full volume, except inside the sleep timer's last minute. */
    private void applyVolume() {
        if (player == null) {
            return;
        }
        float level = 1f;
        if (sleepDeadline != 0L) {
            long remaining = sleepRemainingMs();
            if (remaining < FADE_MS) {
                level = remaining / (float) FADE_MS;
            }
        }
        try {
            player.setVolume(level, level);
        } catch (IllegalStateException ignored) {
        }
    }

    private void startTicking() {
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, TICK_MS);
    }

    private void stopTicking() {
        if (sleepDeadline == 0L) {
            handler.removeCallbacks(tick);
        }
    }

    public void release() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }

    /** Full teardown; the screen calls this when it goes away for good. */
    public void shutdown() {
        handler.removeCallbacks(tick);
        listener = null;
        sleepDeadline = 0L;
        release();
    }

    private int resolve(String name) {
        try {
            return appContext.getResources().getIdentifier(
                    name, "raw", appContext.getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }

    private void notifyTrack() {
        if (listener != null) {
            listener.onTrackChanged(index);
        }
        if (mediaListener != null) {
            mediaListener.onTrackChanged(index);
        }
    }

    private void notifyPlaying(boolean playing) {
        if (listener != null) {
            listener.onPlayingChanged(playing);
        }
        if (mediaListener != null) {
            mediaListener.onPlayingChanged(playing);
        }
    }

    private void notifyMissing(@NonNull Lullaby lullaby) {
        if (listener != null) {
            listener.onClipMissing(lullaby);
        }
    }

    private void notifyTimer() {
        if (listener != null) {
            listener.onTimerChanged(sleepRemainingMs());
        }
    }

    private void notifyProgress() {
        if (listener == null && mediaListener == null) {
            return;
        }
        int position = positionMs();
        int duration = durationMs();
        if (listener != null) {
            listener.onProgress(position, duration);
        }
        if (mediaListener != null) {
            mediaListener.onProgress(position, duration);
        }
    }
}
