package ir.brandimo.pashmak.audio;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.data.catalog.Lullaby;
import ir.brandimo.pashmak.data.catalog.LullabyCatalog;
import ir.brandimo.pashmak.data.catalog.Story;
import ir.brandimo.pashmak.data.catalog.StoryBeat;
import ir.brandimo.pashmak.data.catalog.StoryCatalog;

/**
 * Counts how many of the recordings listed in res/raw/audio_manifest.txt are
 * actually present. Whoever is recording them has no other way to tell whether a
 * file landed under the right name, so the parent screen reports the tally.
 */
public final class AudioInventory {

    /** How many of the expected clips exist, and the first few that do not. */
    public static final class Report {
        public final int found;
        public final int total;
        public final List<String> missingSample;

        Report(int found, int total, List<String> missingSample) {
            this.found = found;
            this.total = total;
            this.missingSample = missingSample;
        }
    }

    private static final int SAMPLE_SIZE = 6;

    public static Report scan(@NonNull Context context) {
        Context app = context.getApplicationContext();
        List<String> expected = expected(app);
        Resources resources = app.getResources();
        String pkg = app.getPackageName();
        int found = 0;
        List<String> missing = new ArrayList<>();
        for (String name : expected) {
            if (resources.getIdentifier(name, "raw", pkg) != 0) {
                found++;
            } else if (missing.size() < SAMPLE_SIZE) {
                missing.add(name);
            }
        }
        return new Report(found, expected.size(), missing);
    }

    /** Every file name the app will ever ask res/raw for. */
    private static List<String> expected(Context app) {
        List<String> names = new ArrayList<>();

        names.add(AudioManifest.BGM_MENU);
        names.add(AudioManifest.BGM_PLAY);
        names.add(AudioManifest.BGM_STORY);

        names.add(AudioManifest.VOICE_WELCOME);
        names.add(AudioManifest.VOICE_WIN_1);
        names.add(AudioManifest.VOICE_WIN_2);
        names.add(AudioManifest.VOICE_WIN_3);
        names.add(AudioManifest.VOICE_TRY_1);
        names.add(AudioManifest.VOICE_TRY_2);
        names.add(AudioManifest.VOICE_GIGGLE);
        names.add(AudioManifest.VOICE_POKE);

        names.add(AudioManifest.VOICE_HELP_DEFAULT);
        names.add(AudioManifest.VOICE_HELP_PAINT);
        names.add(AudioManifest.VOICE_HELP_TRACE);
        names.add(AudioManifest.VOICE_HELP_MISSION);

        names.add(AudioManifest.VOICE_MISSION_START);
        names.add(AudioManifest.VOICE_ITEM_ARRIVED);
        names.add(AudioManifest.VOICE_MISSION_DONE);

        names.add(AudioManifest.VOICE_PAINT_RIGHT);
        names.add(AudioManifest.VOICE_PAINT_WRONG);
        names.add(AudioManifest.VOICE_PAINT_DONE);

        names.add(AudioManifest.VOICE_TRACE_DONE);
        names.add(AudioManifest.VOICE_TRACE_MORE);

        names.add(AudioManifest.VOICE_MEMORY_MATCH);
        names.add(AudioManifest.VOICE_MEMORY_MISS);
        names.add(AudioManifest.VOICE_MEMORY_WIN);
        names.add(AudioManifest.VOICE_MEMORY_NEXT);

        names.add(AudioManifest.VOICE_BUBBLE_POP);
        names.add(AudioManifest.VOICE_BUBBLE_GOAL);
        names.add(AudioManifest.VOICE_BUBBLE_WRONG);
        names.add(AudioManifest.VOICE_BUBBLE_ROUND);
        names.add(AudioManifest.VOICE_DRAW_EMPTY);
        names.add(AudioManifest.VOICE_STAGE_LOCKED);

        names.add(AudioManifest.VOICE_STORY_WRONG);
        names.add(AudioManifest.VOICE_STORY_END);

        names.add(AudioManifest.VOICE_NIGHT_HELLO);
        names.add(AudioManifest.VOICE_NIGHT_GOODNIGHT);
        names.add(AudioManifest.VOICE_NIGHT_MISSING);

        for (String sfx : AudioManifest.PRELOAD_SFX) {
            names.add(sfx);
        }

        for (String glyph : app.getResources().getStringArray(R.array.trace_letters)) {
            names.add(AudioManifest.letterVoice(glyph));
        }
        for (int digit = 0; digit <= 9; digit++) {
            names.add(AudioManifest.digitVoice(digit));
        }

        for (Lullaby lullaby : LullabyCatalog.all()) {
            names.add(lullaby.clip);
        }

        for (Story story : StoryCatalog.all()) {
            for (int beat = 0; beat < story.size(); beat++) {
                names.add(AudioManifest.storyBeat(story.id, beat));
                if (story.beat(beat).type == StoryBeat.Type.ASK_TAP) {
                    names.add(AudioManifest.storyPraise(story.id, beat));
                }
            }
        }

        return names;
    }

    private AudioInventory() {
    }
}
