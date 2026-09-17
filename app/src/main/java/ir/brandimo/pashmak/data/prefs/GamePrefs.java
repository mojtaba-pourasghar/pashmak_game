package ir.brandimo.pashmak.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Light, frequently-read state. Keys match the prototype's localStorage names so
 * the two implementations stay recognisably the same product.
 */
public final class GamePrefs {

    private static final String FILE = "pashmak_prefs";

    public static final String KEY_MUTED = "mascot_muted";
    public static final String KEY_WELCOMED = "mascot_has_welcomed";
    public static final String KEY_STARS = "mascot_stars_count";
    public static final String KEY_DIFFICULTY = "difficulty";
    public static final String KEY_SOUND = "sound_enabled";
    public static final String KEY_MUSIC = "music_enabled";

    public static final int DIFFICULTY_EASY = 0;
    public static final int DIFFICULTY_MEDIUM = 1;
    public static final int DIFFICULTY_HARD = 2;

    private static volatile GamePrefs instance;

    private final SharedPreferences prefs;
    private final MutableLiveData<Integer> stars = new MutableLiveData<>();
    private final MutableLiveData<Boolean> muted = new MutableLiveData<>();

    private GamePrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
        stars.setValue(prefs.getInt(KEY_STARS, 0));
        muted.setValue(prefs.getBoolean(KEY_MUTED, false));
    }

    public static GamePrefs get(@NonNull Context context) {
        GamePrefs local = instance;
        if (local == null) {
            synchronized (GamePrefs.class) {
                if (instance == null) {
                    instance = new GamePrefs(context);
                }
                local = instance;
            }
        }
        return local;
    }

    public LiveData<Integer> starsLive() {
        return stars;
    }

    public int stars() {
        Integer value = stars.getValue();
        return value == null ? 0 : value;
    }

    /** Stars only ever accumulate downward to zero, never below it. */
    public void addStars(int delta) {
        int next = Math.max(0, stars() + delta);
        prefs.edit().putInt(KEY_STARS, next).apply();
        stars.setValue(next);
    }

    public void resetStars() {
        prefs.edit().putInt(KEY_STARS, 0).apply();
        stars.setValue(0);
    }

    public LiveData<Boolean> mutedLive() {
        return muted;
    }

    public boolean isMuted() {
        Boolean value = muted.getValue();
        return value != null && value;
    }

    public void setMuted(boolean value) {
        prefs.edit().putBoolean(KEY_MUTED, value).apply();
        muted.setValue(value);
    }

    public boolean hasWelcomed() {
        return prefs.getBoolean(KEY_WELCOMED, false);
    }

    public void setWelcomed(boolean value) {
        prefs.edit().putBoolean(KEY_WELCOMED, value).apply();
    }

    public int difficulty() {
        return prefs.getInt(KEY_DIFFICULTY, DIFFICULTY_MEDIUM);
    }

    public void setDifficulty(int value) {
        prefs.edit().putInt(KEY_DIFFICULTY, value).apply();
    }

    public boolean soundEnabled() {
        return prefs.getBoolean(KEY_SOUND, true);
    }

    public void setSoundEnabled(boolean value) {
        prefs.edit().putBoolean(KEY_SOUND, value).apply();
    }

    /** Highest memory level the child has finished in a deck, -1 for none. */
    public int memoryProgress(String deckId) {
        return prefs.getInt("memory_level_" + deckId, -1);
    }

    public void setMemoryProgress(String deckId, int level) {
        if (level > memoryProgress(deckId)) {
            prefs.edit().putInt("memory_level_" + deckId, level).apply();
        }
    }

    /** Coloring pages already completed, keyed "packId/pageId". */
    public boolean isColoringDone(String key) {
        return prefs.getBoolean("coloring_done_" + key, false);
    }

    public void setColoringDone(String key) {
        prefs.edit().putBoolean("coloring_done_" + key, true).apply();
    }

    public int coloringDoneInPack(String packId, int pageCount) {
        int done = 0;
        for (int i = 0; i < pageCount; i++) {
            if (isColoringDone(packId + "/" + i)) {
                done++;
            }
        }
        return done;
    }

    /** Glyphs the child has finished tracing, keyed by the character itself. */
    public boolean isTraceDone(String glyph) {
        return prefs.getBoolean("trace_done_" + glyph, false);
    }

    public void setTraceDone(String glyph) {
        prefs.edit().putBoolean("trace_done_" + glyph, true).apply();
    }

    /** Stories the child has reached the end of, keyed by the story's id. */
    public boolean isStoryDone(String storyId) {
        return prefs.getBoolean("story_done_" + storyId, false);
    }

    public void setStoryDone(String storyId) {
        prefs.edit().putBoolean("story_done_" + storyId, true).apply();
    }

    /** Told tales the child has heard all the way through. */
    public boolean isTaleDone(String taleId) {
        return prefs.getBoolean("tale_done_" + taleId, false);
    }

    public void setTaleDone(String taleId) {
        prefs.edit().putBoolean("tale_done_" + taleId, true).apply();
    }

    /** Bedtime settings, remembered so the evening routine is not re-set nightly. */
    public boolean lullabyRepeat() {
        return prefs.getBoolean("lullaby_repeat", false);
    }

    public void setLullabyRepeat(boolean value) {
        prefs.edit().putBoolean("lullaby_repeat", value).apply();
    }

    public boolean lullabyAutoNext() {
        return prefs.getBoolean("lullaby_auto", true);
    }

    public void setLullabyAutoNext(boolean value) {
        prefs.edit().putBoolean("lullaby_auto", value).apply();
    }

    /** Sleep-timer length in minutes; 0 means the timer is off. */
    public int lullabySleepMinutes() {
        return prefs.getInt("lullaby_sleep", 0);
    }

    public void setLullabySleepMinutes(int value) {
        prefs.edit().putInt("lullaby_sleep", value).apply();
    }

    /**
     * On unless a grown-up turns it off.
     *
     * <p>It used to default to off, which meant the app opened in silence on every
     * fresh install — no music, whatever was in res/raw and whatever the synthesised
     * loop could have played. Sound effects and speech were already on by default;
     * this was the odd one out, and it was not a deliberate choice.
     */
    public boolean musicEnabled() {
        return prefs.getBoolean(KEY_MUSIC, true);
    }

    public void setMusicEnabled(boolean value) {
        prefs.edit().putBoolean(KEY_MUSIC, value).apply();
    }
}
