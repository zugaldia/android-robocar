package com.zugaldia.robocar.software.webserver.models;

import com.zugaldia.robocar.software.controller.nes30.Nes30Manager;

/**
 * Created by antonio on 4/5/17.
 */

public class RobocarMove {

  @Nes30Manager.ButtonCode
  private int keyCode;

  public RobocarMove() {
  }

  public RobocarMove(@Nes30Manager.ButtonCode int keyCode) {
    this.keyCode = keyCode;
  }

  @Nes30Manager.ButtonCode public int getKeyCode() {
    return keyCode;
  }

  public void setKeyCode(@Nes30Manager.ButtonCode int keyCode) {
    this.keyCode = keyCode;
  }
}
