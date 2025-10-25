package com.example.pathfinder.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;


class TempResult {
    final RectF boundingBox;
    final String label;
    final float score;
    final float[] maskCoeffs;
    TempResult(RectF box, String lbl, float scr, float[] coeffs) {
        this.boundingBox = box; this.label = lbl; this.score = scr; this.maskCoeffs = coeffs;
    }
}
public class Detector {
    private static final String MODEL_PATH = "yolo11s-seg_saved_model/yolo11s-seg_float32.tflite";
    private static final String LABELS_PATH = "yolo11s-seg_saved_model/labels.txt";
    private static final float CONFIDENCE_THRESHOLD = 0.3f;
    private static final float IOU_THRESHOLD = 0.5f;

    private final Interpreter interpreter;
    private final List<String> labels;
    private final int inputImageWidth;
    private final int inputImageHeight;
    private final int[] outputShape;
    private final int[] outputMaskShape;

    public static class DetectionResult {
        private final RectF boundingBox;
        private final String label;
        private final float score;
        public final Bitmap mask;

        public DetectionResult(RectF boundingBox, String label, float score, Bitmap mask) {
            this.boundingBox = boundingBox;
            this.label = label;
            this.score = score;
            this.mask = mask;
        }

        public RectF getBoundingBox() { return boundingBox; }
        public String getLabel() { return label; }
        public float getScore() { return score; }
    }

    public Detector(Context context) throws IOException {
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, MODEL_PATH);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        this.interpreter = new Interpreter(modelFile, options);

        this.labels = loadLabels(context); // Gets path from constant LABELS_PATH

        this.inputImageWidth = interpreter.getInputTensor(0).shape()[2];
        this.inputImageHeight = interpreter.getInputTensor(0).shape()[1];
        this.outputShape = interpreter.getOutputTensor(0).shape();
        this.outputMaskShape = interpreter.getOutputTensor(1).shape();
    }

    private List<String> loadLabels(Context context) throws IOException {
        List<String> labelList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(Detector.LABELS_PATH)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labelList.add(line);
            }
        }
        return labelList;
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        TensorImage tensorImage = new TensorImage(interpreter.getInputTensor(0).dataType());
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32);
        TensorBuffer outputMaskBuffer = TensorBuffer.createFixedSize(outputMaskShape, DataType.FLOAT32);
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputBuffer.getBuffer().rewind());
        outputs.put(1, outputMaskBuffer.getBuffer().rewind());

        interpreter.runForMultipleInputsOutputs(new Object[]{tensorImage.getBuffer()}, outputs);

        float[] detections = outputBuffer.getFloatArray();
        float[] maskPrototypes = outputMaskBuffer.getFloatArray();

        int numDetections = outputShape[2];
        int detectionSize = outputShape[1];
        int numClasses = labels.size();
        int numMaskCoeffs = detectionSize - 4 - numClasses;

        float[][] transposedDetections = new float[numDetections][detectionSize];
        for (int i = 0; i < numDetections; i++) {
            for (int j = 0; j < detectionSize; j++) {
                transposedDetections[i][j] = detections[j * numDetections + i];
            }
        }

        List<TempResult> preNMSResults = new ArrayList<>();
        for (int i = 0; i < numDetections; i++) {
            float[] detection = transposedDetections[i];
            float maxScore = 0f;
            int classId = -1;
            for (int j = 0; j < numClasses; j++) {
                if (detection[4 + j] > maxScore) {
                    maxScore = detection[4 + j];
                    classId = j;
                }
            }
            if (maxScore > CONFIDENCE_THRESHOLD) {
                float x = detection[0]; float y = detection[1]; float w = detection[2]; float h = detection[3];
                RectF boundingBox = new RectF(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
                String label = labels.get(classId);
                float[] maskCoeffs = new float[numMaskCoeffs];
                System.arraycopy(detection, 4 + numClasses, maskCoeffs, 0, numMaskCoeffs);
                preNMSResults.add(new TempResult(boundingBox, label, maxScore, maskCoeffs));
            }
        }

        List<TempResult> nmsResults = nonMaxSuppression(preNMSResults);

        List<DetectionResult> finalResults = new ArrayList<>();
        int maskProtoHeight = outputMaskShape[2];
        int maskProtoWidth = outputMaskShape[3];

        Matrix downscaleMatrix = new Matrix();
        float scaleX = (float) originalWidth / inputImageWidth;
        float scaleY = (float) originalHeight / inputImageHeight;
        downscaleMatrix.postScale(scaleX, scaleY);

        for (TempResult result : nmsResults) {
            float[] finalMask = new float[maskProtoHeight * maskProtoWidth];
            for (int i = 0; i < numMaskCoeffs; i++) {
                for (int j = 0; j < finalMask.length; j++) {
                    finalMask[j] += result.maskCoeffs[i] * maskPrototypes[i * finalMask.length + j];
                }
            }

            int[] pixels = new int[maskProtoHeight * maskProtoWidth];
            for (int i = 0; i < finalMask.length; i++) {
                float value = (float) (1.0 / (1.0 + Math.exp(-finalMask[i])));
                if (value > 0.5f) {
                    pixels[i] = Color.RED;
                }
            }
            Bitmap maskBitmap = Bitmap.createBitmap(maskProtoWidth, maskProtoHeight, Bitmap.Config.ARGB_8888);
            maskBitmap.setPixels(pixels, 0, maskProtoWidth, 0, 0, maskProtoWidth, maskProtoHeight);

            Matrix upscaleMatrix = new Matrix();
            upscaleMatrix.postScale((float) inputImageWidth / maskProtoWidth, (float) inputImageHeight / maskProtoHeight);
            Bitmap scaledMask = Bitmap.createBitmap(maskBitmap, 0, 0, maskProtoWidth, maskProtoHeight, upscaleMatrix, true);

            RectF scaledBox = new RectF(
                    result.boundingBox.left * inputImageWidth, result.boundingBox.top * inputImageHeight,
                    result.boundingBox.right * inputImageWidth, result.boundingBox.bottom * inputImageHeight
            );
            
            RectF finalBox = new RectF();
            downscaleMatrix.mapRect(finalBox, scaledBox);

            if(finalBox.width() <= 0 || finalBox.height() <= 0) continue;

            Bitmap croppedMask = Bitmap.createBitmap(scaledMask,
                    (int) scaledBox.left, (int) scaledBox.top,
                    (int) scaledBox.width(), (int) scaledBox.height());

            finalResults.add(new DetectionResult(finalBox, result.label, result.score, croppedMask));
        }

        return finalResults;
    }

    private List<TempResult> nonMaxSuppression(List<TempResult> detections) {
        detections.sort(Comparator.comparingDouble(d -> -d.score));
        List<TempResult> finalDetections = new ArrayList<>();
        while (!detections.isEmpty()) {
            TempResult first = detections.remove(0);
            finalDetections.add(first);
            detections.removeIf(d -> calculateIoU(first.boundingBox, d.boundingBox) > IOU_THRESHOLD);
        }
        return finalDetections;
    }

    private float calculateIoU(RectF box1, RectF box2) {
        float xA = Math.max(box1.left, box2.left);
        float yA = Math.max(box1.top, box2.top);
        float xB = Math.min(box1.right, box2.right);
        float yB = Math.min(box1.bottom, box2.bottom);
        float interArea = Math.max(0, xB - xA) * Math.max(0, yB - yA);
        float box1Area = (box1.width()) * (box1.height());
        float box2Area = (box2.width()) * (box2.height());
        float unionArea = box1Area + box2Area - interArea;
        return unionArea > 0 ? interArea / unionArea : 0;
    }
}
