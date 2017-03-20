package com.zugaldia.robocar.cv;

import org.bytedeco.javacv.JavaCV;
import org.junit.Test;

import static org.junit.Assert.*;

public class JavaCVTest {

  private final static double DELTA = 1e-10;

  @Test
  public void testSanity() {
    assertEquals(JavaCV.SQRT2, 1.41421356237309504880, DELTA);
  }
}
