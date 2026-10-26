package com.pashmak.app.ui.lullaby;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.pashmak.app.audio.LullabyMedia;
import com.pashmak.app.audio.LullabyPlayer;

/**
 * Owns the bedtime player so that turning the screen does not stop the song.
 *
 * <p>The lullaby screen is explicit that playback survives {@code onPause}, because
 * the song has to keep going when the screen dims in a child's hands. A rotation
 * destroys and rebuilds the Activity, which used to take the player down with it —
 * so tilting the tablet did what dimming the screen deliberately does not. The
 * player lives here instead, and is only really released when the screen is
 * finished with for good.
 *
 * <p>It is built with the Application context on purpose: a ViewModel outlives the
 * Activity, so holding one would leak it.
 */
public class LullabyHolder extends AndroidViewModel {

    private LullabyPlayer player;
    /** Lock-screen / notification controls; lives exactly as long as the player. */
    private LullabyMedia media;

    public LullabyHolder(@NonNull Application application) {
        super(application);
    }

    @NonNull
    public LullabyPlayer player() {
        if (player == null) {
            player = new LullabyPlayer(getApplication());
            media = new LullabyMedia(getApplication(), player);
        }
        return player;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (media != null) {
            media.release();
            media = null;
        }
        if (player != null) {
            player.shutdown();
            player = null;
        }
    }
}
