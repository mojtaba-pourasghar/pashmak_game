package ir.brandimo.pashmak.ui.parentgate;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.databinding.DialogParentGateBinding;
import ir.brandimo.pashmak.ui.settings.SettingsActivity;
import ir.brandimo.pashmak.util.FaNum;
import ir.brandimo.pashmak.util.PersianWords;

/**
 * Keeps small hands out of the settings. The challenge is written in Persian
 * words rather than digits, so it reads as arithmetic to an adult and as
 * nothing at all to a pre-reader.
 */
public class ParentGateDialog extends Dialog {

    private static final int MAX_DIGITS = 3;

    private DialogParentGateBinding binding;
    private final Random random = new Random();
    private int answer;
    private String input = "";

    public static void show(@NonNull Context context) {
        new ParentGateDialog(context).show();
    }

    private ParentGateDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogParentGateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }

        newChallenge();
        wireKeypad();
        binding.gateClose.setOnClickListener(v -> dismiss());
    }

    /** Multiplication when it stays small, addition otherwise. */
    private void newChallenge() {
        boolean multiply = random.nextBoolean();
        int a;
        int b;
        if (multiply) {
            a = 3 + random.nextInt(7);
            b = 3 + random.nextInt(7);
            answer = a * b;
        } else {
            a = 11 + random.nextInt(28);
            b = 11 + random.nextInt(28);
            answer = a + b;
        }
        String operator = getContext().getString(
                multiply ? R.string.lock_times : R.string.lock_plus);
        String question = PersianWords.of(a) + " " + operator + " " + PersianWords.of(b);
        binding.gateQuestion.setText(getContext().getString(R.string.lock_question, question));
        input = "";
        renderInput();
    }

    private void wireKeypad() {
        View.OnClickListener digit = v -> {
            Object tag = v.getTag();
            if (tag == null) {
                return;
            }
            if (input.length() < MAX_DIGITS) {
                input = input + tag;
            }
            binding.gateHint.setText(R.string.lock_hint);
            renderInput();
        };
        binding.key0.setOnClickListener(digit);
        binding.key1.setOnClickListener(digit);
        binding.key2.setOnClickListener(digit);
        binding.key3.setOnClickListener(digit);
        binding.key4.setOnClickListener(digit);
        binding.key5.setOnClickListener(digit);
        binding.key6.setOnClickListener(digit);
        binding.key7.setOnClickListener(digit);
        binding.key8.setOnClickListener(digit);
        binding.key9.setOnClickListener(digit);

        binding.keyBackspace.setOnClickListener(v -> {
            if (!input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
            }
            renderInput();
        });
        binding.keyConfirm.setOnClickListener(v -> submit());
    }

    private void renderInput() {
        binding.gateInput.setText(input.isEmpty()
                ? getContext().getString(R.string.lock_empty)
                : FaNum.of(input));
    }

    private void submit() {
        int entered;
        try {
            entered = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            entered = -1;
        }
        if (entered == answer) {
            getContext().startActivity(new Intent(getContext(), SettingsActivity.class));
            dismiss();
        } else {
            binding.gateHint.setText(R.string.lock_error);
            newChallenge();
        }
    }
}
