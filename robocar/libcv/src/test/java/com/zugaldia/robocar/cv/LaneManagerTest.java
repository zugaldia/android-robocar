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

  private final static String TEST_IMAGE = "/robocar.jpg";
  private final static int TEST_IMAGE_WIDTH = 320;
  private final static int TEST_IMAGE_HEIGHT = 240;

  @Test
  public void testReadImage() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    assertNotNull(src.data());
    assertEquals(src.size().width(), TEST_IMAGE_WIDTH);
    assertEquals(src.size().height(), TEST_IMAGE_HEIGHT);
  }

  @Test
  public void testGrayscale() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat dst = LaneManager.doGrayscale(src);
    assertNotNull(dst.data());
    assertEquals(dst.size().width(), TEST_IMAGE_WIDTH);
    assertEquals(dst.size().height(), TEST_IMAGE_HEIGHT);
    assertTrue(LaneManager.writeImage("/tmp/robocar_grayscale.jpg", dst));
  }

  @Test
  public void testCanny() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat edges = LaneManager.doCanny(src, 50, 150);
    assertNotNull(edges.data());
    assertEquals(edges.size().width(), TEST_IMAGE_WIDTH);
    assertEquals(edges.size().height(), TEST_IMAGE_HEIGHT);
    assertTrue(LaneManager.writeImage("/tmp/robocar_canny.jpg", edges));
  }

  @Test
  public void testGaussianBlur() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat gaussian = LaneManager.doGaussianBlur(src, 25);
    assertNotNull(gaussian.data());
    assertEquals(gaussian.size().width(), TEST_IMAGE_WIDTH);
    assertEquals(gaussian.size().height(), TEST_IMAGE_HEIGHT);
    assertTrue(LaneManager.writeImage("/tmp/robocar_gaussian.jpg", gaussian));
  }

  @Test
  public void testHoughLines() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat gray = LaneManager.doGrayscale(src);

    double rho = 1;
    double theta = Math.PI / 180;
    int threshold = 1;
    double minLineLength = 10;
    double maxLineGap = 1;
    opencv_core.Mat lines = LaneManager.doHoughLines(
        gray, rho, theta, threshold, minLineLength, maxLineGap);

    assertNotNull(lines.data());
    assertEquals(lines.rows(), TEST_IMAGE_WIDTH);
    assertEquals(lines.cols(), 1);
    assertEquals(lines.channels(), 4);

    IntIndexer linesIndexer = lines.createIndexer();
    assertEquals(linesIndexer.rows(), TEST_IMAGE_WIDTH);
    assertEquals(linesIndexer.cols(), 1);
    assertEquals(linesIndexer.channels(), 4);

    assertEquals(linesIndexer.get(0, 0, 0), 245);
    assertEquals(linesIndexer.get(0, 0, 1), 239);
    assertEquals(linesIndexer.get(0, 0, 2), 245);
    assertEquals(linesIndexer.get(0, 0, 3), 0);

    assertTrue(LaneManager.writeImage("/tmp/robocar_hough.jpg", lines));
  }

  @Test
  public void testApplyMask() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    int[] points = new int[] {
        0, src.size().height(), // bottom left
        src.size().width() / 2, src.size().height() / 2, // middle point
        src.size().width(), src.size().height()}; // // bottom right
    opencv_core.Mat masked = LaneManager.applyMask(src, points);
    assertTrue(LaneManager.writeImage("/tmp/robocar_masked.jpg", masked));
  }

  @Test
  public void testPerspectiveTransform() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat warped = LaneManager.perspectiveTransform(
        src, LaneManager.SOURCE_POINTS, LaneManager.getToPoints(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT));
    assertTrue(LaneManager.writeImage("/tmp/robocar_warped.jpg", warped));
  }

  @Test
  public void testGetSaturationChannel() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(src);
    assertTrue(LaneManager.writeImage("/tmp/robocar_saturationChannel.jpg", saturationChannel));
  }

  @Test
  public void testGetYuvChannel() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat yuvChannel = LaneManager.getYuvChannel(src);
    assertTrue(LaneManager.writeImage("/tmp/robocar_yuvChannel.jpg", yuvChannel));
  }

  @Test
  public void testThreshold() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(src);
    opencv_core.Mat thresholdBinary = LaneManager.threshold(saturationChannel, 150);
    assertTrue(LaneManager.writeImage("/tmp/robocar_threshold.jpg", thresholdBinary));
  }

  @Test
  public void testThresholdColor() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat thresholdBinary = LaneManager.thresholdColor(src);
    assertTrue(LaneManager.writeImage("/tmp/robocar_threshold_color.jpg", thresholdBinary));
  }

  @Test
  public void testHistogramArray() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(src);
    opencv_core.Mat thresholdBinary = LaneManager.threshold(saturationChannel, 150);
    int[] histogram = LaneManager.histogramArray(thresholdBinary);

    assertEquals(histogram.length, LaneManager.HISTOGRAM_BINS);
    drawHistogram(histogram, thresholdBinary, "/tmp/robocar_histogramArray.jpg");
  }

  @Test
  public void testHistogram() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Mat saturationChannel = LaneManager.getSaturationChannel(src);
    opencv_core.Mat histogram = LaneManager.histogram(saturationChannel);
    assertTrue(LaneManager.writeImage("/tmp/robocar_histogram.jpg", histogram));

    assertEquals(histogram.size().height(), 32);
    assertEquals(histogram.size().width(), 1);
    assertEquals(histogram.rows(), 32);
    assertEquals(histogram.cols(), 1);
  }

  @Test
  public void testGetMaxPosition() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
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
    assertEquals(position.getBinIndex(), 21);
    assertEquals(position.getBinValue(), 21541.0, DELTA);
    assertEquals(position.getBinIndex(), maxIndex);
    assertEquals(position.getBinValue(), max, DELTA);
  }

  @Test
  public void testFindLane() {
    String[] testImages = new String[] {
        TEST_IMAGE,
        "/robocar-lanetest-01.jpg",
        "/robocar-lanetest-02.jpg",
        "/robocar-lanetest-03.jpg",
        "/robocar-lanetest-04.jpg",
        "/robocar-lanetest-05.jpg",
        "/robocar-lanetest-06.jpg",
        "/robocar-lanetest-07.jpg",
        "/robocar-lanetest-08.jpg",
        "/robocar-lanetest-09.jpg",
        "/robocar-lanetest-10.jpg",};
    for (String testImage: testImages) {
      opencv_core.Mat src = LaneManager.readImage(getResourcePath(testImage));
      HistogramPosition position = LaneManager.findLane(src);

      // Print line
      int xPos = (int) Math.floor(TEST_IMAGE_WIDTH * position.getBinIndex() / LaneManager.HISTOGRAM_BINS);
      opencv_core.Scalar color = new opencv_core.Scalar(255, 0, 0, 0); // Blue (BGR)
      LaneManager.drawLine(src,
          new opencv_core.Point(xPos, 0), // top left
          new opencv_core.Point(xPos, src.size().height()), // bottom right
          color, 5);
      assertTrue(LaneManager.writeImage("/tmp" + testImage, src));
    }
  }

  @Test
  public void testDrawLine() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath(TEST_IMAGE));
    opencv_core.Scalar color = new opencv_core.Scalar(255, 0, 0, 0); // Blue (BGR)
    LaneManager.drawLine(src,
        new opencv_core.Point(0, 0), // top left
        new opencv_core.Point(src.size().width(), src.size().height()), // bottom right
        color, 5);
    assertTrue(LaneManager.writeImage("/tmp/robocar_line.jpg", src));
  }

  @Test
  public void testLaneDetection() {
    opencv_core.Mat original = LaneManager.readImage(getResourcePath(TEST_IMAGE));
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

    assertTrue(LaneManager.writeImage("/tmp/robocar_lanes.jpg", original));
  }

  private String getResourcePath(String filename) {
    return getClass().getResource(filename).getPath();
  }
}
