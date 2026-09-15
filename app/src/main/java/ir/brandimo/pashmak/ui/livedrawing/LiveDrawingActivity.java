package ir.brandimo.pashmak.ui.livedrawing;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

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

import ir.brandimo.pashmak.R;
import ir.brandimo.pashmak.audio.AudioManifest;
import ir.brandimo.pashmak.data.catalog.Mission;
import ir.brandimo.pashmak.data.catalog.MissionScene;
import ir.brandimo.pashmak.data.catalog.MissionSceneCatalog;
import ir.brandimo.pashmak.data.catalog.Palette;
import ir.brandimo.pashmak.data.db.CapturedItem;
import ir.brandimo.pashmak.data.db.GalleryEntry;
import ir.brandimo.pashmak.data.repo.GalleryRepository;
import ir.brandimo.pashmak.databinding.ActivityLiveDrawingBinding;
import ir.brandimo.pashmak.mascot.MascotController;
import ir.brandimo.pashmak.mascot.MascotState;
import ir.brandimo.pashmak.ui.base.BaseActivity;
import ir.brandimo.pashmak.ui.gallery.GalleryActivity;
import ir.brandimo.pashmak.util.BitmapIO;
import ir.brandimo.pashmak.util.FaNum;
import ir.brandimo.pashmak.util.ViewCapture;
import ir.brandimo.pashmak.vision.FrameDecoder;
import ir.brandimo.pashmak.vision.RoiMapper;

/**
 * The Osmo-style loop: read the brief, draw on real paper, scan it, watch it come
 * to life beside the mascot, repeat until the collection is finished.
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
    private int sceneSavedForCount = -1;
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
        wireAlive();
        MissionScene room = MissionSceneCatalog.forMission(missionIndex);
        binding.aliveSceneView.setScene(room);
        binding.briefScene.setScene(room);
        String[] labels = viewModel.mission().items.toArray(new String[0]);
        binding.aliveSceneView.setSlotLabels(labels);
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
            binding.aliveMascot.setSpeaking(speaking);
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
        binding.briefGoCamera.setOnClickListener(v -> {
            tap();
            openCamera();
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
        String current = viewModel.currentItem(items);

        binding.briefHeader.headerTitle.setText(mission.title);
        binding.briefLine.setText(complete
                ? getString(R.string.draw_line_complete, name)
                : getString(R.string.draw_line_incomplete, name, mission.title, current));
        binding.briefNow.setText(getString(R.string.draw_now, current));
        binding.briefNow.setVisibility(complete ? View.GONE : View.VISIBLE);
        binding.briefGoCamera.setVisibility(complete ? View.GONE : View.VISIBLE);
        binding.cameraTitle.setText(getString(R.string.cam_title, current));
        binding.processingTitle.setText(getString(R.string.processing_title, current));

        renderSlots(mission, items);
        renderAlive(mission, items, captured, complete);
    }

    private void renderSlots(Mission mission, @Nullable List<CapturedItem> items) {
        LinearLayout container = binding.briefSlots;
        container.removeAllViews();
        for (int slot = 0; slot < mission.size(); slot++) {
            CapturedItem captured = itemForSlot(items, slot);
            View card = buildSlotCard(mission.itemAt(slot), captured, slot);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(8);
            container.addView(card, params);
        }
    }

    private View buildSlotCard(String label, @Nullable CapturedItem captured, int slot) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
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
            thumb.setBackgroundColor(Palette.hueFill(slot));
        }
        card.addView(thumb, thumbParams);

        AppCompatTextView text = new AppCompatTextView(this);
        text.setText(label);
        text.setTextSize(13f);
        text.setTextColor(ContextCompat.getColor(this,
                captured != null ? R.color.ink_slate : R.color.blue_slot_ink));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart(dp(8));
        card.addView(text, textParams);

        if (captured != null) {
            card.setOnClickListener(v -> {
                tap();
                viewModel.retakeSlot(slot);
            });
        }
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

    private void openCamera() {
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
                        viewModel.processCapture(frame, roi, currentSlot(),
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

    private int currentSlot() {
        List<CapturedItem> items = viewModel.items().getValue();
        return viewModel.capturedCount(items);
    }

    private void onCaptured(String label, int capturedCount) {
        pendingArrivalSlot = Math.max(0, capturedCount - 1);
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

    // ------------------------------------------------------------------ alive

    private void wireAlive() {
        binding.aliveBack.setOnClickListener(v -> {
            tap();
            viewModel.goTo(LiveDrawingViewModel.Step.BRIEF);
        });
        binding.aliveMascot.setState(MascotState.CHEER);
        binding.aliveGallery.setOnClickListener(v -> open(GalleryActivity.class));
        binding.aliveRetake.setOnClickListener(v -> {
            tap();
            int last = currentSlot() - 1;
            if (last >= 0) {
                viewModel.retakeSlot(last);
            }
            viewModel.goTo(LiveDrawingViewModel.Step.CAMERA);
        });
    }

    private void renderAlive(Mission mission, @Nullable List<CapturedItem> items,
                             int captured, boolean complete) {
        String name = getString(R.string.mascot_name);
        binding.aliveHeadline.setText(complete
                ? getString(R.string.alive_headline_complete)
                : getString(R.string.alive_headline_progress,
                FaNum.of(captured), FaNum.of(mission.size())));

        String lastLabel = captured > 0 ? mission.itemAt(captured - 1) : "";
        binding.aliveLine.setText(complete
                ? getString(R.string.alive_line_complete, mission.title, name)
                : getString(R.string.alive_line_progress, lastLabel, name,
                FaNum.of(mission.size() - captured)));

        binding.aliveNext.setText(complete
                ? getString(R.string.alive_next_mission)
                : getString(R.string.alive_next_item, mission.currentItem(captured)));
        binding.aliveNext.setBackgroundResource(
                complete ? R.drawable.btn_green : R.drawable.btn_orange);
        binding.aliveNext.setOnClickListener(v -> {
            tap();
            if (complete) {
                finish();
            } else {
                openCamera();
            }
        });
        binding.aliveRetake.setVisibility(captured > 0 ? View.VISIBLE : View.GONE);

        renderAliveItems(mission, items);

        if (complete && sceneSavedForCount != captured) {
            sceneSavedForCount = captured;
            celebrate(mission);
        }
    }

    /** Puts every captured drawing into the mission's world. */
    private void renderAliveItems(Mission mission, @Nullable List<CapturedItem> items) {
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
        binding.aliveSceneView.setItems(bitmaps, slots);
        binding.briefScene.setItems(bitmaps, slots);
        if (pendingArrivalSlot >= 0) {
            binding.aliveSceneView.playArrival(pendingArrivalSlot);
            celebrate();
            pendingArrivalSlot = -1;
        }
    }

    /** Pashmak jumps up and down while the new drawing settles into the room. */
    private void celebrate() {
        binding.aliveMascot.setState(MascotState.CHEER);
        binding.briefMascot.setState(MascotState.CHEER);
        binding.getRoot().postDelayed(() -> {
            binding.aliveMascot.setState(MascotState.CHEER);
            binding.briefMascot.setState(MascotState.WAVE);
        }, MascotController.HOLD_CHEER_MS);
    }

    private void celebrate(Mission mission) {
        sounds.play(AudioManifest.SFX_FANFARE);
        binding.liveConfetti.burst();
        mascot.addStars(5);
        mascot.say(getString(R.string.ms_mission_done), MascotState.CHEER,
                MascotController.HOLD_CHEER_MS, AudioManifest.VOICE_MISSION_DONE);
        // Give the items time to land before snapshotting the finished scene.
        binding.getRoot().postDelayed(() -> {
            Bitmap scene = ViewCapture.of(binding.aliveScene, Color.WHITE);
            if (scene != null) {
                GalleryRepository.get(this).save(GalleryEntry.KIND_MISSION_SCENE,
                        mission.index, mission.title, scene, null);
            }
        }, 900L);
    }

    // ------------------------------------------------------------------ steps

    private void showStep(LiveDrawingViewModel.Step step) {
        binding.stepBrief.setVisibility(step == LiveDrawingViewModel.Step.BRIEF
                ? View.VISIBLE : View.GONE);
        binding.stepCamera.setVisibility(step == LiveDrawingViewModel.Step.CAMERA
                ? View.VISIBLE : View.GONE);
        binding.stepProcessing.setVisibility(step == LiveDrawingViewModel.Step.PROCESSING
                ? View.VISIBLE : View.GONE);
        binding.stepAlive.setVisibility(step == LiveDrawingViewModel.Step.ALIVE
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
