package com.zugaldia.robocar.cv;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;

/**
 * This is a port of the utility methods in Udacity's Lane Finding Project
 * for Self-Driving Car ND to Java/Android.
 * <p>
 * See: https://github.com/udacity/CarND-LaneLines-P1
 */
public class LaneManager {

  private static final opencv_core.Scalar WHITE = new opencv_core.Scalar(255, 255, 255, 0);

  public static opencv_core.Mat readImage(String filename) {
    opencv_core.Mat image = opencv_imgcodecs.imread(filename);
    if (image.data() == null) {
      throw new RuntimeException("The image could not be read (because of missing file, improper permissions, "
        + "unsupported or invalid format).");
    }

    return image;
  }

  public static boolean writeImage(String filename, opencv_core.Mat img) {
    return opencv_imgcodecs.imwrite(filename, img);
  }

  public static opencv_core.Mat doGrayscale(opencv_core.Mat src) {
    opencv_core.Mat dst = new opencv_core.Mat();
    opencv_imgproc.cvtColor(src, dst, opencv_imgproc.COLOR_BGR2GRAY);
    return dst;
  }

  public static opencv_core.Mat doCanny(opencv_core.Mat image, double lowThreshold, double highThreshold) {
    opencv_core.Mat edges = new opencv_core.Mat();
    opencv_imgproc.Canny(image, edges, lowThreshold, highThreshold);
    return edges;
  }

  public static opencv_core.Mat doGaussianBlur(opencv_core.Mat src, int kernelSize) {
    opencv_core.Mat dst = new opencv_core.Mat();
    opencv_core.Size ksize = new opencv_core.Size(kernelSize, kernelSize);
    opencv_imgproc.GaussianBlur(src, dst, ksize, 0);
    return dst;
  }

  public static opencv_core.Mat doHoughLines(opencv_core.Mat image, double rho, double theta, int threshold, double minLineLength, double maxLineGap) {
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

  public static void doDrawLine(opencv_core.Mat image, opencv_core.Point from, opencv_core.Point to, opencv_core.Scalar color, int thickness) {
    int lineType = opencv_core.LINE_8;
    int shift = 0;
    opencv_imgproc.line(image, from, to, color, thickness, lineType, shift);
  }

}
