package com.zugaldia.robocar.cv;

/**
 * Created by antonio on 6/16/17.
 */

public class HistogramPosition {
  private int binIndex;
  private double binValue;

  public HistogramPosition(int binIndex, double binValue) {
    this.binIndex = binIndex;
    this.binValue = binValue;
  }

  public int getBinIndex() {
    return binIndex;
  }

  public double getBinValue() {
    return binValue;
  }
}
