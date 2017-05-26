package com.zugaldia.robocar.software.camera.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import junit.framework.Assert;

import timber.log.Timber;

/**
 * Utility class for manipulating images.
 * Based on: https://github.com/androidthings/sample-tensorflow-imageclassifier/blob/master/app/src/main/java/com/example/androidthings/imageclassifier/env/ImageUtils.java
 **/
public class ImageUtils {
  // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
  // are normalized to eight bits.
  static final int kMaxChannelValue = 262143;

  /**
   * Utility method to compute the allocated size in bytes of a YUV420SP image
   * of the given dimensions.
   */
  public static int getYuvByteSize(final int width, final int height) {
    // The luminance plane requires 1 byte per pixel.
    final int ySize = width * height;

    // The UV plane works on 2x2 blocks, so dimensions with odd size must be rounded up.
    // Each 2x2 block takes 2 bytes to encode, one each for U and V.
    final int uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2;

    return ySize + uvSize;
  }

  /**
   * Saves a Bitmap object to disk for analysis.
   *
   * @param bitmap The bitmap to save.
   */
  public static void saveBitmap(final Bitmap bitmap, String root, String name) {
    Timber.i("Saving %dx%d bitmap to %s.", bitmap.getWidth(), bitmap.getHeight(), root);
    final File rootFile = new File(root);
    final File file = new File(rootFile, name);
    if (file.exists()) {
      file.delete();
    }

    try {
      final FileOutputStream out = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
      out.flush();
      out.close();
    } catch (final Exception e) {
      Timber.e(e, "Exception!");
    }
  }

  /**
   * Converts an image into a bitmap.
   */
  public static int[] convertImageToBitmap(
      Image image, int[] output, byte[][] cachedYuvBytes) {
    if (cachedYuvBytes == null || cachedYuvBytes.length != 3) {
      cachedYuvBytes = new byte[3][];
    }
    Image.Plane[] planes = image.getPlanes();
    fillBytes(planes, cachedYuvBytes);

    final int yRowStride = planes[0].getRowStride();
    final int uvRowStride = planes[1].getRowStride();
    final int uvPixelStride = planes[1].getPixelStride();

    convertYuv420ToArgb8888(cachedYuvBytes[0], cachedYuvBytes[1], cachedYuvBytes[2],
        image.getWidth(), image.getHeight(), yRowStride, uvRowStride, uvPixelStride,
        output);
    return output;
  }

  private static void convertYuv420ToArgb8888(byte[] ydata, byte[] udata, byte[] vdata,
                                              int width, int height, int yrowstride,
                                              int uvRowStride, int uvPixelStride, int[] out) {
    int i = 0;
    for (int y = 0; y < height; y++) {
      int py = yrowstride * y;
      int uvRowStart = uvRowStride * (y >> 1);
      int pu = uvRowStart;
      int pv = uvRowStart;

      for (int x = 0; x < width; x++) {
        int uvOffset = (x >> 1) * uvPixelStride;
        out[i++] = yuv2rgb(
            convertByteToInt(ydata, py + x),
            convertByteToInt(udata, pu + uvOffset),
            convertByteToInt(vdata, pv + uvOffset));
      }
    }
  }

  private static int convertByteToInt(byte[] arr, int pos) {
    return arr[pos] & 0xFF;
  }

  private static int yuv2rgb(int ny, int nu, int nv) {
    ny -= 16;
    nu -= 128;
    nv -= 128;
    if (ny < 0) {
      ny = 0;
    }

    // This is the floating point equivalent. We do the conversion in integer
    // because some Android devices do not have floating point in hardware.
    // nR = (int)(1.164 * nY + 2.018 * nU);
    // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
    // nB = (int)(1.164 * nY + 1.596 * nV);

    int nr = (int) (1192 * ny + 1634 * nv);
    int ng = (int) (1192 * ny - 833 * nv - 400 * nu);
    int nb = (int) (1192 * ny + 2066 * nu);

    nr = Math.min(kMaxChannelValue, Math.max(0, nr));
    ng = Math.min(kMaxChannelValue, Math.max(0, ng));
    nb = Math.min(kMaxChannelValue, Math.max(0, nb));

    nr = (nr >> 10) & 0xff;
    ng = (ng >> 10) & 0xff;
    nb = (nb >> 10) & 0xff;

    return 0xff000000 | (nr << 16) | (ng << 8) | nb;
  }

  private static void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null || yuvBytes[i].length != buffer.capacity()) {
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  /**
   * Crops and rescales and image for TensorFlow.
   */
  public static void cropAndRescaleBitmap(
      final Bitmap src, final Bitmap dst, int sensorOrientation) {
    Assert.assertEquals(dst.getWidth(), dst.getHeight());
    final float minDim = Math.min(src.getWidth(), src.getHeight());

    final Matrix matrix = new Matrix();

    // We only want the center square out of the original rectangle.
    final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
    final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
    matrix.preTranslate(translateX, translateY);

    final float scaleFactor = dst.getHeight() / minDim;
    matrix.postScale(scaleFactor, scaleFactor);

    // Rotate around the center if necessary.
    if (sensorOrientation != 0) {
      matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
      matrix.postRotate(sensorOrientation);
      matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
    }

    final Canvas canvas = new Canvas(dst);
    canvas.drawBitmap(src, matrix, null);
  }
}
