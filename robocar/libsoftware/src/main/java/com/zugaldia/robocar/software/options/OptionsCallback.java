package com.zugaldia.robocar.software.options;

/**
 * Options file is ready asynchronously.
 */
public interface OptionsCallback {

  void onLoad();
  void onError(String error);

}
