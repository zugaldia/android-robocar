package com.zugaldia.robocar.app.autonomous;

import android.content.Context;
import android.media.ImageReader;

import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;
import com.zugaldia.robocar.software.camera.CameraOperator;
import com.zugaldia.robocar.software.camera.CameraOperatorListener;
import com.zugaldia.robocar.software.camera.ImageSaver;

import java.io.File;

import timber.log.Timber;

/**
 * Autonomous driving using CV processing on camera photos.
 */
public class CameraDriver implements CameraOperatorListener, ImageReader.OnImageAvailableListener {

  private final static String PHOTO_FILENAME = "cv.jpg";

  private AdafruitMotorHat motorHat;
  private CameraOperator cameraOperator;

  public CameraDriver(Context context, AdafruitMotorHat motorHat) {
    this.motorHat = motorHat;
    cameraOperator = new CameraOperator(context, this);
  }

  public void start() {
    cameraOperator.startSession(this);
  }

  public void stop() {
    cameraOperator.endSession();
  }

  @Override
  public void sessionStarted() {
    Timber.d("Camera is ready, taking a picture.");
    cameraOperator.takePicture();
  }

  @Override
  public void onImageAvailable(final ImageReader reader) {
    Timber.d("Picture is available.");
    new Thread(new Runnable() {
      @Override
      public void run() {
        File root = ImageSaver.getRoot(CameraOperator.ROBOCAR_FOLDER);
        new ImageSaver(reader.acquireLatestImage(), root, PHOTO_FILENAME).run();
        processPhoto(root, PHOTO_FILENAME);
      }
    }).start();
  }

  private void processPhoto(File root, String filename) {
    File path = new File(root, filename);
    Timber.d("TODO: Process %s", path.getAbsolutePath());
  }
}
