package com.zugaldia.robocar.app.manual;

import com.zugaldia.robocar.hardware.adafruit2348.AdafruitDcMotor;
import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;
import com.zugaldia.robocar.software.controller.nes30.Nes30Manager;

import timber.log.Timber;

/**
 * Remote control driver.
 */
public class RCDriver {

  // Set the speed, from 0 (off) to 255 (max speed)
  private static final int MOTOR_SPEED_SLOW = 95;
  private static final int MOTOR_SPEED = 255;

  private AdafruitDcMotor motorFrontLeft;
  private AdafruitDcMotor motorFrontRight;
  private AdafruitDcMotor motorBackLeft;
  private AdafruitDcMotor motorBackRight;

  private boolean isUpPressed = false;
  private boolean isDownPressed = false;
  private boolean isLeftPressed = false;
  private boolean isRightPressed = false;
  private boolean isUpOrDownPressed = false;
  private boolean allButtonsReleased = true;

  private boolean isMoving = false;

  public RCDriver(AdafruitMotorHat motorHat) {
    motorFrontLeft = motorHat.getMotor(1);
    motorBackLeft = motorHat.getMotor(2);
    motorFrontRight = motorHat.getMotor(3);
    motorBackRight = motorHat.getMotor(4);
  }

  public void updateButtonPressedStates(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
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

  private boolean preCheck(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
    updateButtonPressedStates(keyCode, isDown);
    if (allButtonsReleased && isMoving) {
      // Stop
      release();
      isMoving = false;
    } else {
      // We start moving the moment the key is pressed
      setMotorSpeedsBasedOnButtonsPressed();
      isMoving = true;
    }

    return isMoving;
  }

  public void moveForward(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
    if (!preCheck(keyCode, isDown)) return;

    Timber.d("Moving forward.");
    motorFrontLeft.run(AdafruitMotorHat.FORWARD);
    motorFrontRight.run(AdafruitMotorHat.BACKWARD);
    motorBackLeft.run(AdafruitMotorHat.BACKWARD);
    motorBackRight.run(AdafruitMotorHat.FORWARD);
  }

  public void moveBackward(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
    if (!preCheck(keyCode, isDown)) return;

    Timber.d("Moving backward.");
    motorFrontLeft.run(AdafruitMotorHat.BACKWARD);
    motorFrontRight.run(AdafruitMotorHat.FORWARD);
    motorBackLeft.run(AdafruitMotorHat.FORWARD);
    motorBackRight.run(AdafruitMotorHat.BACKWARD);
  }

  public void turnLeft(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
    if (!preCheck(keyCode, isDown)) return;
    if (isUpOrDownPressed) return;

    Timber.d("Turning left.");
    motorFrontLeft.run(AdafruitMotorHat.BACKWARD);
    motorFrontRight.run(AdafruitMotorHat.BACKWARD);
    motorBackLeft.run(AdafruitMotorHat.FORWARD);
    motorBackRight.run(AdafruitMotorHat.FORWARD);
  }

  public void turnRight(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
    if (!preCheck(keyCode, isDown)) return;
    if (isUpOrDownPressed) return;

    Timber.d("Turning right.");
    motorFrontLeft.run(AdafruitMotorHat.FORWARD);
    motorFrontRight.run(AdafruitMotorHat.FORWARD);
    motorBackLeft.run(AdafruitMotorHat.BACKWARD);
    motorBackRight.run(AdafruitMotorHat.BACKWARD);
  }

  public void release() {
    Timber.d("Release.");
    motorFrontLeft.run(AdafruitMotorHat.RELEASE);
    motorFrontRight.run(AdafruitMotorHat.RELEASE);
    motorBackLeft.run(AdafruitMotorHat.RELEASE);
    motorBackRight.run(AdafruitMotorHat.RELEASE);
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
}
