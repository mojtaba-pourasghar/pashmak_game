package ir.brandimo.pashmak.mascot;

import androidx.annotation.Nullable;

/** One thing the mascot can say: the words, the pose, and the clip to play. */
public final class MascotLine {

    public final String text;
    @Nullable
    public final String audio;
    public final MascotState mood;

    public MascotLine(String text, @Nullable String audio, MascotState mood) {
        this.text = text == null ? "" : text;
        this.audio = audio;
        this.mood = mood == null ? MascotState.TALK : mood;
    }
}
