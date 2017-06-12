package com.zugaldia.robocar.software.camera;

import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;

import timber.log.Timber;

/**
 * Callback for a device connection
 */
public class DeviceCallback extends CameraDevice.StateCallback {

  private CameraDevice cameraDevice;

  public CameraDevice getCameraDevice() {
    return cameraDevice;
  }

  public void closeDevice() {
    if (cameraDevice != null) {
      cameraDevice.close();
      cameraDevice = null;
    }
  }

  @Override
  public void onOpened(@NonNull CameraDevice cameraDevice) {
    Timber.d("Camera opened: %s.", cameraDevice.getId());
    this.cameraDevice = cameraDevice;
  }

  @Override
  public void onDisconnected(@NonNull CameraDevice cameraDevice) {
    Timber.d("Camera disconnected: %s.", cameraDevice.getId());
    this.cameraDevice = cameraDevice;
    closeDevice();
  }

  @Override
  public void onError(@NonNull CameraDevice cameraDevice, int error) {
    switch (error) {
      case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
        Timber.e("Camera error: ERROR_CAMERA_IN_USE");
        break;
      case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
        Timber.e("Camera error: ERROR_MAX_CAMERAS_IN_USE");
        break;
      case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
        Timber.e("Camera error: ERROR_CAMERA_DISABLED");
        break;
      case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
        Timber.e("Camera error: ERROR_CAMERA_DEVICE");
        break;
      case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
        Timber.e("Camera error: ERROR_CAMERA_SERVICE");
        break;
      default:
        Timber.e("Camera error: UNKNOWN");
        break;
    }

    this.cameraDevice = cameraDevice;
    closeDevice();
  }
}
