package com.example.pathfinder.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;


class BoundingBox {
    float x1;
    float y1;
    float x2;

    public BoundingBox(float x1, float y1, float x2, float y2, float cx, float cy, float h, float w, float cnf, float cls, String clsName) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.cx = cx;
        this.cy = cy;
        this.h = h;
        this.w = w;
        this.cnf = cnf;
        this.cls = cls;
        this.clsName = clsName;
    }

    float y2;
    float cx;
    float cy;
    float h;
    float w;
    float cnf;
    float cls;
    String clsName;
};


public class Detector {

    //
    float INPUT_MEAN = 0f;
    float INPUT_STANDARD_DEVIATION = 255f;
    DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;

    private ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(new CastOp(INPUT_IMAGE_TYPE))
            .build();


    //

    private static final String MODEL_PATH = "yolo11s_saved_model/yolo11s_float32.tflite";
    private static final String LABELS_PATH = "yolo11s_saved_model/labels.txt";
    private static final float CONFIDENCE_THRESHOLD = 0.3f;
    private static final float IOU_THRESHOLD = 0.4f;

    private final Interpreter interpreter;
    private final List<String> labels;
    private final int inputImageWidth;
    private final int inputImageHeight;
    private final int[] outputShape;
    private final int[] inputShape;
    private int numChannel = 0 ;
    private int numElements = 0;
//    public static class DetectionResult {
//        private final RectF boundingBox;
//        private final String label;
//        private final float score;
//        public final Bitmap mask;
//
//        public DetectionResult(RectF boundingBox, String label, float score, Bitmap mask) {
//            this.boundingBox = boundingBox;
//            this.label = label;
//            this.score = score;
//            this.mask = mask;
//        }
//
//        public RectF getBoundingBox() { return boundingBox; }
//        public String getLabel() { return label; }
//        public float getScore() { return score; }
//    }

    public Detector(Context context) throws IOException {
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, MODEL_PATH);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        this.interpreter = new Interpreter(modelFile, options);

        this.labels = loadLabels(context); // Gets path from constant LABELS_PATH

        this.inputImageWidth = interpreter.getInputTensor(0).shape()[2];
        this.inputImageHeight = interpreter.getInputTensor(0).shape()[1];
        this.inputShape = interpreter.getInputTensor(0).shape();
        this.outputShape = interpreter.getOutputTensor(0).shape();

        if (outputShape != null) {
            this.numChannel = outputShape[1];
            this.numElements = outputShape[2];
        }
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

    public List<Bitmap> detect(Bitmap bitmap) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, false);

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new NormalizeOp(0f, 255f))
//                .add(new ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(new CastOp(DataType.FLOAT32))
                .build();

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(resizedBitmap);
        var procImg = imageProcessor.process(tensorImage);

        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32);
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputBuffer.getBuffer().rewind());

        interpreter.runForMultipleInputsOutputs(new Object[]{procImg.getBuffer()}, outputs);

        float[] detections = outputBuffer.getFloatArray();

        ByteBuffer buf = outputBuffer.getBuffer();

        var res = bestBox(detections);

        int numDetections = outputShape[2];
        int detectionSize = outputShape[1];
        int numClasses = labels.size();


//        List<TempResult> nmsResults = nonMaxSuppression(preNMSResults);

//        List<DetectionResult> finalResults = new ArrayList<>();
        List<Bitmap> finalResults = new ArrayList<>();

//        Matrix downscaleMatrix = new Matrix();
//        float scaleX = (float) originalWidth / inputImageWidth;
//        float scaleY = (float) originalHeight / inputImageHeight;
//        downscaleMatrix.postScale(scaleX, scaleY);
//
        return finalResults;
    }

//    private List<TempResult> nonMaxSuppression(List<TempResult> detections) {
//        detections.sort(Comparator.comparingDouble(d -> -d.score));
//        List<TempResult> finalDetections = new ArrayList<>();
//        while (!detections.isEmpty()) {
//            TempResult first = detections.remove(0);
//            finalDetections.add(first);
//            detections.removeIf(d -> calculateIoU(first.boundingBox, d.boundingBox) > IOU_THRESHOLD);
//        }
//        return finalDetections;
//    }

    private List<BoundingBox> bestBox(float[] array) {

        int numChannels = 84;
        int numElements = 8400;
        float[][] output2D = new float[numChannels][numElements];

        for (int j = 0; j < numChannels; j++) {
            int base = j * numElements;
            System.arraycopy(array, base, output2D[j], 0, numElements);
        }

        List<BoundingBox> boundingBoxes = new ArrayList<BoundingBox>();

        for(int j = 0; j < 8400; j++) {
            var maxConf = CONFIDENCE_THRESHOLD; // Menor confiança admitida
            var maxIdx = -1; //Classe com maior confiança inicializada como 'nenhuma'
            int i = 4; //Inicializa i para percorrer as 80 classes

            var arrayIdx = numElements * i + j; // Indice do array. 8400 * j -> 'linha correta' + i -> 'coluna correta'

            while (i < numChannels) { //Numero de canais da saida [1, 84, 8400]
                if (array[arrayIdx] > maxConf){
                    maxConf = array[arrayIdx];
                    maxIdx = i - 4; // Corrige o offset das colunas das caixas para selecionar o rotulo correto
                }
                i++;
                arrayIdx += numElements; // Muda para proxima linha/proxima classe de probs
            }


            if (maxConf > CONFIDENCE_THRESHOLD) {
//                var clsName = this.labels.get(maxIdx);
//                var x1 = array[j]; // 0
//                var y1 = array[j + numElements]; // 1
//                var w = array[j + numElements * 2];
//                var h = array[j + numElements * 3];
//                var x2 = x1 + w;
//                var y2 = y1 + h;
//                var cx = x1 + w / 2;
//                var cy = y1 + h / 2;
                var clsName = this.labels.get(maxIdx);
                var cx = array[j]; // 0
                var cy = array[j + numElements]; // 1
                var w = array[j + numElements * 2];
                var h = array[j + numElements * 3];
                var x1 = cx - (w/2F);
                var y1 = cy - (h/2F);
                var x2 = cx + (w/2F);
                var y2 = cy + (h/2F);

                //Descarta boxes que vazam das dimensoes da imagem
                if (x1 < 0F || x1 > 1F) continue;
                if (y1 < 0F || y1 > 1F) continue;
                if (x2 < 0F || x2 > 1F) continue;
                if (y2 < 0F || y2 > 1F) continue;


                boundingBoxes.add( new BoundingBox(x1, y1, x2, y2, cx, cy, h, w, maxConf, maxIdx, clsName) );
            }
        }
        return applyNMS(boundingBoxes);
    }

    private List<BoundingBox> applyNMS(List<BoundingBox> boxes) {
        // Step 1: Sort boxes by confidence score in descending order.
        boxes.sort(new Comparator<BoundingBox>() {
            @Override
            public int compare(BoundingBox o1, BoundingBox o2) {
                return Float.compare(o2.cnf, o1.cnf);
            }
        });

        List<BoundingBox> selectedBoxes = new ArrayList<>();

        // Step 2: Loop while there are boxes to process.
        while (!boxes.isEmpty()) {
            // Step 3: Select the box with the highest confidence.
            BoundingBox first = boxes.get(0);
            selectedBoxes.add(first);
            boxes.remove(0);

            // Step 4: Iterate through the remaining boxes and remove ones that overlap significantly.
            Iterator<BoundingBox> iterator = boxes.iterator();
            while (iterator.hasNext()) {
                BoundingBox nextBox = iterator.next();
                // Calculate the Intersection over Union (IoU).
                float iou = calculateIoU(first, nextBox);
                // If IoU is greater than the threshold, remove the box.
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove();
                }
            }
        }

        return selectedBoxes;
    }

//    private List<BoundingBox> applyNMS(List<BoundingBox> boxes) : MutableList<BoundingBox> {
//        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
//        val selectedBoxes = mutableListOf<BoundingBox>()
//
//        while(sortedBoxes.isNotEmpty()) {
//            val first = sortedBoxes.first()
//            selectedBoxes.add(first)
//            sortedBoxes.remove(first)
//
//            val iterator = sortedBoxes.iterator()
//            while (iterator.hasNext()) {
//                val nextBox = iterator.next()
//                val iou = calculateIoU(first, nextBox)
//                if (iou >= IOU_THRESHOLD) {
//                    iterator.remove()
//                }
//            }
//        }
//
//        return selectedBoxes
//    }
    private float calculateIoU(BoundingBox box1, BoundingBox box2) {
        float xA = Math.max(box1.x1, box2.x1);
        float yA = Math.max(box1.y1, box2.y1);
        float xB = Math.min(box1.x2, box2.x2);
        float yB = Math.min(box1.y2, box2.y2);

        // Calculate the area of the intersection rectangle.
        float interArea = Math.max(0, xB - xA) * Math.max(0, yB - yA);

        // Calculate the area of both bounding boxes.
        float box1Area = (box1.x2 - box1.x1) * (box1.y2 - box1.y1);
        float box2Area = (box2.x2 - box2.x1) * (box2.y2 - box2.y1);

        // Calculate the area of the union.
        float unionArea = box1Area + box2Area - interArea;

        // Compute the IoU. Return 0 if unionArea is 0 to avoid division by zero.
        return unionArea > 0 ? interArea / unionArea : 0;
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
