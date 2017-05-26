package com.zugaldia.robocar.software.camera.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Size;

import java.util.Collections;

import timber.log.Timber;

/**
 * Based on https://github.com/androidthings/sample-tensorflow-imageclassifier/blob/master/app/src/main/java/com/example/androidthings/imageclassifier/CameraHandler.java
 */
public class CameraHandler {
  public static final int IMAGE_WIDTH = 320;
  public static final int IMAGE_HEIGHT = 240;
  private static final int MAX_IMAGES = 1;

  private CameraDevice cameraDevice;
  private CameraCaptureSession captureSession;

  /**
   * An {@link android.media.ImageReader} that handles still image capture.
   */
  private ImageReader imageReader;

  /**
   * Lazy-loaded singleton, so only one instance of the camera is created.
   */
  private CameraHandler() {
  }

  private static class InstanceHolder {
    private static CameraHandler mCamera = new CameraHandler();
  }

  public static CameraHandler getInstance() {
    return InstanceHolder.mCamera;
  }

  /**
   * Initialize the camera device.
   */
  public void initializeCamera(Context context,
                               Handler backgroundHandler,
                               ImageReader.OnImageAvailableListener imageAvailableListener) {
    // Discover the camera instance
    CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    String[] camIds = {};
    try {
      camIds = manager.getCameraIdList();
    } catch (CameraAccessException e) {
      Timber.e(e, "Camera access exception getting IDs.");
    }
    if (camIds.length < 1) {
      Timber.d("No cameras found.");
      return;
    }
    String id = camIds[0];
    Timber.d("Using camera id: %d.", id);

    // Initialize the image processor
    imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
        ImageFormat.YUV_420_888, MAX_IMAGES);
    imageReader.setOnImageAvailableListener(
        imageAvailableListener, backgroundHandler);

    // Open the camera resource
    try {
      if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
          != PackageManager.PERMISSION_GRANTED) {
        Timber.d("Permission to use the camera has not been granted, try rebooting your Robocar.");
        return;
      }
      manager.openCamera(id, stateCallback, backgroundHandler);
    } catch (CameraAccessException cae) {
      Timber.d("Camera access exception", cae);
    }
  }

  /**
   * Callback handling device state changes.
   */
  private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
      Timber.d("Opened camera.");
      CameraHandler.this.cameraDevice = cameraDevice;
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
      Timber.d("Camera disconnected, closing.");
      closeCaptureSession();
      cameraDevice.close();
    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int i) {
      Timber.d("Camera device error, closing.");
      closeCaptureSession();
      cameraDevice.close();
    }

    @Override
    public void onClosed(@NonNull CameraDevice cameraDevice) {
      Timber.d("Closed camera, releasing");
      CameraHandler.this.cameraDevice = null;
    }
  };

  /**
   * Begin a still image capture.
   */
  public void takePicture() {
    if (cameraDevice == null) {
      Timber.w("Cannot capture image. Camera not initialized.");
      return;
    }

    // Here, we create a CameraCaptureSession for capturing still images.
    try {
      cameraDevice.createCaptureSession(
          Collections.singletonList(imageReader.getSurface()),
          sessionCallback,
          null);
    } catch (CameraAccessException e) {
      Timber.d(e, "access exception while preparing pic");
    }
  }

  /**
   * Callback handling session state changes.
   */
  private CameraCaptureSession.StateCallback sessionCallback =
      new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
          // The camera is already closed
          if (cameraDevice == null) {
            return;
          }
          // When the session is ready, we start capture.
          captureSession = cameraCaptureSession;
          triggerImageCapture();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
          Timber.w("Failed to configure camera");
        }
      };

  /**
   * Execute a new capture request within the active session.
   */
  private void triggerImageCapture() {
    try {
      final CaptureRequest.Builder captureBuilder =
          cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      captureBuilder.addTarget(imageReader.getSurface());
      captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
      Timber.d("Capture request created.");
      captureSession.capture(captureBuilder.build(), captureCallback, null);
    } catch (CameraAccessException cae) {
      Timber.d("camera capture exception");
    }
  }

  /**
   * Callback handling capture session events.
   */
  private final CameraCaptureSession.CaptureCallback captureCallback =
      new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
          Timber.d("Partial result");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
          session.close();
          captureSession = null;
          Timber.d("CaptureSession closed");
        }
      };

  private void closeCaptureSession() {
    if (captureSession != null) {
      try {
        captureSession.close();
      } catch (Exception ex) {
        Timber.e("Could not close capture session", ex);
      }
      captureSession = null;
    }
  }

  /**
   * Close the camera resources.
   */
  public void shutDown() {
    closeCaptureSession();
    if (cameraDevice != null) {
      cameraDevice.close();
    }
  }

  /**
   * Helpful debugging method:  Dump all supported camera formats to log.  You don't need to run
   * this for normal operation, but it's very helpful when porting this code to different
   * hardware.
   */
  public static void dumpFormatInfo(Context context) {
    CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    String[] camIds = {};
    try {
      camIds = manager.getCameraIdList();
    } catch (CameraAccessException e) {
      Timber.d("Cam access exception getting IDs");
    }
    if (camIds.length < 1) {
      Timber.d("No cameras found");
    }
    String id = camIds[0];
    Timber.d("Using camera id " + id);
    try {
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
      StreamConfigurationMap configs = characteristics.get(
          CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      for (int format : configs.getOutputFormats()) {
        Timber.d("Getting sizes for format: " + format);
        for (Size s : configs.getOutputSizes(format)) {
          Timber.d("\t" + s.toString());
        }
      }
      int[] effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
      for (int effect : effects) {
        Timber.d("Effect available: " + effect);
      }
    } catch (CameraAccessException e) {
      Timber.d("Cam access exception getting characteristics.");
    }
  }
}