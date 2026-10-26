package com.pashmak.app.ui.livedrawing;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

import com.pashmak.app.R;
import com.pashmak.app.audio.AudioManifest;
import com.pashmak.app.data.catalog.Mission;
import com.pashmak.app.data.catalog.MissionScene;
import com.pashmak.app.data.catalog.MissionSceneCatalog;
import com.pashmak.app.data.catalog.Palette;
import com.pashmak.app.data.db.CapturedItem;
import com.pashmak.app.data.db.GalleryEntry;
import com.pashmak.app.data.repo.GalleryRepository;
import com.pashmak.app.databinding.ActivityLiveDrawingBinding;
import com.pashmak.app.mascot.MascotController;
import com.pashmak.app.mascot.MascotState;
import com.pashmak.app.ui.base.BaseActivity;
import com.pashmak.app.util.BitmapIO;
import com.pashmak.app.util.FaNum;
import com.pashmak.app.util.ViewCapture;
import com.pashmak.app.vision.FrameDecoder;
import com.pashmak.app.vision.RoiMapper;

/**
 * The Osmo-style loop: draw on real paper, tap the item's card to photograph it,
 * and watch it land in its place in the room beside the mascot. Tapping a card that
 * already has a drawing takes a new photo that replaces it. Once the room is full,
 * the child can save the finished scene to the gallery.
 */
public class LiveDrawingActivity extends BaseActivity {

    public static final String EXTRA_MISSION = "mission_index";
    private static final int REQUEST_CAMERA = 41;
    /** Hold the extracting screen at least this long so the moment lands. */
    private static final long MIN_PROCESSING_MS = 900L;

    private ActivityLiveDrawingBinding binding;
    private LiveDrawingViewModel viewModel;

    private ImageCapture imageCapture;
    private boolean cameraBound;
    private boolean capturing;
    private long processingStartedAt;
    /** Set when a scan lands, so the scene knows which drawing should fly in. */
    private int pendingArrivalSlot = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveDrawingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // The child is holding a drawing up to the camera, not touching the screen.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int missionIndex = getIntent().getIntExtra(EXTRA_MISSION, 0);
        viewModel = new ViewModelProvider(this).get(LiveDrawingViewModel.class);
        viewModel.start(missionIndex);

        attachCompanion();
        wireBrief();
        wireCamera();
        wireProcessing();
        MissionScene room = MissionSceneCatalog.forMission(missionIndex);
        binding.briefScene.setScene(room);
        String[] labels = viewModel.mission().items.toArray(new String[0]);
        binding.briefScene.setSlotLabels(labels);

        viewModel.step().observe(this, this::showStep);
        viewModel.items().observe(this, this::renderMission);
        viewModel.progress().observe(this, percent -> {
            int value = percent == null ? 0 : percent;
            binding.processingBar.setProgress(value);
            binding.processingPercent.setText(FaNum.percent(value));
        });
        viewModel.extractionFailed().observe(this, failed ->
                binding.processingFailure.setVisibility(
                        Boolean.TRUE.equals(failed) ? View.VISIBLE : View.GONE));
    }

    @Override
    protected void onSpeakingChanged(boolean speaking) {
        if (binding != null) {
            binding.briefMascot.setSpeaking(speaking);
        }
    }

    // ------------------------------------------------------------------ brief

    private void wireBrief() {
        binding.briefHeader.headerBack.setOnClickListener(v -> {
            tap();
            finish();
        });
        bindStars(binding.briefHeader.headerStarsValue);
        binding.briefMascot.setState(MascotState.WAVE);
        // There is no "go to the camera" button any more: each item card opens the
        // camera for its own item. This one only appears once the room is full.
        binding.briefSave.setOnClickListener(v -> {
            tap();
            saveSceneToGallery();
        });
    }

    private void renderMission(@Nullable List<CapturedItem> items) {
        Mission mission = viewModel.mission();
        if (mission == null) {
            return;
        }
        int captured = viewModel.capturedCount(items);
        boolean complete = viewModel.isComplete(items);
        String name = getString(R.string.mascot_name);
        // Items can be shot in any order now, so "what's next" is the first empty
        // slot rather than the one after however many are done.
        String current = mission.itemAt(firstEmptySlot(mission, items));

        binding.briefHeader.headerTitle.setText(mission.title);
        binding.briefLine.setText(complete
                ? getString(R.string.draw_line_complete, name)
                : getString(R.string.draw_line_incomplete, name, mission.title, current));
        binding.briefNow.setText(complete
                ? getString(R.string.draw_tap_to_redo)
                : getString(R.string.draw_tap_to_shoot));
        binding.briefSave.setVisibility(complete ? View.VISIBLE : View.GONE);
        // Rebuilt after a rotation mid-shot, the camera still names the tapped item.
        String shooting = mission.itemAt(targetSlot());
        binding.cameraTitle.setText(getString(R.string.cam_title, shooting));
        binding.processingTitle.setText(getString(R.string.processing_title, shooting));

        renderSlots(mission, items);
        renderSceneItems(items);

        if (complete && viewModel.claimCelebration(captured)) {
            celebrate(mission);
        }
    }

    /**
     * One card per item in the mission. The container is a column down the side of
     * the screen in landscape and a row across the bottom in portrait, so rather
     * than keeping two lists this asks the container which way it runs and builds
     * the cards to suit: stacked and full width in a column, share-and-share-alike
     * in a row.
     */
    private void renderSlots(Mission mission, @Nullable List<CapturedItem> items) {
        LinearLayout container = binding.briefSlots;
        boolean row = container.getOrientation() == LinearLayout.HORIZONTAL;
        container.removeAllViews();
        for (int slot = 0; slot < mission.size(); slot++) {
            CapturedItem captured = itemForSlot(items, slot);
            View card = buildSlotCard(mission.itemAt(slot), captured, slot, row);
            LinearLayout.LayoutParams params = row
                    ? new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    : new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            if (row) {
                if (slot > 0) {
                    params.setMarginStart(dp(6));
                }
            } else {
                params.bottomMargin = dp(8);
            }
            container.addView(card, params);
        }
    }

    private View buildSlotCard(String label, @Nullable CapturedItem captured, int slot,
                               boolean row) {
        LinearLayout card = new LinearLayout(this);
        // A card in a row is a quarter of the screen wide, so the name goes under the
        // picture rather than beside it; there is no width to put them side by side.
        card.setOrientation(row ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.setBackgroundResource(captured != null
                ? R.drawable.bg_slot_done : R.drawable.bg_slot_empty);

        AppCompatImageView thumb = new AppCompatImageView(this);
        thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int thumbSize = getResources().getDimensionPixelSize(R.dimen.slot_thumb);
        LinearLayout.LayoutParams thumbParams =
                new LinearLayout.LayoutParams(thumbSize, thumbSize);
        if (captured != null) {
            Bitmap bitmap = BitmapIO.readSampled(captured.pngPath, 200);
            if (bitmap != null) {
                thumb.setImageBitmap(bitmap);
            }
        } else {
            // An empty card shows a camera, so it reads as "tap me to take its photo".
            thumb.setBackgroundColor(Palette.hueFill(slot));
            thumb.setImageResource(R.drawable.ic_camera);
            thumb.setColorFilter(Color.WHITE);
            thumb.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int inset = thumbSize / 4;
            thumb.setPadding(inset, inset, inset, inset);
        }
        card.addView(thumb, thumbParams);

        AppCompatTextView text = new AppCompatTextView(this);
        text.setText(label);
        text.setTextSize(row ? 12f : 13f);
        text.setMaxLines(1);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(ContextCompat.getColor(this,
                captured != null ? R.color.ink_slate : R.color.blue_slot_ink));
        LinearLayout.LayoutParams textParams = row
                ? new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (row) {
            textParams.topMargin = dp(4);
        } else {
            textParams.setMarginStart(dp(8));
        }
        card.addView(text, textParams);

        // Every card opens the camera for its own item. On a card that already has a
        // drawing, the new photo simply replaces the old one once it has been cut out
        // — the old one stays in the room until then, so backing out loses nothing.
        card.setOnClickListener(v -> {
            tap();
            openCamera(slot);
        });
        card.setContentDescription(getString(R.string.cam_title, label));
        return card;
    }

    @Nullable
    private CapturedItem itemForSlot(@Nullable List<CapturedItem> items, int slot) {
        if (items == null) {
            return null;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).slotIndex == slot) {
                return items.get(i);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- camera

    private void wireCamera() {
        binding.cameraBack.setOnClickListener(v -> {
            tap();
            viewModel.goTo(LiveDrawingViewModel.Step.BRIEF);
        });
        binding.cameraShutter.setOnClickListener(v -> capture());
        binding.cameraPermissionButton.setOnClickListener(v -> requestCameraPermission());
        // FIT_CENTER keeps the whole frame visible, which is what RoiMapper assumes.
        binding.cameraPreview.setScaleType(PreviewView.ScaleType.FIT_CENTER);
    }

    private void openCamera(int slot) {
        Mission mission = viewModel.mission();
        viewModel.setTargetSlot(slot);
        String label = mission == null ? "" : mission.itemAt(slot);
        binding.cameraTitle.setText(getString(R.string.cam_title, label));
        binding.processingTitle.setText(getString(R.string.processing_title, label));
        viewModel.goTo(LiveDrawingViewModel.Step.CAMERA);
        if (hasCameraPermission()) {
            binding.cameraPermissionPanel.setVisibility(View.GONE);
            bindCamera();
        } else {
            binding.cameraPermissionPanel.setVisibility(View.VISIBLE);
            requestCameraPermission();
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CAMERA) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            binding.cameraPermissionPanel.setVisibility(View.GONE);
            bindCamera();
        } else {
            binding.cameraPermissionText.setText(R.string.cam_permission_denied);
        }
    }

    private void bindCamera() {
        if (cameraBound) {
            return;
        }
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();
                preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageCapture);
                cameraBound = true;
            } catch (Exception e) {
                binding.cameraPermissionPanel.setVisibility(View.VISIBLE);
                binding.cameraPermissionText.setText(R.string.cam_unavailable);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capture() {
        if (capturing || imageCapture == null) {
            return;
        }
        capturing = true;
        sounds.play(AudioManifest.SFX_SHUTTER);
        binding.cameraOverlay.setPulsing(false);
        final RectF roiInView = binding.cameraOverlay.roiRect();
        final int viewW = binding.cameraOverlay.getWidth();
        final int viewH = binding.cameraOverlay.getHeight();

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Bitmap frame = FrameDecoder.toUprightBitmap(image);
                        image.close();
                        capturing = false;
                        binding.cameraOverlay.setPulsing(true);
                        if (frame == null) {
                            // Don't drop the child back on the camera with no word of
                            // why — send them to the same "let's try again" panel a
                            // failed extraction uses.
                            viewModel.reportCaptureFailed();
                            return;
                        }
                        // Ask the preview how it is fitting the image rather than
                        // assuming: getting this wrong crops the child's drawing out
                        // of its own photo and every capture fails.
                        RectF roi = RoiMapper.toNormalized(roiInView, viewW, viewH,
                                frame.getWidth(), frame.getHeight(), previewFit());
                        processingStartedAt = System.currentTimeMillis();
                        viewModel.processCapture(frame, roi, targetSlot(),
                                LiveDrawingActivity.this::onCaptured);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        capturing = false;
                        binding.cameraOverlay.setPulsing(true);
                    }
                });
    }

    /** Whether the preview letterboxes the image or crops it. */
    private RoiMapper.Fit previewFit() {
        PreviewView.ScaleType type = binding.cameraPreview.getScaleType();
        boolean crop = type == PreviewView.ScaleType.FILL_CENTER
                || type == PreviewView.ScaleType.FILL_START
                || type == PreviewView.ScaleType.FILL_END;
        return crop ? RoiMapper.Fit.CROP : RoiMapper.Fit.LETTERBOX;
    }

    /** The slot the tapped card stands for; the first empty one if none was tapped. */
    private int targetSlot() {
        int slot = viewModel.targetSlot();
        if (slot >= 0) {
            return slot;
        }
        Mission mission = viewModel.mission();
        return mission == null ? 0 : firstEmptySlot(mission, viewModel.items().getValue());
    }

    private int firstEmptySlot(Mission mission, @Nullable List<CapturedItem> items) {
        for (int i = 0; i < mission.size(); i++) {
            if (itemForSlot(items, i) == null) {
                return i;
            }
        }
        return 0;
    }

    private void onCaptured(String label, int slotIndex, int capturedCount) {
        // The room re-renders from the database a moment later; this tells it which
        // drawing is the new one, so that one flies in instead of just appearing.
        pendingArrivalSlot = slotIndex;
        viewModel.setTargetSlot(-1);
        long elapsed = System.currentTimeMillis() - processingStartedAt;
        long wait = Math.max(0L, MIN_PROCESSING_MS - elapsed);
        // Let the drawing land before Pashmak reacts to it by name.
        binding.getRoot().postDelayed(() -> {
            mascot.addStars(2);
            mascot.say(getString(R.string.alive_arrived, label), MascotState.CHEER,
                    MascotController.HOLD_CHEER_MS, AudioManifest.VOICE_ITEM_ARRIVED);
        }, wait + 700L);
    }

    // ------------------------------------------------------------- processing

    private void wireProcessing() {
        binding.processingBack.setOnClickListener(v -> {
            tap();
            viewModel.goTo(LiveDrawingViewModel.Step.CAMERA);
        });
        binding.processingRetry.setOnClickListener(v -> {
            tap();
            binding.processingFailure.setVisibility(View.GONE);
            viewModel.goTo(LiveDrawingViewModel.Step.CAMERA);
        });
    }

    // ------------------------------------------------------------------ scene

    /** Puts every captured drawing into the mission's world. */
    private void renderSceneItems(@Nullable List<CapturedItem> items) {
        List<Bitmap> bitmaps = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                CapturedItem item = items.get(i);
                Bitmap bitmap = BitmapIO.readSampled(item.pngPath, 600);
                if (bitmap != null) {
                    bitmaps.add(bitmap);
                    slots.add(item.slotIndex);
                }
            }
        }
        binding.briefScene.setItems(bitmaps, slots);
        if (pendingArrivalSlot >= 0) {
            binding.briefScene.playArrival(pendingArrivalSlot);
            celebrate();
            pendingArrivalSlot = -1;
        }
    }

    /** Pashmak jumps up and down while the new drawing settles into the room. */
    private void celebrate() {
        binding.briefMascot.setState(MascotState.CHEER);
        binding.getRoot().postDelayed(
                () -> binding.briefMascot.setState(MascotState.WAVE),
                MascotController.HOLD_CHEER_MS);
    }

    private void celebrate(Mission mission) {
        sounds.play(AudioManifest.SFX_FANFARE);
        binding.liveConfetti.burst();
        mascot.addStars(5);
        mascot.say(getString(R.string.ms_mission_done), MascotState.CHEER,
                MascotController.HOLD_CHEER_MS, AudioManifest.VOICE_MISSION_DONE);
    }

    /**
     * Saves the finished room, with every drawing in place, both to the app's own
     * gallery and to the device's photo gallery (Pictures/PashmakGame). Only the
     * child's button does this now, so a room they are still redoing is never saved.
     */
    private void saveSceneToGallery() {
        Mission mission = viewModel.mission();
        Bitmap scene = ViewCapture.of(binding.briefScene, Color.WHITE);
        if (mission == null || scene == null) {
            Toast.makeText(this, R.string.gallery_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        binding.briefSave.setEnabled(false);
        GalleryRepository.get(this).saveAndExport(GalleryEntry.KIND_MISSION_SCENE,
                mission.index, mission.title, scene, uri -> {
                    binding.briefSave.setEnabled(true);
                    if (uri != null) {
                        sounds.play(AudioManifest.SFX_STAR);
                    }
                    Toast.makeText(this, uri != null
                                    ? R.string.draw_scene_saved
                                    : R.string.gallery_export_failed,
                            Toast.LENGTH_LONG).show();
                });
    }

    // ------------------------------------------------------------------ steps

    private void showStep(LiveDrawingViewModel.Step step) {
        binding.stepBrief.setVisibility(step == LiveDrawingViewModel.Step.BRIEF
                ? View.VISIBLE : View.GONE);
        binding.stepCamera.setVisibility(step == LiveDrawingViewModel.Step.CAMERA
                ? View.VISIBLE : View.GONE);
        binding.stepProcessing.setVisibility(step == LiveDrawingViewModel.Step.PROCESSING
                ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
        LiveDrawingViewModel.Step step = viewModel.step().getValue();
        if (step != null && step != LiveDrawingViewModel.Step.BRIEF) {
            viewModel.goTo(LiveDrawingViewModel.Step.BRIEF);
            return;
        }
        super.onBackPressed();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
