package ir.brandimo.pashmak.mascot;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Random;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;

/**
 * The Persian dialogue buckets from the design's MascotConfig, including its
 * no-immediate-repeat picker so the child never hears the same line twice running.
 */
public final class MascotDialogues {

    public static final int BUCKET_SUCCESS = 0;
    public static final int BUCKET_ENCOURAGE = 1;
    public static final int BUCKET_POKE = 2;
    private static final int BUCKET_COUNT = 3;

    private final Context appContext;
    private final Random random = new Random();
    private final int[] lastPicked = new int[BUCKET_COUNT];

    public MascotDialogues(@NonNull Context context) {
        appContext = context.getApplicationContext();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            lastPicked[i] = -1;
        }
    }

    public String mascotName() {
        return appContext.getString(R.string.mascot_name);
    }

    public MascotLine welcome() {
        return new MascotLine(
                appContext.getString(R.string.ms_welcome, mascotName()),
                AudioManifest.VOICE_WELCOME,
                MascotState.WAVE);
    }

    public MascotLine pick(int bucket) {
        MascotLine[] lines = bucketLines(bucket);
        if (lines.length == 0) {
            return new MascotLine("", null, MascotState.IDLE);
        }
        int index = lines.length == 1 ? 0 : random.nextInt(lines.length);
        if (lines.length > 1 && index == lastPicked[bucket]) {
            index = (index + 1) % lines.length;
        }
        lastPicked[bucket] = index;
        return lines[index];
    }

    private MascotLine[] bucketLines(int bucket) {
        switch (bucket) {
            case BUCKET_SUCCESS:
                return new MascotLine[]{
                        new MascotLine(appContext.getString(R.string.ms_win_1),
                                AudioManifest.VOICE_WIN_1, MascotState.CHEER),
                        new MascotLine(appContext.getString(R.string.ms_win_2),
                                AudioManifest.VOICE_WIN_2, MascotState.CHEER),
                        new MascotLine(appContext.getString(R.string.ms_win_3),
                                AudioManifest.VOICE_WIN_3, MascotState.CHEER)
                };
            case BUCKET_ENCOURAGE:
                return new MascotLine[]{
                        new MascotLine(appContext.getString(R.string.ms_try_1),
                                AudioManifest.VOICE_TRY_1, MascotState.ENCOURAGE),
                        new MascotLine(appContext.getString(R.string.ms_try_2),
                                AudioManifest.VOICE_TRY_2, MascotState.ENCOURAGE)
                };
            case BUCKET_POKE:
            default:
                return new MascotLine[]{
                        new MascotLine(appContext.getString(R.string.ms_poke_1),
                                AudioManifest.VOICE_GIGGLE, MascotState.TICKLE),
                        new MascotLine(appContext.getString(R.string.ms_poke_2),
                                AudioManifest.VOICE_POKE, MascotState.TICKLE)
                };
        }
    }

    /** Contextual hints, keyed by screen. Each one carries its own clip. */
    public MascotLine help(String key, String... args) {
        if ("paint".equals(key)) {
            return new MascotLine(appContext.getString(R.string.ms_help_paint),
                    AudioManifest.VOICE_HELP_PAINT, MascotState.TALK);
        }
        if ("trace".equals(key)) {
            return new MascotLine(appContext.getString(R.string.ms_help_trace),
                    AudioManifest.VOICE_HELP_TRACE, MascotState.TALK);
        }
        if ("mission".equals(key) && args.length >= 2) {
            return new MascotLine(
                    appContext.getString(R.string.ms_help_mission, args[0], args[1]),
                    AudioManifest.VOICE_HELP_MISSION, MascotState.TALK);
        }
        return new MascotLine(appContext.getString(R.string.ms_help_default),
                AudioManifest.VOICE_HELP_DEFAULT, MascotState.TALK);
    }
}
