package com.zugaldia.robocar.cv;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LaneManagerTest {

  private final static double DELTA = 1e-10;

  @Test
  public void testReadImage() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteCurve.jpg"));
    assertNotNull(src.data());
    assertEquals(src.size().width(), 960);
    assertEquals(src.size().height(), 540);
  }

  @Test
  public void testGrayscale() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteCurve.jpg"));
    opencv_core.Mat dst = LaneManager.doGrayscale(src);
    assertNotNull(dst.data());
    assertEquals(dst.size().width(), 960);
    assertEquals(dst.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteCurve_grayscale.jpg", dst));
  }

  @Test
  public void testCanny() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteCurve.jpg"));
    opencv_core.Mat edges = LaneManager.doCanny(src, 50, 150);
    assertNotNull(edges.data());
    assertEquals(edges.size().width(), 960);
    assertEquals(edges.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteCurve_canny.jpg", edges));
  }

  @Test
  public void testGaussianBlur() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteCurve.jpg"));
    opencv_core.Mat gaussian = LaneManager.doGaussianBlur(src, 25);
    assertNotNull(gaussian.data());
    assertEquals(gaussian.size().width(), 960);
    assertEquals(gaussian.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteCurve_gaussian.jpg", gaussian));
  }

  @Test
  public void testApplyMask() {
    opencv_core.Mat src = LaneManager.readImage(getResourcePath("/test_images/solidWhiteCurve.jpg"));

    opencv_core.MatVector points = new opencv_core.MatVector(
      new opencv_core.Mat(3, 2, opencv_core.CV_32F, new IntPointer(1, 2, 3, 4, 5, 6))
    );

    opencv_core.MatVector vertices = new opencv_core.MatVector(points);
    opencv_core.Mat masked = LaneManager.applyMask(src, vertices);

    assertNotNull(masked.data());
    assertEquals(masked.size().width(), 960);
    assertEquals(masked.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteCurve_masked.jpg", masked));
  }

  private String getResourcePath(String filename) {
    return getClass().getResource(filename).getPath();
  }
}
