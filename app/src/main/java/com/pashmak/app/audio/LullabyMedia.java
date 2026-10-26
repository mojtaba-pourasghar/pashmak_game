package com.pashmak.app.audio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.pashmak.app.R;
import com.pashmak.app.data.catalog.Lullaby;
import com.pashmak.app.data.catalog.LullabyCatalog;
import com.pashmak.app.ui.lullaby.LullabyActivity;

/**
 * The lullaby on the lock screen: a media session plus a media-style notification,
 * so a parent can pause the song or skip to the next one without unlocking the
 * phone (and without the bright app filling a dark bedroom).
 *
 * <p>It listens to the same {@link LullabyPlayer} as the screen, through a second
 * listener slot, and drives it back through the same calls the screen's buttons
 * use, so the two always agree. Once a song has started, {@link LullabyService}
 * keeps the process in the foreground, which is what lets playback carry on with
 * the screen off.
 *
 * <p>Lives exactly as long as the player: {@code LullabyHolder} builds it with the
 * player and releases it when the lullaby screen is finished with for good.
 */
public final class LullabyMedia implements LullabyPlayer.Listener {

    static final String ACTION_TOGGLE = "com.pashmak.app.lullaby.TOGGLE";
    static final String ACTION_NEXT = "com.pashmak.app.lullaby.NEXT";
    static final String ACTION_PREV = "com.pashmak.app.lullaby.PREV";

    static final int NOTIFICATION_ID = 4107;
    static final String CHANNEL_ID = "lullaby_playback";

    /** A tick whose position is this far from where it should be means a seek. */
    private static final long SEEK_DRIFT_MS = 1_500L;

    private static final long ACTIONS = PlaybackState.ACTION_PLAY
            | PlaybackState.ACTION_PAUSE
            | PlaybackState.ACTION_PLAY_PAUSE
            | PlaybackState.ACTION_SKIP_TO_NEXT
            | PlaybackState.ACTION_SKIP_TO_PREVIOUS
            | PlaybackState.ACTION_SEEK_TO
            | PlaybackState.ACTION_STOP;

    /** The one live instance, for {@link LullabyService}; null when none. */
    @Nullable
    private static LullabyMedia active;

    private final Context appContext;
    private final LullabyPlayer player;
    private final MediaSession session;

    private boolean serviceStarted;
    private int artFor = -1;
    @Nullable
    private Bitmap art;

    /** Where the last published state says playback is, to spot seeks. */
    private long statePosition;
    private long stateTime;
    private boolean statePlaying;

    public LullabyMedia(@NonNull Context context, @NonNull LullabyPlayer player) {
        this.appContext = context.getApplicationContext();
        this.player = player;
        createChannel();

        session = new MediaSession(appContext, "PashmakLullaby");
        session.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                if (!LullabyMedia.this.player.isPlaying()) {
                    LullabyMedia.this.player.toggle();
                }
            }

            @Override
            public void onPause() {
                if (LullabyMedia.this.player.isPlaying()) {
                    LullabyMedia.this.player.toggle();
                }
            }

            @Override
            public void onStop() {
                // Stop from a headset or a watch means "be quiet", not "forget the song".
                onPause();
            }

            @Override
            public void onSkipToNext() {
                LullabyMedia.this.player.next();
            }

            @Override
            public void onSkipToPrevious() {
                LullabyMedia.this.player.previous();
            }

            @Override
            public void onSeekTo(long pos) {
                LullabyMedia.this.player.seekTo((int) pos);
            }
        }, new Handler(Looper.getMainLooper()));
        session.setSessionActivity(openScreenIntent());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Before Oreo a session only gets the headset / lock-screen buttons when
            // it asks for them; from Oreo on these flags are always set.
            setLegacyFlags();
        }

        player.setMediaListener(this);
        active = this;
        publishMetadata();
        publishState();
    }

    @SuppressWarnings("deprecation")
    private void setLegacyFlags() {
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    @Nullable
    static LullabyMedia active() {
        return active;
    }

    /** Tears down the session, the notification and the foreground service. */
    public void release() {
        player.setMediaListener(null);
        if (active == this) {
            active = null;
        }
        if (serviceStarted) {
            serviceStarted = false;
            appContext.stopService(new Intent(appContext, LullabyService.class));
        }
        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID);
        session.setActive(false);
        session.release();
    }

    /** The app was swiped away from recents: nobody is left to hear it. */
    void onTaskRemoved() {
        player.shutdown();
        release();
    }

    /** A button on the notification, routed through {@link LullabyService}. */
    void handle(@Nullable String action) {
        if (ACTION_TOGGLE.equals(action)) {
            player.toggle();
        } else if (ACTION_NEXT.equals(action)) {
            player.next();
        } else if (ACTION_PREV.equals(action)) {
            player.previous();
        }
    }

    /* LullabyPlayer.Listener */

    @Override
    public void onTrackChanged(int index) {
        publishMetadata();
        publishState();
        refreshNotification();
    }

    @Override
    public void onPlayingChanged(boolean playing) {
        if (playing) {
            // The first start comes from a tap on the screen, so the app is in the
            // foreground and may start a foreground service. The service then stays
            // up, paused or not, until the screen is left, so resuming from the
            // lock screen never has to start one from the background.
            session.setActive(true);
            startService();
        }
        publishMetadata();
        publishState();
        refreshNotification();
    }

    @Override
    public void onProgress(int positionMs, int durationMs) {
        // The session extrapolates the position itself; only a jump needs telling.
        long expected = statePlaying
                ? statePosition + (System.currentTimeMillis() - stateTime)
                : statePosition;
        if (Math.abs(expected - positionMs) > SEEK_DRIFT_MS) {
            publishState();
        }
    }

    @Override
    public void onClipMissing(@NonNull Lullaby lullaby) {
    }

    @Override
    public void onTimerChanged(long remainingMs) {
    }

    @Override
    public void onPlaylistFinished() {
    }

    /* Session */

    private void publishMetadata() {
        Lullaby lullaby = LullabyCatalog.at(player.index());
        int duration = player.durationMs();
        if (duration <= 0) {
            duration = lullaby.seconds * 1000;
        }
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, lullaby.title)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, lullaby.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST,
                        appContext.getString(R.string.lullaby_title))
                .putString(MediaMetadata.METADATA_KEY_ALBUM,
                        appContext.getString(R.string.app_name))
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork())
                .build());
    }

    private void publishState() {
        statePlaying = player.isPlaying();
        statePosition = player.positionMs();
        stateTime = System.currentTimeMillis();
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(ACTIONS)
                .setState(statePlaying
                                ? PlaybackState.STATE_PLAYING
                                : PlaybackState.STATE_PAUSED,
                        statePosition, statePlaying ? 1f : 0f)
                .build());
    }

    /** The lullaby's own icon on the night-sky colour, for the lock screen. */
    @Nullable
    private Bitmap artwork() {
        int index = player.index();
        if (art != null && artFor == index) {
            return art;
        }
        Drawable icon = ContextCompat.getDrawable(appContext,
                LullabyCatalog.at(index).icon);
        if (icon == null) {
            return null;
        }
        int size = 256;
        int inset = size / 5;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(ContextCompat.getColor(appContext, R.color.night_top));
        icon = icon.mutate();
        icon.setBounds(inset, inset, size - inset, size - inset);
        icon.draw(canvas);
        art = bitmap;
        artFor = index;
        return art;
    }

    /* Notification */

    /**
     * The platform's own media notification (API 21+, the app's minSdk), so no
     * extra library is needed for the lock-screen controls.
     */
    @NonNull
    @SuppressWarnings("deprecation") // addAction(int, ...) and Builder(Context) below API 23/26
    Notification buildNotification() {
        Lullaby lullaby = LullabyCatalog.at(player.index());
        boolean playing = player.isPlaying();
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(appContext, CHANNEL_ID)
                : new Notification.Builder(appContext).setPriority(Notification.PRIORITY_LOW);
        return builder
                .setSmallIcon(R.drawable.ic_moon)
                .setLargeIcon(artwork())
                .setContentTitle(lullaby.title)
                .setContentText(appContext.getString(R.string.lullaby_title))
                .setContentIntent(openScreenIntent())
                // Shown in full on the lock screen: it is only a song title.
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(playing)
                .addAction(R.drawable.ic_back,
                        appContext.getString(R.string.lullaby_prev),
                        serviceIntent(ACTION_PREV, 1))
                .addAction(playing ? R.drawable.ic_pause : R.drawable.ic_play,
                        appContext.getString(playing
                                ? R.string.lullaby_pause : R.string.lullaby_play),
                        serviceIntent(ACTION_TOGGLE, 2))
                .addAction(R.drawable.ic_next,
                        appContext.getString(R.string.lullaby_next),
                        serviceIntent(ACTION_NEXT, 3))
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(session.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    private void refreshNotification() {
        if (!serviceStarted) {
            return;
        }
        try {
            NotificationManagerCompat.from(appContext)
                    .notify(NOTIFICATION_ID, buildNotification());
        } catch (SecurityException ignored) {
            // Notifications switched off: the session still drives the lock screen.
        }
    }

    private void startService() {
        if (serviceStarted) {
            return;
        }
        try {
            ContextCompat.startForegroundService(appContext,
                    new Intent(appContext, LullabyService.class));
            serviceStarted = true;
        } catch (RuntimeException ignored) {
            // Not allowed from here (a background start on Android 12+): the song
            // still plays, it just has no lock-screen controls this time.
        }
    }

    @NonNull
    private PendingIntent serviceIntent(@NonNull String action, int requestCode) {
        Intent intent = new Intent(appContext, LullabyService.class).setAction(action);
        return PendingIntent.getService(appContext, requestCode, intent, immutable());
    }

    /** Brings the existing lullaby screen back rather than opening a second one. */
    @NonNull
    private PendingIntent openScreenIntent() {
        Intent intent = new Intent(appContext, LullabyActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(appContext, 0, intent, immutable());
    }

    private static int immutable() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = appContext.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                appContext.getString(R.string.lullaby_channel),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(appContext.getString(R.string.lullaby_channel_desc));
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }
}
