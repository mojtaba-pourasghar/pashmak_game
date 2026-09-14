package ir.brandimo.pashmak.ui.story;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.catalog.Story;
import ir.brandimo.pashmak.data.catalog.StoryBeat;
import ir.brandimo.pashmak.data.catalog.StoryCatalog;
import ir.brandimo.pashmak.data.catalog.StoryProp;
import ir.brandimo.pashmak.databinding.ActivityStoryBinding;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.GameActivity;

/**
 * Pashmak tells a story and the child helps: finding things in the picture and
 * choosing where the story goes next. A wrong tap is never a failure — he just
 * asks again.
 */
public class StoryActivity extends GameActivity {

    private static final int STARS_PER_STORY = 4;

    private ActivityStoryBinding binding;
    private StoryListAdapter storyAdapter;
    private Story story;
    private int beatIndex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.storyHeader.headerTitle.setTextColor(
                ContextCompat.getColor(this, R.color.gold_ink));
        binding.storyHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        binding.storyHeader.headerAction.setVisibility(View.VISIBLE);
        binding.storyHeader.headerAction.setText(R.string.story_again);
        binding.storyHeader.headerAction.setTextColor(
                ContextCompat.getColor(this, R.color.gold_ink));
        binding.storyHeader.headerAction.setOnClickListener(v -> {
            tap();
            openStory(StoryCatalog.all().indexOf(story));
        });
        bindStars(binding.storyHeader.headerStarsValue);
        attachCompanion();

        storyAdapter = new StoryListAdapter(StoryCatalog.all(), index -> {
            tap();
            openStory(index);
        });
        binding.storyList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.storyList.setAdapter(storyAdapter);

        binding.storyScene.setOnPropTapped(this::onPropTapped);
        binding.storyNext.setOnClickListener(v -> {
            tap();
            advance(currentBeat().next);
        });

        openStory(0);
    }

    private void openStory(int index) {
        story = StoryCatalog.story(Math.max(0, index));
        storyAdapter.setSelected(StoryCatalog.all().indexOf(story));
        binding.storyHeader.headerTitle.setText(story.title);
        beatIndex = 0;
        renderBeat();
    }

    private StoryBeat currentBeat() {
        return story.beat(beatIndex);
    }

    private void renderBeat() {
        StoryBeat beat = currentBeat();
        binding.storyLine.setText(beat.text);
        binding.storyScene.setProps(beat.props);
        binding.storyScene.setInteractive(beat.type == StoryBeat.Type.ASK_TAP);

        boolean choosing = beat.type == StoryBeat.Type.CHOICE;
        binding.storyChoices.setVisibility(choosing ? View.VISIBLE : View.GONE);
        if (choosing) {
            binding.storyChoiceA.setText(beat.optionA);
            binding.storyChoiceB.setText(beat.optionB);
            binding.storyChoiceA.setOnClickListener(v -> {
                tap();
                advance(beat.targetA);
            });
            binding.storyChoiceB.setOnClickListener(v -> {
                tap();
                advance(beat.targetB);
            });
        }

        boolean waitingForChild = choosing || beat.type == StoryBeat.Type.ASK_TAP;
        boolean ended = beat.type == StoryBeat.Type.CELEBRATE;
        binding.storyNext.setVisibility(waitingForChild || ended ? View.GONE : View.VISIBLE);

        // Pashmak narrates every beat, so his mouth moves with the words.
        mascot.say(beat.text, MascotState.TALK, MascotController.HOLD_DEFAULT_MS);

        if (ended) {
            celebrate();
        }
    }

    private void onPropTapped(StoryProp prop) {
        StoryBeat beat = currentBeat();
        if (beat.type != StoryBeat.Type.ASK_TAP) {
            return;
        }
        if (prop.id.equals(beat.answerPropId)) {
            sounds.play(AudioManifest.SFX_MATCH);
            binding.storyScene.bounce(prop.id);
            mascot.addStars(1);
            String praise = beat.praise == null ? "" : beat.praise;
            mascot.say(praise, MascotState.CHEER, MascotController.HOLD_CHEER_MS);
            binding.storyScene.setInteractive(false);
            binding.getRoot().postDelayed(() -> advance(beat.next), 1500L);
        } else {
            onWrong(getString(R.string.story_wrong));
        }
    }

    private void advance(int explicitNext) {
        int next = explicitNext >= 0 ? explicitNext : beatIndex + 1;
        if (next >= story.size()) {
            mascot.say(getString(R.string.story_finished), MascotState.TALK,
                    MascotController.HOLD_DEFAULT_MS);
            return;
        }
        beatIndex = next;
        renderBeat();
    }

    private void celebrate() {
        sounds.play(AudioManifest.SFX_FANFARE);
        binding.storyConfetti.burst();
        mascot.addStars(STARS_PER_STORY);
        binding.getRoot().postDelayed(() -> mascot.say(currentBeat().text,
                MascotState.CHEER, MascotController.HOLD_CHEER_MS), 400L);
    }
}
