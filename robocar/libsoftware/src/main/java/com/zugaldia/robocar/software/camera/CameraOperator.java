package com.zugaldia.robocar.software.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.Surface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

/**
 * This class uses the camera2 API to open a session, able to take multiple quick photos, and
 * close the session once the training data has been collected. It uses the default camera, at
 * the lowest resolution, using automatic settings (3A; do we need to lock focus?). Files are stored
 * in external storage for easier access.
 */

public class CameraOperator implements
    SessionCallback.SessionCallbackListener,
    ImageReader.OnImageAvailableListener {

  private static final int CAMERA_INDEX = 0;
  private static final int IMAGE_WIDTH = 320;
  private static final int IMAGE_HEIGHT = 240;
  private static final int IMAGE_FORMAT = ImageFormat.JPEG;
  private static final int MAX_IMAGES = 5;
  private static final String ROBOCAR_FOLDER = "robocar";
  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US);

  private CameraOperatorListener listener;
  private SpeedOwner speedOwner;

  private boolean inSession;
  private File root;
  private boolean autofocusSupported = false;

  private DeviceCallback deviceCallback;
  private SessionCallback sessionCallback;
  private CaptureCallback captureCallback;

  private HandlerThread handlerThread;
  private Handler backgroundHandler;

  private ImageReader imageReader;
  private String sessionId;
  private int sessionCount;

  public CameraOperator(Context context, CameraOperatorListener listener, SpeedOwner speedOwner) {
    Timber.d("Building camera training object.");
    this.listener = listener;
    this.speedOwner = speedOwner;

    try {
      init(context);
    } catch (CameraAccessException e) {
      Timber.e(e, "Failed to initialize camera training.");
    }
  }

  public boolean isInSession() {
    return inSession;
  }

  public boolean isAutofocusSupported() {
    return autofocusSupported;
  }

  private void init(Context context) throws CameraAccessException {
    inSession = false;

    if (!ImageSaver.isExternalStorageWritable()) {
      Timber.e("Cannot save file, external storage is not writable.");
      return;
    }

    root = ImageSaver.getRoot(ROBOCAR_FOLDER);
    if (root == null) {
      Timber.e("Failed to create destination folder.");
      return;
    }

    CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    String[] cameras = manager.getCameraIdList();
    if (cameras.length == 0) {
      Timber.e("No cameras available.");
      return;
    }

    Timber.d("Default camera selected (%s), %d cameras found.",
        cameras[CAMERA_INDEX], cameras.length);

    if (ActivityCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      Timber.d("Camera permission not granted yet, restart your device.");
      return;
    }

    // Debug and check for autofocus support
    dumpFormatInfo(manager, cameras[CAMERA_INDEX]);

    startBackgroundThread();
    deviceCallback = new DeviceCallback();
    manager.openCamera(cameras[CAMERA_INDEX], deviceCallback, backgroundHandler);
  }

  /**
   * Starts a background thread and its {@link Handler}.
   */
  private void startBackgroundThread() {
    handlerThread = new HandlerThread("CAMERA_BACKGROUND");
    handlerThread.start();
    backgroundHandler = new Handler(handlerThread.getLooper());
  }

  /**
   * Stops the background thread and its {@link Handler}.
   */
  private void stopBackgroundThread() {
    handlerThread.quitSafely();
    try {
      // Waits for this thread to die
      handlerThread.join();
      handlerThread = null;
      backgroundHandler = null;
    } catch (InterruptedException e) {
      Timber.e(e, "Failed to stop background thread.");
    }
  }

  public void startSession() {
    Timber.d("Starting a session.");
    if (inSession) {
      Timber.d("Session already started, end it first.");
      return;
    }

    CameraDevice cameraDevice = deviceCallback != null ? deviceCallback.getCameraDevice() : null;
    if (cameraDevice == null) {
      Timber.e("Cannot open a session because no camera device is opened.");
      return;
    }

    imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_FORMAT, MAX_IMAGES);
    imageReader.setOnImageAvailableListener(this, backgroundHandler);
    List<Surface> outputs = Collections.singletonList(imageReader.getSurface());
    sessionCallback = new SessionCallback(this);
    try {
      Timber.d("Creating a camera session.");
      cameraDevice.createCaptureSession(outputs, sessionCallback, backgroundHandler);
    } catch (CameraAccessException e) {
      Timber.e(e, "Failed to start a camera session.");
    }

    sessionId = UUID.randomUUID().toString().replace("-", "");
    sessionCount = 0;
  }

  public void takePicture() {
    Timber.d("Taking a picture.");
    if (!inSession) {
      Timber.d("Cannot take a picture because no session is started.");
      return;
    }

    CameraDevice cameraDevice = deviceCallback != null ? deviceCallback.getCameraDevice() : null;
    if (cameraDevice == null) {
      Timber.e("Cannot open a capture request because no camera device is opened.");
      return;
    }

    try {
      CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(
          CameraDevice.TEMPLATE_STILL_CAPTURE);
      builder.addTarget(imageReader.getSurface());
      // The camera device's autoexposure routine is active, with no flash control
      builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      // The camera device's auto-white balance routine is active
      builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
      if (isAutofocusSupported()) {
        // Basic automatic focus mode
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
      }
      CaptureRequest captureRequest = builder.build();
      captureCallback = new CaptureCallback();
      sessionCallback.getSession().capture(captureRequest, captureCallback, backgroundHandler);
    } catch (CameraAccessException e) {
      Timber.e(e, "Failed to create a capture request.");
    }
  }

  public void endSession() {
    Timber.d("Ending a session.");
    if (!inSession) {
      Timber.d("Session not started, start it first.");
      return;
    }

    captureCallback.closeSession();
    sessionCallback.closeSession();
    deviceCallback.closeDevice();
    if (imageReader != null) {
      imageReader.close();
      imageReader = null;
    }

    stopBackgroundThread();
    inSession = false;
  }

  /**
   * This method indicates the camera is ready to take picture requests.
   */
  @Override
  public void onConfigured() {
    Timber.d("Session configured.");
    inSession = true;
    listener.sessionStarted();
  }

  /**
   * This method indicates we got an image from the camera.
   */
  @Override
  public void onImageAvailable(ImageReader reader) {
    Timber.d("Image available.");
    String timestamp = dateFormat.format(new Date());
    int[] speeds = speedOwner.getSpeeds();
    String speedState = String.format(Locale.US, "%d-%d-%d-%d",
        speeds[0], speeds[1], speeds[2], speeds[3]);
    String filename = String.format(Locale.US, "robocar-%s-%d-%s-%s.jpg",
        sessionId, sessionCount++, timestamp, speedState);
    backgroundHandler.post(new ImageSaver(reader.acquireLatestImage(), root, filename));
  }

  /**
   * Helpful debugging method:  Dump all supported camera formats to log.  You don't need to run
   * this for normal operation, but it's very helpful when porting this code to different
   * hardware.
   */
  private void dumpFormatInfo(CameraManager manager, String cameraId) {
    try {
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

      StreamConfigurationMap configs = characteristics.get(
          CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      for (int format : configs.getOutputFormats()) {
        if (format == IMAGE_FORMAT) {
          Timber.d("Getting sizes for format: %d.", format);
          for (Size s : configs.getOutputSizes(format)) {
            Timber.d("Supported size: %s", s.toString());
            // It should include IMAGE_WIDTH x IMAGE_HEIGHT
            if (s.getWidth() == IMAGE_WIDTH && s.getHeight() == IMAGE_HEIGHT) {
              Timber.d("(currently selected ^^^)", s.toString());
            }
          }
        }
      }

      int[] effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
      for (int effect : effects) {
        switch (effect) {
          case CaptureRequest.CONTROL_EFFECT_MODE_OFF:
            Timber.d("Supported effect: OFF.");
            break;
          case CaptureRequest.CONTROL_EFFECT_MODE_MONO:
            Timber.d("Supported effect: MONO.");
            break;
          case CaptureRequest.CONTROL_EFFECT_MODE_NEGATIVE:
            Timber.d("Supported effect: NEGATIVE.");
            break;
          case CaptureRequest.CONTROL_EFFECT_MODE_SOLARIZE:
            Timber.d("Supported effect: SOLARIZE.");
            break;
          case CaptureRequest.CONTROL_EFFECT_MODE_SEPIA:
            Timber.d("Supported effect: SEPIA.");
            break;
          case CaptureRequest.CONTROL_EFFECT_MODE_POSTERIZE:
            Timber.d("Supported effect: POSTERIZE.");
            break;
          case CaptureRequest.CONTROL_EFFECT_MODE_WHITEBOARD:
            Timber.d("Supported effect: WHITEBOARD.");
            break;
          case CaptureRequest.CONTROL_EFFECT_MODE_BLACKBOARD:
            Timber.d("Supported effect: BLACKBOARD.");
            break;
          case CaptureRequest.CONTROL_EFFECT_MODE_AQUA:
            Timber.d("Supported effect: AQUA.");
            break;
          default:
            Timber.d("Unknown effect available: %d", effect);
            break;
        }
      }

      int[] modes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
      for (int mode : modes) {
        if (mode == CameraMetadata.CONTROL_AF_MODE_AUTO) {
          autofocusSupported = true;
          break;
        }
      }

      if (isAutofocusSupported()) {
        Timber.d("Autofocus is supported.");
      } else {
        Timber.d("Autofocus is NOT supported.");
      }
    } catch (CameraAccessException e) {
      Timber.e(e, "Camera access exception getting characteristics.");
    } catch (Exception e) {
      Timber.e(e, "Error getting characteristics.");
    }
  }
}
