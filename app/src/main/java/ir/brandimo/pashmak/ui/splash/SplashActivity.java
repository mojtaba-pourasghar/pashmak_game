package ir.brandimo.pashmak.ui.splash;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.databinding.ActivitySplashBinding;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.ui.home.HomeActivity;

/**
 * Holds for 3.2 seconds while the character springs in and waves, matching the
 * prototype's timing, then hands over to home.
 */
public class SplashActivity extends BaseActivity {

    private static final long WAVE_AT_MS = 820L;
    private static final long HAND_OFF_MS = 3200L;

    private ActivitySplashBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable handOff = this::goHome;
    private boolean handedOver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.splashTagline.setText(
                getString(R.string.splash_tagline, getString(R.string.mascot_name)));
        binding.splashMascot.setState(MascotState.ENTER);
        binding.getRoot().setOnClickListener(v -> goHome());

        animateProgress();
        handler.postDelayed(() -> binding.splashMascot.setState(MascotState.WAVE), WAVE_AT_MS);
        handler.postDelayed(handOff, HAND_OFF_MS);
    }

    private void animateProgress() {
        ValueAnimator animator = ValueAnimator.ofInt(6, 100);
        animator.setDuration(HAND_OFF_MS);
        animator.addUpdateListener(value ->
                binding.splashProgress.setProgress((Integer) value.getAnimatedValue()));
        animator.start();
    }

    private void goHome() {
        if (handedOver) {
            return;
        }
        handedOver = true;
        handler.removeCallbacks(handOff);
        openAndFinish(HomeActivity.class);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
