package com.example.pathfinder.manager;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import com.example.pathfinder.R;
import com.example.pathfinder.detection.BoundingBox;
import com.example.pathfinder.detection.DetectorModel;
import com.example.pathfinder.ui.OverlayView;
import com.example.pathfinder.utils.ImageUtils;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Manager {
    private final DetectorModel detector;
    private final OverlayView overlayView;

    private final ArFragment arFragment;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean isProcessing = false;

    private long lastProcessedTime = 0;
    private static final long PROCESS_INTERVAL_MS = 300;

    public Manager(DetectorModel detector, OverlayView overlayView, ArFragment arFragment) {
        this.detector = detector;
        this.overlayView = overlayView;
        this.arFragment = arFragment;
    }

    public void startArCore() {
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::handleFrameUpdate);
    }

    private void handleFrameUpdate(FrameTime frameTime) {
        long now = System.currentTimeMillis();

        if (isProcessing || (now - lastProcessedTime < PROCESS_INTERVAL_MS)) return;

        lastProcessedTime = now;

        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame == null) return;

        try {
            Image image = frame.acquireCameraImage();
            isProcessing = true;

            long startTime = System.nanoTime();

            Bitmap bitmap = ImageUtils.convertImageToBitmap(image);
            long afterConversion = System.nanoTime();
            Log.d("Performance", "Tempo de conversão: " + (afterConversion - startTime) / 1_000_000.0 + " ms");
            startTime = System.nanoTime();
            process(bitmap);
            long endTime = System.nanoTime();
            Log.d("Performance", "Tempo de processamento YOLO: " + (endTime - startTime) / 1_000_000.0 + " ms");
            isProcessing = false;
            image.close();

            Pose pose = frame.getCamera().getPose();
            float[] t = pose.getTranslation();
            Log.d("ARPose", "Posição: x=" + t[0] + " y=" + t[1] + " z=" + t[2]);

        }catch (Exception e) {
            Log.e("ARCore", "Erro ao capturar frame: " + e.getMessage());
        }
    }

    public void process(Bitmap image) {
        Pair<Bitmap, List<BoundingBox>> results = detector.Detect(image);

        // Post the results to the UI thread to update the overlay
        mainHandler.post(() -> {
            overlayView.setResults(results.second);
        });
    }
}
