package com.zugaldia.robocar.cv;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.JavaCV;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class JavaCVTest {

  private final static double DELTA = 1e-10;

  @Test
  public void testSanity() {
    assertEquals(JavaCV.SQRT2, 1.41421356237309504880, DELTA);
  }

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
    opencv_core.Mat dst = LaneManager.grayscale(src);
    assertNotNull(dst.data());
    assertEquals(dst.size().width(), 960);
    assertEquals(dst.size().height(), 540);
    assertTrue(LaneManager.writeImage("/tmp/solidWhiteCurve.jpg", dst));
  }

  private String getResourcePath(String filename) {
    return getClass().getResource(filename).getPath();
  }
}
