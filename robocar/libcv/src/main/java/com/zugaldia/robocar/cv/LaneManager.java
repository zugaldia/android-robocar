package com.zugaldia.robocar.cv;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;

/**
 * This is a port of the utility methods in Udacity's Lane Finding Project
 * for Self-Driving Car ND to Java/Android.
 * <p>
 * <p>See: https://github.com/udacity/CarND-LaneLines-P1
 */
public class LaneManager {

  private static final opencv_core.Scalar WHITE = new opencv_core.Scalar(255, 255, 255, 0);

  public static final int HISTOGRAM_BINS = 32;

  /**
   * Read image path into object.
   */
  public static opencv_core.Mat readImage(String filename) {
    opencv_core.Mat image = opencv_imgcodecs.imread(filename);
    if (image.data() == null) {
      throw new RuntimeException(
          "The image could not be read (because of missing file, improper permissions, "
              + "unsupported or invalid format).");
    }

    return image;
  }

  /**
   * Write image object into specific path.
   */
  public static boolean writeImage(String filename, opencv_core.Mat img) {
    return opencv_imgcodecs.imwrite(filename, img);
  }

  public static boolean writeImage(String filename, opencv_core.UMat img) {
    return opencv_imgcodecs.imwrite(filename, img);
  }

  /**
   * Convert image to grayscale.
   */
  public static opencv_core.Mat doGrayscale(opencv_core.Mat src) {
    opencv_core.Mat dst = new opencv_core.Mat();
    opencv_imgproc.cvtColor(src, dst, opencv_imgproc.COLOR_BGR2GRAY);
    return dst;
  }

  /**
   * Apply Canny filter to image.
   */
  public static opencv_core.Mat doCanny(
      opencv_core.Mat image, double lowThreshold, double highThreshold) {
    opencv_core.Mat edges = new opencv_core.Mat();
    opencv_imgproc.Canny(image, edges, lowThreshold, highThreshold);
    return edges;
  }

  /**
   * Apply Gaussian Blur filter to image.
   */
  public static opencv_core.Mat doGaussianBlur(opencv_core.Mat src, int kernelSize) {
    opencv_core.Mat dst = new opencv_core.Mat();
    opencv_core.Size ksize = new opencv_core.Size(kernelSize, kernelSize);
    opencv_imgproc.GaussianBlur(src, dst, ksize, 0);
    return dst;
  }

  /**
   * Apply Hough Lines filter to image.
   */
  public static opencv_core.Mat doHoughLines(
      opencv_core.Mat image, double rho, double theta, int threshold,
      double minLineLength, double maxLineGap) {
    opencv_core.Mat lines = new opencv_core.Mat();
    opencv_imgproc.HoughLinesP(image, lines, rho, theta, threshold, minLineLength, maxLineGap);
    return lines;
  }

  /**
   * Applies an image mask. Only keeps the region of the image defined by the
   * polygon formed from `vertices`. The rest of the image is set to black.
   */
  public static opencv_core.Mat applyMask(opencv_core.Mat image, int[] points) {
    opencv_core.Mat mask = new opencv_core.Mat(image.size(), image.type());

    // Array of polygons where each polygon is represented as an array of points
    opencv_core.Point polygon = new opencv_core.Point();
    polygon.put(points, 0, points.length);
    opencv_imgproc.fillPoly(mask, polygon, new int[] {points.length / 2}, 1, WHITE);

    opencv_core.Mat masked = new opencv_core.Mat(image.size(), image.type());
    opencv_core.bitwise_and(image, mask, masked);
    return masked;
  }

  /**
   * Given source and destination points, calculate the perspective transform matrix and warp
   * the image.
   */
  public static opencv_core.Mat perspectiveTransform(
      opencv_core.Mat image, float[] fromPoints, float[] toPoints) {
    // Convert float[] into Point2f
    opencv_core.Point2f fromPointsWrapped = new opencv_core.Point2f();
    fromPointsWrapped.put(fromPoints, 0, fromPoints.length);
    opencv_core.Point2f toPointsWrapped = new opencv_core.Point2f();
    toPointsWrapped.put(toPoints, 0, toPoints.length);

    // Get perspective transformation for the corresponding 4 point pairs
    opencv_core.Mat matrix = opencv_imgproc.getPerspectiveTransform(
        fromPointsWrapped, toPointsWrapped);

    // Transform the source image
    opencv_core.Mat warped = new opencv_core.Mat();
    opencv_imgproc.warpPerspective(image, warped, matrix, image.size());
    return warped;
  }

  public static opencv_core.Mat getSaturationChannel(opencv_core.Mat image) {
    opencv_core.Mat hsv = new opencv_core.Mat();
    opencv_imgproc.cvtColor(image, hsv, opencv_imgproc.COLOR_BGR2HSV);
    opencv_core.MatVector channels = new opencv_core.MatVector();
    opencv_core.split(hsv, channels);
    return channels.get(1);
  }

  public static opencv_core.Mat threshold(opencv_core.Mat image, double minValue) {
    opencv_core.Mat thresholdBinary = new opencv_core.Mat();
    opencv_imgproc.threshold(image, thresholdBinary, minValue, 255, opencv_imgproc.CV_THRESH_BINARY);
    return thresholdBinary;
  }

  /**
   * Compute the histogram manually.
   */
  public static int[] histogramArray(opencv_core.Mat image) {
    final int[] result = new int[HISTOGRAM_BINS];
    final double binSize = image.size().width() / HISTOGRAM_BINS;

    UByteRawIndexer binaryIndex = image.createIndexer();
    for (int col = 0; col < image.cols(); col++) {
      int bin = (int) (col / binSize);
      for (int row = 0; row < image.rows(); row++) {
        int value = binaryIndex.get(row, col);
        if (value > 0) {
          result[bin]++;
        }
      }
    }

    return result;
  }

  /**
   * Calculates a histogram of an image. Unfortunately, I can't get this to work,
   * the returned values don't make sense.
   */
  public static opencv_core.UMat histogram(opencv_core.Mat image) {
    if (image.channels() != 1) {
      throw new LaneManagerException("Histogram expects images with one channel only.");
    }

    int[] channels = new int[] {0}; // Assume 1 channel
    int[] histSize = new int[] {HISTOGRAM_BINS}; // 32 bins

    // Saturation varies from 0 (black-gray-white) to 255 (pure spectrum color)
    float[] ranges = new float[] {0, 256}; // upper limit is exclusive

    opencv_core.UMat mask = new opencv_core.UMat();
    opencv_core.UMat histogram = new opencv_core.UMat();
    opencv_imgproc.calcHist(image, 1 /* nimages */, channels, mask, histogram, 1 /* dims */,
        histSize, ranges, true /* uniform */, false /* accumulate */);

    return histogram;
  }

  /**
   * See: https://github.com/bytedeco/javacpp-presets/issues/432
   */
  public static HistogramPosition getMaxPosition(opencv_core.UMat histogram) {
    DoublePointer minVal = new DoublePointer(1);
    DoublePointer maxVal = new DoublePointer(1);
    opencv_core.Point minLoc = new opencv_core.Point(1);
    opencv_core.Point maxLoc = new opencv_core.Point(1);
    opencv_core.minMaxLoc(histogram, minVal, maxVal, minLoc, maxLoc, null);
    return new HistogramPosition(maxLoc.y(), maxVal.get(0));
  }

  /**
   * Workaround while ^^^ gets fixed.
   */
  public static HistogramPosition getMaxPosition(int[] histogram) {
    int binIndex = -1;
    double binValue = -1;
    for (int bin = 0; bin < histogram.length; bin++) {
      if (binIndex == -1 || histogram[bin] > binValue) {
        binIndex = bin;
        binValue = histogram[bin];
      }
    }

    return new HistogramPosition(binIndex, binValue);
  }

  /**
   * Draw line into image.
   */
  public static void drawLine(
      opencv_core.Mat image, opencv_core.Point from, opencv_core.Point to,
      opencv_core.Scalar color, int thickness) {
    int lineType = opencv_core.LINE_8;
    int shift = 0;
    opencv_imgproc.line(image, from, to, color, thickness, lineType, shift);
  }

  /**
   * Draw filled rectangle into image.
   */
  public static void drawFilledRectangle(
      opencv_core.Mat image, opencv_core.Point from, opencv_core.Point to,
      opencv_core.Scalar color) {
    int thickness = opencv_core.FILLED;
    int lineType = opencv_core.LINE_8;
    int shift = 0;
    opencv_imgproc.rectangle(image, from, to, color, thickness, lineType, shift);
  }

  public static opencv_core.Mat crop(opencv_core.Mat image, int x, int y, int width, int height) {
    opencv_core.Rect roi = new opencv_core.Rect(x, y, width, height);
    return image.apply(roi);
  }

}
