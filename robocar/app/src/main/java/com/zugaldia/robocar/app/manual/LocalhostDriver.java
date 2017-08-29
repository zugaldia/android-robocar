package com.zugaldia.robocar.app.manual;

import com.zugaldia.robocar.hardware.adafruit2348.AdafruitDcMotor;
import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;

/**
 * Created by Halim.Salameh on 5/9/2017.
 */
public class RobocarSpeedChanger {

  // If speed was set too low, the motor could burn.
  private static final int MIN_SPEED = 64;

  private AdafruitDcMotor motorFrontLeft;
  private AdafruitDcMotor motorFrontRight;
  private AdafruitDcMotor motorBackLeft;
  private AdafruitDcMotor motorBackRight;

  public RobocarSpeedChanger(AdafruitMotorHat motorHat) {
    motorFrontLeft = motorHat.getMotor(1);
    motorBackLeft = motorHat.getMotor(2);
    motorFrontRight = motorHat.getMotor(3);
    motorBackRight = motorHat.getMotor(4);
  }

  public void changeSpeed(RobocarSpeed speed) {

    if (speed.getLeft() != null) {
      setLeftSpeed(speed.getLeft());
    }

    if (speed.getRight() != null) {
      setRightSpeed(speed.getRight());
    }
  }

  public void setLeftSpeed(int speed) {

    int unsignedSpeed = Math.abs(speed);
    if (unsignedSpeed < MIN_SPEED) {
      unsignedSpeed = 0;
    }

    motorBackLeft.setSpeed(unsignedSpeed);
    motorFrontLeft.setSpeed(unsignedSpeed);

    if (speed > 0) { // Positive speed motor directions
      motorFrontLeft.run(AdafruitMotorHat.FORWARD);
      motorBackLeft.run(AdafruitMotorHat.BACKWARD);
    } else { // Negative speed motor directions
      motorFrontLeft.run(AdafruitMotorHat.BACKWARD);
      motorBackLeft.run(AdafruitMotorHat.FORWARD);
    }
  }

  public void setRightSpeed(int speed) {

    int unsignedSpeed = Math.abs(speed);
    if (unsignedSpeed < MIN_SPEED) {
      unsignedSpeed = 0;
    }

    motorBackRight.setSpeed(unsignedSpeed);
    motorFrontRight.setSpeed(unsignedSpeed);

    if (speed > 0) { // Positive speed motor directions
      motorFrontRight.run(AdafruitMotorHat.BACKWARD);
      motorBackRight.run(AdafruitMotorHat.FORWARD);
    } else { // Negative speed motor directions
      motorFrontRight.run(AdafruitMotorHat.FORWARD);
      motorBackRight.run(AdafruitMotorHat.BACKWARD);
    }

  }
}
