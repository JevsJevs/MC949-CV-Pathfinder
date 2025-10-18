package com.example.pathfinder.detection;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.IOException;
import java.util.List;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

public class Detector {
    private ObjectDetector detector;

    public Detector(Context context) {
        try {
            ObjectDetector.ObjectDetectorOptions.Builder optionsBuilder =
                    ObjectDetector.ObjectDetectorOptions.builder()
                            .setScoreThreshold(0.4f)
                            .setMaxResults(5);

            BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(4);

            optionsBuilder.setBaseOptions(baseOptionsBuilder.build());

            detector = ObjectDetector.createFromFileAndOptions(
                    context,
                    "yolov8m-seg-fp32.tflite",
                    optionsBuilder.build()
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Detection> detect(Bitmap bitmap) {
        TensorImage image = TensorImage.fromBitmap(bitmap);
        return detector.detect(image);
    }
}
