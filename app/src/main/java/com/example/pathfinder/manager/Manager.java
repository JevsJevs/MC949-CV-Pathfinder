package com.example.pathfinder.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pathfinder.detection.BoundingBox;
import com.example.pathfinder.detection.DetectorModel;
import com.example.pathfinder.risk.RiskAnalyzer;
import com.example.pathfinder.risk.RiskAssessment;
import com.example.pathfinder.slam.ARCoreDistanceCalculation;
import com.example.pathfinder.tts.TTS;
import com.example.pathfinder.ui.OverlayView;
import com.example.pathfinder.utils.ImageUtils;
import com.google.ar.core.Frame;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.List;

public class Manager {
    private final DetectorModel detector;
    private final OverlayView overlayView;
    private final RiskAnalyzer riskAnalyzer;
    private final TTS tts;

    private final ArFragment arFragment;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isProcessing = false;

    private boolean shouldProcess = false;
    private final MutableLiveData<Boolean> shouldAlert = new MutableLiveData<>(true);

    float EPSILON = 0.05f; // distância mínima para considerar válida

    public Manager(Context context, DetectorModel detector, OverlayView overlayView, ArFragment arFragment, int screenWidth, int screenHeight) {
        this.detector = detector;
        this.overlayView = overlayView;
        this.arFragment = arFragment;
        this.riskAnalyzer = new RiskAnalyzer(screenWidth, screenHeight);
        this.tts = new TTS(context);
    }

    public void startArCore() {
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::handleFrameUpdate);
    }

    private void handleFrameUpdate(FrameTime frameTime) {
        if (!shouldProcess || isProcessing) return;

        isProcessing = true;

        Frame frame = arFragment.getArSceneView().getArFrame();
        if (frame == null) return;

        try {
            Image image = frame.acquireCameraImage();

            long startTime = System.nanoTime();
            Bitmap bitmap = ImageUtils.convertImageToBitmap(image); //convert frame to bitmap
            image.close();

            long afterConversion = System.nanoTime();
            Log.d("Performance", "Tempo de conversão: " + (afterConversion - startTime) / 1_000_000.0 + " ms");

            startTime = System.nanoTime();
            Pair<Bitmap, List<BoundingBox>> detectionResult = process(bitmap); //call model
            long endTime = System.nanoTime();
            Log.d("Performance", "Tempo de processamento YOLO: " + (endTime - startTime) / 1_000_000.0 + " ms");

            // Draw bounding boxes on the bitmap
            List<Pair<BoundingBox, Float>> objects = ARCoreDistanceCalculation.getObjectDistances(detectionResult, frame);
            mainHandler.post(() -> {
                overlayView.setResults(objects);
            });

            List<Pair<BoundingBox, Float>> nearObjects = ARCoreDistanceCalculation.getObjectsWithLessThanDistance(objects, 3f); //near objects, less than threshold

            for (Pair<BoundingBox, Float> obj : nearObjects) {
                Log.d("ARCoreDistance", "Objeto: " + obj.first.clsName + ", Distância: " + String.format("%.2f", obj.second) + " metros");
            }

            float distanceToNearestWall = ARCoreDistanceCalculation.distanceToNearestWall(frame);
            if (distanceToNearestWall > EPSILON && distanceToNearestWall < 3f) {
                Log.d("ARCoreDistance", "Parede, Distancia: " + distanceToNearestWall + " metros");
            }

            RiskAssessment riskAssessment = riskAnalyzer.analyzeRisk(nearObjects, distanceToNearestWall);
            Log.d("RiskAnalysis", riskAssessment.toString());

            if (riskAssessment.shouldAlert()) {
                Log.i("RiskAnalysis", "ALERTA: " + riskAssessment.getFullMessage());
                if (Boolean.TRUE.equals(shouldAlert.getValue())) {
                    tts.speak(riskAssessment.getMessage());
                }
            }

            isProcessing = false;
        } catch (Exception e) {
            Log.e("ARCore", "Erro ao capturar frame: " + e.getMessage());
            isProcessing = false;
        }
    }

    public Pair<Bitmap, List<BoundingBox>> process(Bitmap image) {
        return detector.Detect(image);
    }

    public LiveData<Boolean> getTtsInitialized() {
        return tts.isInitialized();
    }

    public LiveData<Boolean> getShouldAlert() {
        return shouldAlert;
    }

    public void toggleProcessing() {
        shouldProcess = !shouldProcess;
        // Clear overlay when not processing frames
        if (!shouldProcess) {
            mainHandler.post(() -> overlayView.setResults(null));
        }
    }

    public void toggleTTS() {
        shouldAlert.setValue(!Boolean.TRUE.equals(shouldAlert.getValue()));
        // Clear TTS queue
        if (!Boolean.TRUE.equals(shouldAlert.getValue())) {
            tts.stop();
        }
    }

    public void repeatLastAlert() {
        if (Boolean.TRUE.equals(shouldAlert.getValue())) {
            tts.repeatLastAlert();
        }
    }

    public void shutdown() {
        tts.shutdown();
    }
}
