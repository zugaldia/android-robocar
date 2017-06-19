package com.zugaldia.robocar.cv;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LaneManagerTest extends BaseTest {

  private final static double DELTA = 0.001;

  @Test
  public void testReadImage() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/solidWhiteRight.jpg"));
    assertNotNull(src.data());
    assertEquals(src.size().width(), 960);
    assertEquals(src.size().height(), 540);
  }

  @Test
  public void testGrayscale() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat dst = LaneManager.doGrayscale(src);
    assertNotNull(dst.data());
    assertEquals(dst.size().width(), 960);
    assertEquals(dst.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_grayscale.jpg", dst));
  }

  @Test
  public void testCanny() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat edges = LaneManager.doCanny(src, 50, 150);
    assertNotNull(edges.data());
    assertEquals(edges.size().width(), 960);
    assertEquals(edges.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_canny.jpg", edges));
  }

  @Test
  public void testGaussianBlur() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat gaussian = LaneManager.doGaussianBlur(src, 25);
    assertNotNull(gaussian.data());
    assertEquals(gaussian.size().width(), 960);
    assertEquals(gaussian.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_gaussian.jpg", gaussian));
  }

  @Test
  public void testHoughLines() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat gray = LaneManager.doGrayscale(src);
    double rho = 1;
    double theta = Math.PI / 180;
    int threshold = 1;
    double minLineLength = 10;
    double maxLineGap = 1;
    opencv_core.Mat lines = LaneManager.doHoughLines(
        gray, rho, theta, threshold, minLineLength, maxLineGap);
    assertNotNull(lines.data());
    assertEquals(lines.rows(), 960);
    assertEquals(lines.cols(), 1);
    assertEquals(lines.channels(), 4);

    IntIndexer linesIndexer = lines.createIndexer();
    assertEquals(linesIndexer.rows(), 960);
    assertEquals(linesIndexer.cols(), 1);
    assertEquals(linesIndexer.channels(), 4);

    assertEquals(linesIndexer.get(0, 0, 0), 885);
    assertEquals(linesIndexer.get(0, 0, 1), 539);
    assertEquals(linesIndexer.get(0, 0, 2), 885);
    assertEquals(linesIndexer.get(0, 0, 3), 0);
  }

  @Test
  public void testApplyMask() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/solidWhiteRight.jpg"));
    int[] points = new int[] {
        0, src.size().height(), // bottom left
        src.size().width() / 2, src.size().height() / 2, // middle point
        src.size().width(), src.size().height()}; // // bottom right
    opencv_core.Mat masked = LaneManager.applyMask(src, points);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_masked.jpg", masked));
  }

  @Test
  public void testPerspectiveTransform() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/straightLines.jpg"));
    float[] source = new float[] {
        594, 451,
        685, 451,
        268, 677,
        1037, 677};

    float offset_x = 100;
    float offset_y = offset_x * src.size().height() / src.size().width();
    float[] destination = new float[] {
        source[4] + offset_x, offset_y,
        source[6] - offset_x, offset_y,
        source[4] + offset_x, src.size().height(),
        source[6] - offset_x, src.size().height()};
    opencv_core.Mat warped = LaneManager.perspectiveTransform(src, source, destination);
    assertTrue(LaneManager.writeImage("/tmp/straightLines_warped.jpg", warped));
  }

  @Test
  public void testGetSaturationChannel() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/straightLines.jpg"));
    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(src);
    assertTrue(LaneManager.writeImage("/tmp/straightLines_saturationChannel.jpg", saturationChannel));
  }

  @Test
  public void testThreshold() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/straightLines.jpg"));
    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(src);
    opencv_core.Mat thresholdBinary = LaneManager.threshold(saturationChannel, 150);
    assertTrue(LaneManager.writeImage("/tmp/straightLines_thresholded.jpg", thresholdBinary));
  }

  @Test
  public void testHistogramArray() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/straightLines.jpg"));
    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(src);
    opencv_core.Mat thresholdBinary = LaneManager.threshold(saturationChannel, 150);
    int[] histogram = LaneManager.histogramArray(thresholdBinary);

    assertEquals(histogram.length, LaneManager.HISTOGRAM_BINS);
    drawHistogram(histogram, thresholdBinary, "/tmp/straightLines_histogramArray.jpg");
  }

  @Test
  public void testHistogram() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/straightLines.jpg"));
    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(src);
    opencv_core.Mat histogram = LaneManager.histogram(saturationChannel);
    assertTrue(LaneManager.writeImage("/tmp/straightLines_histogram.jpg", histogram));

    assertEquals(histogram.size().height(), 32);
    assertEquals(histogram.size().width(), 1);
    assertEquals(histogram.rows(), 32);
    assertEquals(histogram.cols(), 1);
  }

  @Test
  public void testGetMaxPosition() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/straightLines.jpg"));
    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(src);
    opencv_core.Mat histogram = LaneManager.histogram(saturationChannel);

    // Compute manual
    float max = -1.0f;
    int maxIndex = -1;
    FloatIndexer idx = histogram.createIndexer();
    for (int row = 0; row < histogram.rows(); row++) {
      for (int col = 0; col < histogram.cols(); col++) {
        float value = idx.get(row, col);
        //System.out.println(String.format(Locale.US, "row: %d, col: %d, value: %f", row, col, value));
        if (max == -1 || value > max) {
          maxIndex = row;
          max = value;
        }
      }
    }

    // Compute with OpenCV
    HistogramPosition position = LaneManager.getMaxPosition(histogram);

    // Both results should match
    assertEquals(position.getBinIndex(), 4);
    assertEquals(position.getBinValue(), 86152.0, DELTA);
    assertEquals(position.getBinIndex(), maxIndex);
    assertEquals(position.getBinValue(), max, DELTA);
  }

  @Test
  public void testFindLane() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/straightLines.jpg"));

    float[] fromPoints = getSampleFromPoints();
    opencv_core.Mat warped = LaneManager.perspectiveTransform(
        src, fromPoints, getSampleToPoints(fromPoints, src.size().width(), src.size().height()));

    opencv_core.Mat cropped = LaneManager.crop(warped,
        0, warped.size().height() / 2,
        warped.size().width(), warped.size().height() / 2);

    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(cropped);
    opencv_core.Mat thresholdBinary = LaneManager.threshold(saturationChannel, 150);

    int[] histogram = LaneManager.histogramArray(thresholdBinary);
    HistogramPosition position = LaneManager.getMaxPosition(histogram);
    assertEquals(position.getBinIndex(), 9);
    assertEquals(position.getBinValue(), 5147, DELTA);

    drawHistogram(histogram, thresholdBinary, "/tmp/straightLines_lane.jpg");
  }

  @Test
  public void testDrawLine() {
    opencv_core.Mat src = LaneManager.readImage(
        getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Scalar color = new opencv_core.Scalar(255, 0, 0, 0); // Blue (BGR)
    LaneManager.drawLine(src,
        new opencv_core.Point(0, 0), // top left
        new opencv_core.Point(src.size().width(), src.size().height()), // bottom right
        color, 5);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_line.jpg", src));
  }

  @Test
  public void testLaneDetection() {
    opencv_core.Mat original = LaneManager.readImage(
        getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat grayscale = LaneManager.doGrayscale(original);
    opencv_core.Mat gaussian = LaneManager.doGaussianBlur(grayscale, 5);
    opencv_core.Mat edges = LaneManager.doCanny(gaussian, 50, 150);

    opencv_core.Scalar color = new opencv_core.Scalar(0, 255, 0, 0); // Green (BGR)
    opencv_core.Mat lines = LaneManager.doHoughLines(edges, 1, Math.PI / 180, 5, 20, 1);
    IntIndexer linesIndexer = lines.createIndexer();
    for (int i = 0; i < linesIndexer.rows(); i++) {
      for (int j = 0; j < linesIndexer.cols(); j++) {
        LaneManager.drawLine(original,
            new opencv_core.Point(linesIndexer.get(i, j, 0), linesIndexer.get(i, j, 1)),
            new opencv_core.Point(linesIndexer.get(i, j, 2), linesIndexer.get(i, j, 3)),
            color, 5);
      }
    }

    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_lanes.jpg", original));
  }

  private String getResourcePath(String filename) {
    return getClass().getResource(filename).getPath();
  }
}
