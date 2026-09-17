package ir.brandimo.pashmak.ui.lullaby;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import ir.brandimo.pashmak.audio.LullabyPlayer;

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

    public LullabyHolder(@NonNull Application application) {
        super(application);
    }

    @NonNull
    public LullabyPlayer player() {
        if (player == null) {
            player = new LullabyPlayer(getApplication());
        }
        return player;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (player != null) {
            player.shutdown();
            player = null;
        }
    }
}
