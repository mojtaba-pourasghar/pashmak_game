package ir.brandimo.pashmak.mascot;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;

import ir.brandimo.pashmak.R;

/**
 * The floating bubble above the companion: typewriter text plus a mute toggle.
 * The text is set from outside, one character at a time, by MascotController.
 */
public class SpeechBubbleView extends LinearLayout {

    private AppCompatTextView textView;
    private AppCompatImageButton audioButton;
    @Nullable
    private Runnable muteListener;

    public SpeechBubbleView(Context context) {
        this(context, null);
    }

    public SpeechBubbleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.TOP);
        LayoutInflater.from(context).inflate(R.layout.view_speech_bubble, this, true);
        textView = findViewById(R.id.bubble_text);
        audioButton = findViewById(R.id.bubble_audio);
        audioButton.setOnClickListener(v -> {
            if (muteListener != null) {
                muteListener.run();
            }
        });
    }

    public void setOnMuteToggled(@Nullable Runnable listener) {
        muteListener = listener;
    }

    public void bind(MascotUiState state) {
        if (state == null || !state.bubbleVisible || state.fullText.isEmpty()) {
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);
        textView.setText(state.typedText);
        audioButton.setActivated(!state.muted);
        audioButton.setImageResource(state.muted
                ? R.drawable.ic_volume_off : R.drawable.ic_volume_on);
        audioButton.setBackgroundResource(state.muted
                ? R.drawable.bg_circle_audio_off : R.drawable.bg_circle_orange);
        audioButton.setContentDescription(getContext().getString(R.string.sound_toggle));
    }

    @Override
    public void setVisibility(int visibility) {
        if (getVisibility() != visibility) {
            super.setVisibility(visibility);
            if (visibility == VISIBLE) {
                setAlpha(0f);
                setScaleX(0.9f);
                setScaleY(0.9f);
                animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160L).start();
            }
        }
    }

    public View textView() {
        return textView;
    }
}
