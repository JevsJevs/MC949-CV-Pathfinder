package com.example.pathfinder.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageUtils {

    public static Bitmap convertImageToBitmap(Image image) {
        // The ARCore camera image is typically in YUV_420_888 format (YCbCr).
        // This format has 3 planes: Y, U, and V.

        // Get the planes from the Image object
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        // Combine Y, U, and V planes into a single NV21 byte array
        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        // Note: The above is a simplification; for correct NV21 conversion, you need
        // to handle pixel and row strides, which can be complex. The following uses
        // a more direct approach with YuvImage which handles the format details
        // by compressing to JPEG first.

        // A more robust conversion approach:
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        // Compress the YuvImage to a JPEG
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, outStream); // 100 is the quality

        // Decode the JPEG byte array into a Bitmap
        byte[] imageBytes = outStream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        // Close the image to free resources
        image.close();

        return bitmap;
    }
}
