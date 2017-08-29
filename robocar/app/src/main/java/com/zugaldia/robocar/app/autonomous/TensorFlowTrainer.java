package com.zugaldia.robocar.app.autonomous;

import android.content.Context;

import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;
import com.zugaldia.robocar.software.camera.CameraOperator;
import com.zugaldia.robocar.software.camera.CameraOperatorListener;
import com.zugaldia.robocar.software.camera.SpeedOwner;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Captures photos that can be used to train a TensorFlow model.
 */
public class TensorFlowTrainer implements SpeedOwner, CameraOperatorListener {

  private AdafruitMotorHat motorHat;

  private CameraOperator cameraOperator;
  private Timer timer;

  public TensorFlowTrainer(Context context, AdafruitMotorHat motorHat) {
    this.motorHat = motorHat;
    cameraOperator = new CameraOperator(context, this, this);
  }

  @Override
  public int[] getSpeeds() {
    return new int[] {
        motorHat.getMotor(1).getLastSpeed(),
        motorHat.getMotor(2).getLastSpeed(),
        motorHat.getMotor(3).getLastSpeed(),
        motorHat.getMotor(4).getLastSpeed()};
  }

  public void startSession() {
    if (!cameraOperator.isInSession()) {
      cameraOperator.startSession();
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
}
