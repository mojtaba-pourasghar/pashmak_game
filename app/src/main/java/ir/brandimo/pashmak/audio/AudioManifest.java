package ir.brandimo.pashmak.audio;

/**
 * Every sound the app can play, by resource name. Nothing here has to exist:
 * lookups go through Resources#getIdentifier and a missing file is a silent
 * no-op, so the app builds and runs with an empty res/raw and lights up the
 * moment real recordings are dropped in.
 *
 * <p>Every line the mascot speaks names a clip here — {@code MascotController}
 * has no way to say something anonymously — so this file, together with
 * {@code res/raw/audio_manifest.txt}, is the complete recording list.
 */
public final class AudioManifest {

    /* ---- Mascot voice: greeting and the reaction buckets (MediaPlayer) ---- */
    public static final String VOICE_WELCOME = "welcome";
    public static final String VOICE_WIN_1 = "win1";
    public static final String VOICE_WIN_2 = "win2";
    public static final String VOICE_WIN_3 = "win3";
    public static final String VOICE_TRY_1 = "try_again1";
    public static final String VOICE_TRY_2 = "try_again2";
    public static final String VOICE_GIGGLE = "giggle";
    public static final String VOICE_POKE = "poke";

    /* ---- Screen hints ---- */
    public static final String VOICE_HELP_DEFAULT = "help_default";
    public static final String VOICE_HELP_PAINT = "help_paint";
    public static final String VOICE_HELP_TRACE = "help_trace";
    public static final String VOICE_HELP_MISSION = "help_mission";

    /* ---- Live drawing (نقاشی زنده) ---- */
    public static final String VOICE_MISSION_START = "mission_start";
    public static final String VOICE_ITEM_ARRIVED = "item_arrived";
    public static final String VOICE_MISSION_DONE = "mission_done";

    /* ---- Coloring ---- */
    public static final String VOICE_PAINT_RIGHT = "paint_right";
    public static final String VOICE_PAINT_WRONG = "paint_wrong";
    public static final String VOICE_PAINT_DONE = "paint_done";

    /* ---- Tracing ---- */
    public static final String VOICE_TRACE_DONE = "trace_done";
    public static final String VOICE_TRACE_MORE = "trace_more";

    /* ---- Memory ---- */
    public static final String VOICE_MEMORY_MATCH = "memory_match";
    public static final String VOICE_MEMORY_MISS = "memory_miss";
    public static final String VOICE_MEMORY_WIN = "memory_win";
    public static final String VOICE_MEMORY_NEXT = "memory_next";

    /* ---- Bubbles and free drawing ---- */
    public static final String VOICE_BUBBLE_POP = "bubble_pop";
    public static final String VOICE_DRAW_EMPTY = "draw_empty";

    /* ---- Stories ---- */
    public static final String VOICE_STORY_WRONG = "story_wrong";
    public static final String VOICE_STORY_END = "story_end";

    /* ---- Stage lists ---- */
    public static final String VOICE_STAGE_LOCKED = "stage_locked";

    /* ---- Bedtime ---- */
    public static final String VOICE_NIGHT_HELLO = "night_hello";
    public static final String VOICE_NIGHT_GOODNIGHT = "night_goodnight";
    public static final String VOICE_NIGHT_MISSING = "night_missing";

    /* ---- Short effects (SoundPool) ---- */
    public static final String SFX_POP = "sfx_pop";
    public static final String SFX_FLIP = "sfx_flip";
    public static final String SFX_MATCH = "sfx_match";
    public static final String SFX_STAR = "sfx_star";
    public static final String SFX_SHUTTER = "sfx_shutter";
    public static final String SFX_WRONG = "sfx_wrong";
    public static final String SFX_BRUSH = "sfx_brush";
    public static final String SFX_TAP = "sfx_tap";
    public static final String SFX_WHOOSH = "sfx_whoosh";
    public static final String SFX_FANFARE = "sfx_fanfare";

    /* ---- Background music (MediaPlayer, looped) ---- */
    /** Splash, home, the mission list, the games menu, the gallery, settings. */
    public static final String BGM_MENU = "bgm_menu";
    /** Every mini-game and the whole live-drawing flow. */
    public static final String BGM_PLAY = "bgm_play";
    /** The story screen, which wants something softer than the games loop. */
    public static final String BGM_STORY = "bgm_story";

    /** Loaded into SoundPool up front so the first tap is not late. */
    public static final String[] PRELOAD_SFX = {
            SFX_POP, SFX_FLIP, SFX_MATCH, SFX_STAR, SFX_SHUTTER,
            SFX_WRONG, SFX_BRUSH, SFX_TAP, SFX_WHOOSH, SFX_FANFARE
    };

    /** Spoken letter and digit names: letter_627.mp3, digit_5.mp3 and so on. */
    public static String letterVoice(String glyph) {
        return "letter_" + Integer.toHexString(glyph.codePointAt(0));
    }

    public static String digitVoice(int digit) {
        return "digit_" + digit;
    }

    /**
     * One clip per story beat — story_magic_0, story_ball_3 … — so a story can be
     * narrated in a real voice without the catalog carrying file names around.
     */
    public static String storyBeat(String storyId, int beat) {
        return "story_" + storyId + "_" + beat;
    }

    /** What Pashmak says when the child taps the right thing in that beat. */
    public static String storyPraise(String storyId, int beat) {
        return "story_" + storyId + "_" + beat + "_yes";
    }

    private AudioManifest() {
    }
}
