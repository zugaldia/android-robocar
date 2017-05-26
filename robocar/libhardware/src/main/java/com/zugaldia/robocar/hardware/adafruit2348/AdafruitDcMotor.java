package com.zugaldia.robocar.hardware.adafruit2348;

/**
 * A port of `Adafruit_DCMotor` to Android Things.
 *
 * <p>See: https://github.com/adafruit/Adafruit-Motor-HAT-Python-Library/blob/master/Adafruit_MotorHAT/Adafruit_MotorHAT.py
 */
public class AdafruitDcMotor {

  private AdafruitMotorHat mc;
  private int motornum;
  int pwmPin;
  int in1Pin;
  int in2Pin;
  int lastSpeed = -1;

  /**
   * Public constructor.
   */
  public AdafruitDcMotor(AdafruitMotorHat mc, int num) {
    this.mc = mc;
    this.motornum = num;
    if (num == 0) {
      pwmPin = 8;
      in2Pin = 9;
      in1Pin = 10;
    } else if (num == 1) {
      pwmPin = 13;
      in2Pin = 12;
      in1Pin = 11;
    } else if (num == 2) {
      pwmPin = 2;
      in2Pin = 3;
      in1Pin = 4;
    } else if (num == 3) {
      pwmPin = 7;
      in2Pin = 6;
      in1Pin = 5;
    } else {
      throw new RuntimeException("Motor number must be between 1 and 4, inclusive.");
    }
  }

  /**
   * Run the specific command.
   */
  public void run(int command) {
    if (mc == null) {
      return;
    }

    if (command == AdafruitMotorHat.FORWARD) {
      mc.setPin(in2Pin, 0);
      mc.setPin(in1Pin, 1);
    } else if (command == AdafruitMotorHat.BACKWARD) {
      mc.setPin(in1Pin, 0);
      mc.setPin(in2Pin, 1);
    } else if (command == AdafruitMotorHat.RELEASE) {
      mc.setPin(in1Pin, 0);
      mc.setPin(in2Pin, 0);
    }
  }

  /**
   * Run the specific speed.
   */
  public void setSpeed(int speed) {
    if (speed < 0) {
      speed = 0;
    }
    if (speed > 255) {
      speed = 255;
    }

    // Set the speed only if it has changed, otherwise the motor will be jittery.
    if (lastSpeed == -1 || lastSpeed != speed) {
      mc.getPwm().setPwm(pwmPin, 0, speed * 16);
    }

    lastSpeed = speed;
  }

  public int getLastSpeed() {
    return lastSpeed;
  }
}
