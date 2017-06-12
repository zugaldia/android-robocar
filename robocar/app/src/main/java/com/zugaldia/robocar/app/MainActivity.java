package com.zugaldia.robocar.app;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.zugaldia.robocar.hardware.adafruit2348.AdafruitDcMotor;
import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;
import com.zugaldia.robocar.software.camera.CameraOperator;
import com.zugaldia.robocar.software.camera.CameraOperatorListener;
import com.zugaldia.robocar.software.camera.SpeedOwner;
import com.zugaldia.robocar.software.controller.nes30.Nes30Connection;
import com.zugaldia.robocar.software.controller.nes30.Nes30Listener;
import com.zugaldia.robocar.software.controller.nes30.Nes30Manager;
import com.zugaldia.robocar.software.webserver.LocalWebServer;
import com.zugaldia.robocar.software.webserver.RequestListener;
import com.zugaldia.robocar.software.webserver.models.RobocarMove;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;
import com.zugaldia.robocar.software.webserver.models.RobocarStatus;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity
    implements Nes30Listener, RequestListener, SpeedOwner, CameraOperatorListener {

  // Set the speed, from 0 (off) to 255 (max speed)
  private static final int MOTOR_SPEED = 255;
  private static final int MOTOR_SPEED_SLOW = 95;

  private Nes30Manager nes30Manager;
  private Nes30Connection nes30Connection;
  private boolean isMoving = false;

  private AdafruitMotorHat motorHat;
  private AdafruitDcMotor motorFrontLeft;
  private AdafruitDcMotor motorFrontRight;
  private AdafruitDcMotor motorBackLeft;
  private AdafruitDcMotor motorBackRight;

  //TODO: refactoring: make these into a class, maybe call it ButtonPressStates
  private boolean isUpPressed = false;
  private boolean isDownPressed = false;
  private boolean isLeftPressed = false;
  private boolean isRightPressed = false;
  private boolean isUpOrDownPressed = false;
  private boolean allButtonsReleased = true;

  private CameraOperator cameraOperator;
  private Timer timer;
  private int cameraMode;
  private static final int CAMERA_MODE_ONE = 1;
  private static final int CAMERA_MODE_MULTIPLE = 2;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Controller
    nes30Manager = new Nes30Manager(this);

    // Motors
    motorHat = new AdafruitMotorHat();
    motorFrontLeft = motorHat.getMotor(1);
    motorBackLeft = motorHat.getMotor(2);
    motorFrontRight = motorHat.getMotor(3);
    motorBackRight = motorHat.getMotor(4);

    // Local web server
    setupWebServer();

    // NES30 BT connection
    setupBluetooth();

    // Camera
    cameraOperator = new CameraOperator(this, this, this);
  }

  @Override
  public int[] getSpeeds() {
    return new int[] {
        motorHat.getMotor(1).getLastSpeed(),
        motorHat.getMotor(2).getLastSpeed(),
        motorHat.getMotor(3).getLastSpeed(),
        motorHat.getMotor(4).getLastSpeed()};
  }

  private void setMotorSpeedsBasedOnButtonsPressed() {

    boolean isLowSpeedOnLeft = isLeftPressed && isUpOrDownPressed;
    boolean isLowSpeedOnRight = isRightPressed && isUpOrDownPressed;

    int speedLeft = isLowSpeedOnLeft ? MOTOR_SPEED_SLOW : MOTOR_SPEED;
    int speedRight = isLowSpeedOnRight ? MOTOR_SPEED_SLOW : MOTOR_SPEED;

    motorFrontLeft.setSpeed(speedLeft);
    motorBackLeft.setSpeed(speedLeft);
    motorFrontRight.setSpeed(speedRight);
    motorBackRight.setSpeed(speedRight);
  }

  private void setupWebServer() {
    LocalWebServer localWebServer = new LocalWebServer(this);
    try {
      localWebServer.start();
    } catch (IOException e) {
      Timber.e(e, "Failed to start local web server.");
    }
  }

  private void setupBluetooth() {
    nes30Connection = new Nes30Connection(
        this, RobocarConstants.NES30_NAME, RobocarConstants.NES30_MAC_ADDRESS);

    Timber.d("BT status: %b", nes30Connection.isEnabled());
    Timber.d("Paired devices: %d", nes30Connection.getPairedDevices().size());
    BluetoothDevice nes30device = nes30Connection.isPaired();
    if (nes30device == null) {
      Timber.d("Starting discovery: %b", nes30Connection.startDiscovery());
    } else {
      Timber.d("Creating bond: %b", nes30Connection.createBond(nes30device));
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    release();
    motorHat.close();
    nes30Connection.cancelDiscovery();
    endSession();
  }

  /*
   * Handle keyboard (controller) events
   */

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return nes30Manager.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    return nes30Manager.onKeyLongPress(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return nes30Manager.onKeyUp(keyCode, event);
  }

  @Override
  public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
    return nes30Manager.onKeyMultiple(keyCode, count, event);
  }

  @Override
  public void onKeyPress(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
    updateButtonPressedStates(keyCode, isDown);

    if (allButtonsReleased && isMoving) {
      isMoving = false;
      release();
      return;
    }
    // We start moving the moment the key is pressed
    isMoving = true;

    setMotorSpeedsBasedOnButtonsPressed();

    switch (keyCode) {
      case Nes30Manager.BUTTON_UP_CODE:
        moveForward();
        break;
      case Nes30Manager.BUTTON_DOWN_CODE:
        moveBackward();
        break;
      case Nes30Manager.BUTTON_LEFT_CODE:
        if (!isUpOrDownPressed) {
          turnLeft();
        }
        break;
      case Nes30Manager.BUTTON_RIGHT_CODE:
        if (!isUpOrDownPressed) {
          turnRight();
        }
        break;
      case Nes30Manager.BUTTON_X_CODE:
        if (isDown) {
          Timber.d("Starting camera session for single pics.");
          cameraMode = CAMERA_MODE_ONE;
          startSession();
        }
        break;
      case Nes30Manager.BUTTON_Y_CODE:
        if (isDown) {
          Timber.d("Starting camera session for multiple pics.");
          cameraMode = CAMERA_MODE_MULTIPLE;
          startSession();
        }
        break;
      case Nes30Manager.BUTTON_A_CODE:
        if (isDown) {
          Timber.d("Stopping camera session.");
          endSession();
        }
        break;
      case Nes30Manager.BUTTON_KONAMI:
        // Do your magic here ;-)
        break;
      default:
        // No action
        break;
    }
  }

  private void updateButtonPressedStates(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
    switch (keyCode) {
      case Nes30Manager.BUTTON_UP_CODE:
        isUpPressed = isDown;
        break;
      case Nes30Manager.BUTTON_DOWN_CODE:
        isDownPressed = isDown;
        break;
      case Nes30Manager.BUTTON_LEFT_CODE:
        isLeftPressed = isDown;
        break;
      case Nes30Manager.BUTTON_RIGHT_CODE:
        isRightPressed = isDown;
        break;
      default:
        // No action
        break;
    }

    isUpOrDownPressed = isUpPressed || isDownPressed;

    allButtonsReleased = !(isUpPressed || isDownPressed || isLeftPressed || isRightPressed);
  }

  private void moveForward() {
    Timber.d("Moving forward.");
    motorFrontLeft.run(AdafruitMotorHat.FORWARD);
    motorFrontRight.run(AdafruitMotorHat.BACKWARD);
    motorBackLeft.run(AdafruitMotorHat.BACKWARD);
    motorBackRight.run(AdafruitMotorHat.FORWARD);
  }

  private void moveBackward() {
    Timber.d("Moving backward.");
    motorFrontLeft.run(AdafruitMotorHat.BACKWARD);
    motorFrontRight.run(AdafruitMotorHat.FORWARD);
    motorBackLeft.run(AdafruitMotorHat.FORWARD);
    motorBackRight.run(AdafruitMotorHat.BACKWARD);
  }

  private void turnLeft() {
    Timber.d("Turning left.");
    motorFrontLeft.run(AdafruitMotorHat.BACKWARD);
    motorFrontRight.run(AdafruitMotorHat.BACKWARD);
    motorBackLeft.run(AdafruitMotorHat.FORWARD);
    motorBackRight.run(AdafruitMotorHat.FORWARD);
  }

  private void turnRight() {
    Timber.d("Turning right.");
    motorFrontLeft.run(AdafruitMotorHat.FORWARD);
    motorFrontRight.run(AdafruitMotorHat.FORWARD);
    motorBackLeft.run(AdafruitMotorHat.BACKWARD);
    motorBackRight.run(AdafruitMotorHat.BACKWARD);
  }

  private void release() {
    Timber.d("Release.");
    motorFrontLeft.run(AdafruitMotorHat.RELEASE);
    motorFrontRight.run(AdafruitMotorHat.RELEASE);
    motorBackLeft.run(AdafruitMotorHat.RELEASE);
    motorBackRight.run(AdafruitMotorHat.RELEASE);
  }

  @Override
  public void onRequest(NanoHTTPD.IHTTPSession session) {
    LocalWebServer.logSession(session);
  }

  @Override
  public RobocarStatus onStatus() {
    return new RobocarStatus(200, "OK");
  }

  @Override
  public RobocarResponse onMove(RobocarMove move) {
    return new RobocarResponse(200, "TODO");
  }

  @Override
  public RobocarResponse onSpeed(RobocarSpeed speed) {
    if (speed == null) {
      return new RobocarResponse(400, "Bad Request");
    }
    RobocarSpeedChanger speedChanger = new RobocarSpeedChanger(
        motorFrontLeft, motorFrontRight, motorBackLeft, motorBackRight);
    speedChanger.changeSpeed(speed);
    return new RobocarResponse(200, "OK");
  }

  private void startSession() {
    if (!cameraOperator.isInSession()) {
      cameraOperator.startSession();
    } else if (cameraMode == CAMERA_MODE_ONE) {
      sessionStarted();
    }
  }

  @Override
  public void sessionStarted() {
    switch (cameraMode) {
      case CAMERA_MODE_ONE:
        cameraOperator.takePicture();
        break;
      case CAMERA_MODE_MULTIPLE:
        timer = new Timer("CAMERA_TRAINING");
        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            cameraOperator.takePicture();
          }
        }, 0, 500); // 0 delay, 500 period
        break;
    }
  }

  private void endSession() {
    if (timer != null) {
      timer.cancel();
    }
    cameraOperator.endSession();
  }
}
