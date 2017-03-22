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

  public static opencv_core.Mat applyMask(opencv_core.Mat image, opencv_core.MatVector vertices) {
    opencv_core.Mat mask = opencv_core.Mat.zeros(image.size(), opencv_core.CV_8U).asMat();

    opencv_core.Scalar color = new opencv_core.Scalar(image.channels()); // 3
    double[] colors = new double[] {
      255.0, 255.0, 255.0, 255.0,
      255.0, 255.0, 255.0, 255.0,
      255.0, 255.0, 255.0, 255.0};
    color.put(colors, 0, colors.length);

    opencv_imgproc.fillPoly(mask, vertices, color);

    opencv_core.Mat dst = new opencv_core.Mat();
    opencv_core.bitwise_and(image, mask, dst);
    return dst;
  }
}
