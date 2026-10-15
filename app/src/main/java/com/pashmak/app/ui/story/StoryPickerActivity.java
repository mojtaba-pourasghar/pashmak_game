package com.pashmak.app.ui.story;

import android.content.Intent;

import androidx.annotation.ColorRes;

import java.util.ArrayList;
import java.util.List;

import com.pashmak.app.R;
import com.pashmak.app.data.catalog.Story;
import com.pashmak.app.data.catalog.StoryCatalog;
import com.pashmak.app.ui.common.Stage;
import com.pashmak.app.ui.common.StagePickerActivity;
import com.pashmak.app.util.FaNum;

/** Twenty stories, each a named stage, opening in order as the child finishes them. */
public class StoryPickerActivity extends StagePickerActivity {

    @Override
    protected String screenTitle() {
        return getString(R.string.game_story_title);
    }

    @Override
    protected String screenHint() {
        return getString(R.string.stage_pick_hint);
    }

    @Override
    @ColorRes
    protected int accentColor() {
        return R.color.yellow_shadow;
    }

    @Override
    protected List<Stage> buildStages() {
        List<Stage> stages = new ArrayList<>();
        int furthest = 0;
        for (int i = 0; i < StoryCatalog.count(); i++) {
            if (prefs.isStoryDone(StoryCatalog.story(i).id)) {
                furthest = i + 1;
            }
        }
        for (int i = 0; i < StoryCatalog.count(); i++) {
            Story story = StoryCatalog.story(i);
            boolean done = prefs.isStoryDone(story.id);
            stages.add(new Stage(i, story.title,
                    getString(R.string.stage_story_beats, FaNum.of(story.size())), "",
                    done, i > furthest, story.badge, null));
        }
        return stages;
    }

    @Override
    protected void onStageChosen(Stage stage) {
        Intent intent = new Intent(this, StoryActivity.class);
        intent.putExtra(StoryActivity.EXTRA_STORY, stage.index);
        startActivity(intent);
    }
}
