package ir.brandimo.pashmak.audio;

/**
 * Every sound the app will play, by resource name. Nothing here has to exist:
 * lookups go through Resources#getIdentifier and a missing file is a silent
 * no-op, so the app builds and runs with an empty res/raw and lights up the
 * moment real recordings are dropped in.
 */
public final class AudioManifest {

    /* Mascot voice lines (MediaPlayer). */
    public static final String VOICE_WELCOME = "welcome";
    public static final String VOICE_WIN_1 = "win1";
    public static final String VOICE_WIN_2 = "win2";
    public static final String VOICE_WIN_3 = "win3";
    public static final String VOICE_TRY_1 = "try_again1";
    public static final String VOICE_TRY_2 = "try_again2";
    public static final String VOICE_GIGGLE = "giggle";
    public static final String VOICE_POKE = "poke";

    /* Short effects (SoundPool). */
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

    /* Background music (MediaPlayer, looped). */
    public static final String BGM_MENU = "bgm_menu";
    public static final String BGM_PLAY = "bgm_play";

    /** Loaded into SoundPool up front so the first tap is not late. */
    public static final String[] PRELOAD_SFX = {
            SFX_POP, SFX_FLIP, SFX_MATCH, SFX_STAR, SFX_SHUTTER,
            SFX_WRONG, SFX_BRUSH, SFX_TAP, SFX_WHOOSH, SFX_FANFARE
    };

    /** Spoken letter and digit names: letter_alef.mp3, digit_5.mp3 and so on. */
    public static String letterVoice(String glyph) {
        return "letter_" + Integer.toHexString(glyph.codePointAt(0));
    }

    public static String digitVoice(int digit) {
        return "digit_" + digit;
    }

    private AudioManifest() {
    }
}
