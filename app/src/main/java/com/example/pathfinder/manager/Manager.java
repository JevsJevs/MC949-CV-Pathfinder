package com.example.pathfinder.manager;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import androidx.camera.core.ImageProxy;
import com.example.pathfinder.detection.*;
import com.example.pathfinder.ui.OverlayView;
import java.util.List;
import java.util.Map;

public class Manager {
    private final DetectorModel detector;
    private final OverlayView overlayView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public Manager(DetectorModel detector, OverlayView overlayView) {
        this.detector = detector;
        this.overlayView = overlayView;
    }

    public void process(ImageProxy imageProxy) {
        if (detector == null) {
            imageProxy.close();
            return;
        }

        Bitmap bitmap = imageProxy.toBitmap();
        if (bitmap == null) {
            imageProxy.close();
            return;
        }

        Pair<Bitmap, List<BoundingBox>> results = detector.Detect(bitmap);

        // Post the results to the UI thread to update the overlay
        mainHandler.post(() -> {
            overlayView.setResults(results.second);
        });

        imageProxy.close();
    }
}
