package com.example.pathfinder.manager;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import com.example.pathfinder.detection.BoundingBox;
import com.example.pathfinder.detection.DetectorModel;
import com.example.pathfinder.ui.OverlayView;
import com.example.pathfinder.utils.ImageUtils;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
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
            Bitmap bitmap = ImageUtils.convertImageToBitmap(image); //convert frame to bitmap
            image.close();

            long afterConversion = System.nanoTime();
            Log.d("Performance", "Tempo de conversão: " + (afterConversion - startTime) / 1_000_000.0 + " ms");

            startTime = System.nanoTime();
            Pair<Bitmap, List<BoundingBox>> detectionResult = process(bitmap); //call model
            long endTime = System.nanoTime();
            Log.d("Performance", "Tempo de processamento YOLO: " + (endTime - startTime) / 1_000_000.0 + " ms");

            List<Pair<BoundingBox, Float>> nearObjects = getObjectsWithLessThanDistance(detectionResult, 1f, frame); //near objects, less than threshold

            for (Pair<BoundingBox, Float> obj : nearObjects) {
                Log.d("ARCoreDistance", "Objeto: " + obj.first.clsName + ", Distância: " + String.format("%.2f", obj.second) + " metros");
            }

            float distanceToNearestWall = distanceToNearestWall(frame);
            if (distanceToNearestWall < 1f) {
                Log.d("ARCoreDistance", "Parede, Distancia: " + String.format("%.2f", distanceToNearestWall) + " metros");
            }

            isProcessing = false;
        } catch (Exception e) {
            Log.e("ARCore", "Erro ao capturar frame: " + e.getMessage());
        }
    }

    public Pair<Bitmap, List<BoundingBox>> process(Bitmap image) {
        Pair<Bitmap, List<BoundingBox>> results = detector.Detect(image);

        mainHandler.post(() -> {
            overlayView.setResults(results.second);
        });

        return results;
    }

    private List<Pair<BoundingBox, Float>> getObjectsWithLessThanDistance(Pair<Bitmap, List<BoundingBox>> detectionResult, float distace, Frame frame) {
        List<BoundingBox> boundingBoxes = detectionResult.second;
        List<Pair<BoundingBox, Float>> nearObjects = new ArrayList<>();

        for (BoundingBox box : boundingBoxes) {
            int centerX = (int) box.cx;
            int centerY = (int) box.cy;

            float calculatedDistance = calculateDistanceWithHitTest(frame, centerX, centerY);
            if (calculatedDistance <= distace && calculatedDistance != Float.MIN_VALUE) {
                nearObjects.add(new Pair<>(box, calculatedDistance));
            }
        }

        return nearObjects;
    }

    private float calculateDistanceWithHitTest(Frame frame, int screenX, int screenY) {
        List<com.google.ar.core.HitResult> hitResults = frame.hitTest(screenX, screenY);

        if (!hitResults.isEmpty()) {
            com.google.ar.core.HitResult hit = hitResults.get(0);

            Pose hitPose = hit.getHitPose();

            Pose cameraPose = frame.getCamera().getPose();

            float dx = hitPose.tx() - cameraPose.tx();
            float dy = hitPose.ty() - cameraPose.ty();
            float dz = hitPose.tz() - cameraPose.tz();

            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        } else {
            //Distancia grande, sem risco de colisao
            return Float.MIN_VALUE;
        }
    }

    private float distanceToNearestWall(Frame frame) {
        float minDistance = Float.MAX_VALUE;

        for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
            if (plane.getTrackingState() != TrackingState.TRACKING) continue;
            if (plane.getType() != Plane.Type.VERTICAL) continue;

            Pose planePose = plane.getCenterPose();
            Pose cameraPose = frame.getCamera().getPose();

            float dx = planePose.tx() - cameraPose.tx();
            float dy = planePose.ty() - cameraPose.ty();
            float dz = planePose.tz() - cameraPose.tz();

            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < minDistance) minDistance = distance;
        }

        return minDistance < Float.MAX_VALUE ? minDistance : Float.MIN_VALUE;
    }
}
