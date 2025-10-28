package com.example.pathfinder.detection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Pair;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.util.List;
import java.util.Map;

public interface DetectorModel {

    TensorImage PreProcess(Bitmap ogImg);
    List<BoundingBox> PostProcess(TensorBuffer output);

    Pair<Bitmap, List<BoundingBox>> Detect(Bitmap img);

}
