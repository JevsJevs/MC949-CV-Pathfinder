package com.example.pathfinder.manager;

import android.util.Log;

import androidx.camera.core.ImageProxy;

public class Manager {

    // 1. Add a TAG for logging. It's convention to use the class name.
    private static final String TAG = "Manager";
    /**
     * This method will be called for each frame received from the camera.
     * The ImageProxy contains the image data and metadata.
     *
     * @param imageProxy The camera frame to be processed.
     */
    public void process(ImageProxy imageProxy) {
        // In a real application, you would perform image processing here.
        // For now, we can log the timestamp of the frame.
        long frameTimestamp = imageProxy.getImageInfo().getTimestamp();
        Log.d(TAG, "Processing frame with timestamp: " + frameTimestamp);

        // IMPORTANT: You must close the ImageProxy when you are done with it.
        // Failure to do so will stop the camera from producing new frames.
        imageProxy.close();
    }
}
