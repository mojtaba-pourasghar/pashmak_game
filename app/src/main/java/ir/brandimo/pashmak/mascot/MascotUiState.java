package ir.brandimo.pashmak.mascot;

import androidx.annotation.NonNull;

/** Immutable snapshot every mascot-aware screen observes. */
public final class MascotUiState {

    public final MascotState mood;
    public final String fullText;
    public final String typedText;
    public final boolean bubbleVisible;
    public final boolean muted;

    MascotUiState(@NonNull MascotState mood, String fullText, String typedText,
                  boolean bubbleVisible, boolean muted) {
        this.mood = mood;
        this.fullText = fullText == null ? "" : fullText;
        this.typedText = typedText == null ? "" : typedText;
        this.bubbleVisible = bubbleVisible;
        this.muted = muted;
    }

    MascotUiState withTyped(String typed) {
        return new MascotUiState(mood, fullText, typed, bubbleVisible, muted);
    }

    MascotUiState withMuted(boolean value) {
        return new MascotUiState(mood, fullText, typedText, bubbleVisible, value);
    }

    static MascotUiState idle(boolean muted) {
        return new MascotUiState(MascotState.IDLE, "", "", false, muted);
    }
}
