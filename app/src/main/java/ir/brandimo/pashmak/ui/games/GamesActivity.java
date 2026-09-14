package ir.brandimo.pashmak.ui.games;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.databinding.ActivityGamesBinding;
import ir.brandimo.pashmak.databinding.ViewGameCardBinding;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.ui.bubbles.BubblePopActivity;
import ir.brandimo.pashmak.ui.coloring.ColoringActivity;
import ir.brandimo.pashmak.ui.freedraw.FreeDrawActivity;
import ir.brandimo.pashmak.ui.memory.MemoryActivity;
import ir.brandimo.pashmak.ui.missions.MissionsActivity;
import ir.brandimo.pashmak.ui.parentgate.ParentGateDialog;
import ir.brandimo.pashmak.ui.story.StoryActivity;
import ir.brandimo.pashmak.ui.tracing.TracingActivity;

/** The games menu: every game as a card with its own icon and colour. */
public class GamesActivity extends BaseActivity {

    private ActivityGamesBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGamesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.gamesHeader.headerTitle.setText(R.string.games_title);
        binding.gamesHeader.headerTitle.setTextColor(
                ContextCompat.getColor(this, R.color.red));
        binding.gamesHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        bindStars(binding.gamesHeader.headerStarsValue);
        attachCompanion();

        String name = getString(R.string.mascot_name);

        card(binding.gamesLive, R.drawable.btn_orange_card, R.drawable.ic_camera, R.color.orange,
                R.string.game_live_title, getString(R.string.game_live_sub, name),
                v -> open(MissionsActivity.class));

        card(binding.gamesPaint, R.drawable.btn_purple_card, R.drawable.ic_palette, R.color.purple,
                R.string.game_paint_title, getString(R.string.game_paint_sub),
                v -> open(ColoringActivity.class));

        card(binding.gamesFreedraw, R.drawable.btn_cyan, R.drawable.ic_brush, R.color.cyan,
                R.string.game_freedraw_title, getString(R.string.game_freedraw_sub),
                v -> open(FreeDrawActivity.class));

        card(binding.gamesTrace, R.drawable.btn_green, R.drawable.ic_pencil, R.color.green,
                R.string.game_trace_title, getString(R.string.game_trace_sub),
                v -> open(TracingActivity.class));

        card(binding.gamesBubbles, R.drawable.btn_blue_deep, R.drawable.ic_bubbles, R.color.blue_deep,
                R.string.game_bubbles_title, getString(R.string.game_bubbles_sub),
                v -> open(BubblePopActivity.class));

        card(binding.gamesMemory, R.drawable.btn_red, R.drawable.ic_memory_cards, R.color.red,
                R.string.game_memory_title, getString(R.string.game_memory_sub),
                v -> open(MemoryActivity.class));

        card(binding.gamesStory, R.drawable.btn_yellow, R.drawable.ic_book, R.color.yellow_shadow,
                R.string.game_story_title, getString(R.string.game_story_sub),
                v -> open(StoryActivity.class));

        // The story card is pale, so its text needs the dark ink instead of white.
        int gold = ContextCompat.getColor(this, R.color.gold_ink);
        binding.gamesStory.cardTitle.setTextColor(gold);
        binding.gamesStory.cardSub.setTextColor(gold);

        binding.gamesSettings.setOnClickListener(v -> {
            tap();
            ParentGateDialog.show(this);
        });
    }

    private void card(ViewGameCardBinding card, @DrawableRes int background,
                      @DrawableRes int icon, @ColorRes int iconTint,
                      @StringRes int title, String subtitle,
                      View.OnClickListener click) {
        card.cardRoot.setBackgroundResource(background);
        card.cardIcon.setImageResource(icon);
        card.cardIcon.setColorFilter(ContextCompat.getColor(this, iconTint));
        card.cardTitle.setText(title);
        card.cardSub.setText(subtitle);
        card.cardRoot.setOnClickListener(click);
    }
}
