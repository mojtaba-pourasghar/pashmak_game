package ir.brandimo.pashmak.ui.games;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.databinding.ActivityGamesBinding;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.ui.bubbles.BubblePopActivity;
import ir.brandimo.pashmak.ui.coloring.ColoringPickerActivity;
import ir.brandimo.pashmak.ui.freedraw.FreeDrawActivity;
import ir.brandimo.pashmak.ui.lullaby.LullabyActivity;
import ir.brandimo.pashmak.ui.memory.MemoryPickerActivity;
import ir.brandimo.pashmak.ui.missions.MissionsActivity;
import ir.brandimo.pashmak.ui.parentgate.ParentGateDialog;
import ir.brandimo.pashmak.ui.story.StoryActivity;
import ir.brandimo.pashmak.ui.tracing.TracingPickerActivity;

/**
 * The games menu. The headline card spans the row and the other seven fall into
 * rows beneath it, laid out by the grid rather than placed by hand.
 */
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

        final GameAdapter adapter = new GameAdapter(buildEntries(),
                entry -> open(entry.destination));
        int span = getResources().getInteger(R.integer.games_span);
        GridLayoutManager manager = new GridLayoutManager(this, span);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.entryAt(position).wide ? span : 1;
            }
        });
        binding.gamesGrid.setLayoutManager(manager);
        binding.gamesGrid.setAdapter(adapter);

        binding.gamesSettings.setOnClickListener(v -> {
            tap();
            ParentGateDialog.show(this);
        });
    }

    private List<GameEntry> buildEntries() {
        String name = getString(R.string.mascot_name);
        List<GameEntry> entries = new ArrayList<>();

        entries.add(new GameEntry(R.drawable.btn_orange_card, R.drawable.ic_camera,
                R.color.orange, R.string.game_live_title,
                getString(R.string.game_live_sub, name), true, false,
                MissionsActivity.class));

        entries.add(new GameEntry(R.drawable.btn_purple_card, R.drawable.ic_palette,
                R.color.purple, R.string.game_paint_title,
                getString(R.string.game_paint_sub), false, false,
                ColoringPickerActivity.class));

        entries.add(new GameEntry(R.drawable.btn_cyan, R.drawable.ic_brush,
                R.color.cyan, R.string.game_freedraw_title,
                getString(R.string.game_freedraw_sub), false, false,
                FreeDrawActivity.class));

        entries.add(new GameEntry(R.drawable.btn_green, R.drawable.ic_pencil,
                R.color.green, R.string.game_trace_title,
                getString(R.string.game_trace_sub), false, false,
                TracingPickerActivity.class));

        entries.add(new GameEntry(R.drawable.btn_blue_deep, R.drawable.ic_bubbles,
                R.color.blue_deep, R.string.game_bubbles_title,
                getString(R.string.game_bubbles_sub), false, false,
                BubblePopActivity.class));

        entries.add(new GameEntry(R.drawable.btn_red, R.drawable.ic_memory_cards,
                R.color.red, R.string.game_memory_title,
                getString(R.string.game_memory_sub), false, false,
                MemoryPickerActivity.class));

        // The yellow card is pale, so its text goes dark instead of white.
        entries.add(new GameEntry(R.drawable.btn_yellow, R.drawable.ic_book,
                R.color.yellow_shadow, R.string.game_story_title,
                getString(R.string.game_story_sub), false, true,
                StoryActivity.class));

        entries.add(new GameEntry(R.drawable.btn_night_card, R.drawable.ic_moon,
                R.color.night_top, R.string.game_lullaby_title,
                getString(R.string.game_lullaby_sub), false, false,
                LullabyActivity.class));

        return entries;
    }
}
