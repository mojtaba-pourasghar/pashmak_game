package com.pashmak.app.audio;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.pashmak.app.R;

/**
 * Keeps the app in the foreground while a lullaby is loaded, so the song goes on
 * with the screen locked, and receives the notification's buttons. The player and
 * the session themselves live in {@link LullabyMedia}; this only fronts them.
 */
public class LullabyService extends Service {

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        LullabyMedia media = LullabyMedia.active();
        if (media != null && intent != null) {
            media.handle(intent.getAction());
        }
        // startForeground must follow every startForegroundService, even when the
        // lullaby screen has already gone and there is nothing left to show.
        Notification notification = media != null
                ? media.buildNotification()
                : new NotificationCompat.Builder(this, LullabyMedia.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_moon)
                        .setSilent(true)
                        .build();
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0;
        ServiceCompat.startForeground(this, LullabyMedia.NOTIFICATION_ID, notification, type);
        if (media == null) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        LullabyMedia media = LullabyMedia.active();
        if (media != null) {
            media.onTaskRemoved();
        }
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
