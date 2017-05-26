package com.zugaldia.robocar.software.camera.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.media.Image;

import junit.framework.Assert;

/**
 * Class that process an Image and extracts a Bitmap in a format appropriate for
 * TensorFlowImageClassifier. Based on:
 * https://github.com/androidthings/sample-tensorflow-imageclassifier/blob/master/app/src/main/java/com/example/androidthings/imageclassifier/ImagePreprocessor.java
 */
public class ImagePreprocessor {
  private Bitmap rgbFrameBitmap;
  private Bitmap croppedBitmap;

  private byte[][] cachedYuvBytes;
  private int[] cachedRgbBytes;

  public ImagePreprocessor(int inputImageWidth, int inputImageHeight, int outputSize) {
    this.cachedRgbBytes = new int[inputImageWidth * inputImageHeight];
    this.cachedYuvBytes = new byte[3][];
    this.croppedBitmap = Bitmap.createBitmap(outputSize, outputSize, Config.ARGB_8888);
    this.rgbFrameBitmap = Bitmap.createBitmap(inputImageWidth, inputImageHeight, Config.ARGB_8888);
  }

  // No cropping or rescaling
  public Bitmap convertImage(final Image image) {
    if (image == null) {
      return null;
    }

    Assert.assertEquals("Invalid size width", rgbFrameBitmap.getWidth(), image.getWidth());
    Assert.assertEquals("Invalid size height", rgbFrameBitmap.getHeight(), image.getHeight());

    cachedRgbBytes = ImageUtils.convertImageToBitmap(image, cachedRgbBytes, cachedYuvBytes);

    if (rgbFrameBitmap != null) {
      rgbFrameBitmap.setPixels(cachedRgbBytes, 0, image.getWidth(), 0, 0,
          image.getWidth(), image.getHeight());
    }

    image.close();
    return rgbFrameBitmap;
  }

  public Bitmap preprocessImage(final Image image) {
    if (image == null) {
      return null;
    }

    Assert.assertEquals("Invalid size width", rgbFrameBitmap.getWidth(), image.getWidth());
    Assert.assertEquals("Invalid size height", rgbFrameBitmap.getHeight(), image.getHeight());

    cachedRgbBytes = ImageUtils.convertImageToBitmap(image, cachedRgbBytes, cachedYuvBytes);

    if (croppedBitmap != null && rgbFrameBitmap != null) {
      rgbFrameBitmap.setPixels(cachedRgbBytes, 0, image.getWidth(), 0, 0,
          image.getWidth(), image.getHeight());
      ImageUtils.cropAndRescaleBitmap(rgbFrameBitmap, croppedBitmap, 0);
    }

    image.close();
    return croppedBitmap;
  }
}
