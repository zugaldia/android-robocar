package com.zugaldia.robocar.software.controller.nes30;

/**
 * Created by antonio on 2/25/17.
 */

public interface NES30Listener {

  void onKeyPress(@NES30Manager.ButtonCode int keyCode, boolean isDown);

}
