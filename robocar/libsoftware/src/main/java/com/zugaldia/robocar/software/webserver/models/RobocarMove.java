package com.zugaldia.robocar.software.webserver.models;

import com.zugaldia.robocar.software.controller.nes30.NES30Manager;

/**
 * Created by antonio on 4/5/17.
 */

public class RobocarMove {

  @NES30Manager.ButtonCode
  private int keyCode;

  public RobocarMove() {
  }

  public RobocarMove(@NES30Manager.ButtonCode int keyCode) {
    this.keyCode = keyCode;
  }

  @NES30Manager.ButtonCode public int getKeyCode() {
    return keyCode;
  }

  public void setKeyCode(@NES30Manager.ButtonCode int keyCode) {
    this.keyCode = keyCode;
  }
}
