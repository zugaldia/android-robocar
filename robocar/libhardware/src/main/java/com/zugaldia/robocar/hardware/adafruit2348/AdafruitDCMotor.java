package com.zugaldia.robocar.hardware.adafruit2348;

/**
 * A port of `Adafruit_DCMotor` to Android Things.
 * <p>
 * https://github.com/adafruit/Adafruit-Motor-HAT-Python-Library/blob/master/Adafruit_MotorHAT/Adafruit_MotorHAT.py
 */

public class AdafruitDCMotor {

  private AdafruitMotorHat MC;
  private int motornum;
  int PWMpin;
  int IN1pin;
  int IN2pin;

  public AdafruitDCMotor(AdafruitMotorHat MC, int num) {
    this.MC = MC;
    this.motornum = num;
    if (num == 0) {
      PWMpin = 8;
      IN2pin = 9;
      IN1pin = 10;
    } else if (num == 1) {
      PWMpin = 13;
      IN2pin = 12;
      IN1pin = 11;
    } else if (num == 2) {
      PWMpin = 2;
      IN2pin = 3;
      IN1pin = 4;
    } else if (num == 3) {
      PWMpin = 7;
      IN2pin = 6;
      IN1pin = 5;
    } else {
      throw new RuntimeException("Motor number must be between 1 and 4, inclusive.");
    }
  }

  public void run(int command) {
    if (MC == null) {
      return;
    }

    if (command == AdafruitMotorHat.FORWARD) {
      MC.setPin(IN2pin, 0);
      MC.setPin(IN1pin, 1);
    } else if (command == AdafruitMotorHat.BACKWARD) {
      MC.setPin(IN1pin, 0);
      MC.setPin(IN2pin, 1);
    } else if (command == AdafruitMotorHat.RELEASE) {
      MC.setPin(IN1pin, 0);
      MC.setPin(IN2pin, 0);
    }
  }

  public void setSpeed(int speed) {
    if (speed < 0) {
      speed = 0;
    }
    if (speed > 255) {
      speed = 255;
    }

    MC.getPwm().setPWM(PWMpin, 0, speed * 16);
  }
}
