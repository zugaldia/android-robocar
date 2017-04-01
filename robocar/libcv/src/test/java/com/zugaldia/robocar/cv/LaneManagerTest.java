package com.zugaldia.robocar.cv;

import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LaneManagerTest {

  @Test
  public void testReadImage() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteRight.jpg"));
    assertNotNull(src.data());
    assertEquals(src.size().width(), 960);
    assertEquals(src.size().height(), 540);
  }

  @Test
  public void testGrayscale() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat dst = LaneManager.doGrayscale(src);
    assertNotNull(dst.data());
    assertEquals(dst.size().width(), 960);
    assertEquals(dst.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_grayscale.jpg", dst));
  }

  @Test
  public void testCanny() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat edges = LaneManager.doCanny(src, 50, 150);
    assertNotNull(edges.data());
    assertEquals(edges.size().width(), 960);
    assertEquals(edges.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_canny.jpg", edges));
  }

  @Test
  public void testGaussianBlur() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat gaussian = LaneManager.doGaussianBlur(src, 25);
    assertNotNull(gaussian.data());
    assertEquals(gaussian.size().width(), 960);
    assertEquals(gaussian.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_gaussian.jpg", gaussian));
  }

  @Test
  public void testHoughLines() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat gray = LaneManager.doGrayscale(src);
    double rho = 1;
    double theta = Math.PI / 180;
    int threshold = 1;
    double minLineLength = 10;
    double maxLineGap = 1;
    opencv_core.Mat lines = LaneManager.doHoughLines(gray, rho, theta, threshold, minLineLength, maxLineGap);
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
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteRight.jpg"));
    int[] points = new int[] {
      0, src.size().height(), // bottom left
      src.size().width() / 2, src.size().height() / 2, // middle point
      src.size().width(), src.size().height()}; // // bottom right
    opencv_core.Mat masked = LaneManager.applyMask(src, points);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_masked.jpg", masked));
  }

  @Test
  public void testDrawLine() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Scalar color = new opencv_core.Scalar(255, 0, 0, 0); // Blue (BGR)
    LaneManager.doDrawLine(src,
      new opencv_core.Point(0, 0), // top left
      new opencv_core.Point(src.size().width(), src.size().height()), // bottom right
      color, 5);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteRight_line.jpg", src));
  }

  @Test
  public void testLaneDetection() {
    opencv_core.Mat original = LaneManager.readImage(getResourcePath("/test_images/solidWhiteRight.jpg"));
    opencv_core.Mat grayscale = LaneManager.doGrayscale(original);
    opencv_core.Mat gaussian = LaneManager.doGaussianBlur(grayscale, 5);
    opencv_core.Mat edges = LaneManager.doCanny(gaussian, 50, 150);

    opencv_core.Scalar color = new opencv_core.Scalar(0, 255, 0, 0); // Green (BGR)
    opencv_core.Mat lines = LaneManager.doHoughLines(edges, 1, Math.PI / 180, 5, 20, 1);
    IntIndexer linesIndexer = lines.createIndexer();
    for (int i = 0; i < linesIndexer.rows(); i++) {
      for (int j = 0; j < linesIndexer.cols(); j++) {
        LaneManager.doDrawLine(original,
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
