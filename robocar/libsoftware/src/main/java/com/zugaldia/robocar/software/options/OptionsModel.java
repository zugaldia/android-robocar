package com.zugaldia.robocar.software.options;

import com.google.gson.annotations.SerializedName;

/**
 * This model represents the structure of the local JSON settings. You can find a sample file on
 * options/robocar.json in this repo. To copy the file to your Robocar you can use adb:
 * $ adb push robocar.json /storage/emulated/0
 */
public class OptionsModel {

  @SerializedName("bluetooth_name")
  private String bluetoothName;

  @SerializedName("bluetooth_address")
  private String bluetoothAddress;

  public OptionsModel() {
    // Required no-args constructor
  }

  public String getBluetoothName() {
    return bluetoothName;
  }

  public String getBluetoothAddress() {
    return bluetoothAddress;
  }
}
