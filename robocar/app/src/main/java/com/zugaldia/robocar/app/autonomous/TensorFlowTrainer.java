package com.zugaldia.robocar.app.autonomous;

import android.content.Context;
import android.media.ImageReader;

import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;
import com.zugaldia.robocar.software.camera.CameraOperator;
import com.zugaldia.robocar.software.camera.CameraOperatorListener;
import com.zugaldia.robocar.software.camera.ImageSaver;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import timber.log.Timber;

/**
 * Captures photos that can be used to train a TensorFlow model.
 */
public class TensorFlowTrainer implements
    CameraOperatorListener, ImageReader.OnImageAvailableListener {

  private AdafruitMotorHat motorHat;
  private CameraOperator cameraOperator;

  private String sessionId;
  private int sessionCount;
  private Timer timer;

  public TensorFlowTrainer(Context context, AdafruitMotorHat motorHat) {
    this.motorHat = motorHat;
    cameraOperator = new CameraOperator(context, this);
  }

  public int[] getSpeeds() {
    return new int[] {
        motorHat.getMotor(1).getLastSpeed(),
        motorHat.getMotor(2).getLastSpeed(),
        motorHat.getMotor(3).getLastSpeed(),
        motorHat.getMotor(4).getLastSpeed()};
  }

  public void startSession() {
    if (!cameraOperator.isInSession()) {
      sessionId = UUID.randomUUID().toString().replace("-", "");
      sessionCount = 0;
      cameraOperator.startSession(this);
    }
  }

  @Override
  public void sessionStarted() {
    timer = new Timer("CAMERA_TRAINING");
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        cameraOperator.takePicture();
      }
    }, 0, 250); // 0 delay, 250 period
  }

  public void endSession() {
    if (timer != null) {
      timer.cancel();
    }
    cameraOperator.endSession();
  }

  @Override
  public void onImageAvailable(final ImageReader reader) {
    Timber.d("Image available.");
    new Thread(new Runnable() {
      @Override
      public void run() {
        File root = ImageSaver.getRoot(CameraOperator.ROBOCAR_FOLDER);
        ImageSaver imageSaver = new ImageSaver(reader.acquireLatestImage(), root, getFilename());
        imageSaver.run();
      }
    }).start();
  }

  private String getFilename() {
    String timestamp = CameraOperator.DATE_FORMAT.format(new Date());
    int[] speeds = getSpeeds();
    String speedState = String.format(Locale.US, "%d-%d-%d-%d",
        speeds[0], speeds[1], speeds[2], speeds[3]);
    return String.format(Locale.US, "robocar-%s-%d-%s-%s.jpg",
        sessionId, sessionCount++, timestamp, speedState);
  }
}
