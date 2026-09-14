package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.DrawableRes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A complete story: a title, a cover picture, and the beats that make it up. */
public final class Story {

    public final String id;
    public final String title;
    @DrawableRes
    public final int badge;
    public final List<StoryBeat> beats;

    Story(String id, String title, @DrawableRes int badge, StoryBeat... beats) {
        this.id = id;
        this.title = title;
        this.badge = badge;
        this.beats = Collections.unmodifiableList(Arrays.asList(beats));
    }

    public int size() {
        return beats.size();
    }

    public StoryBeat beat(int index) {
        return beats.get(Math.max(0, Math.min(index, beats.size() - 1)));
    }
}
