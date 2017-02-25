package com.zugaldia.robocar.hardware.adafruit2348;

/**
 * A port of `Adafruit_MotorHAT` to Android Things.
 * <p>
 * https://github.com/adafruit/Adafruit-Motor-HAT-Python-Library/blob/master/Adafruit_MotorHAT/Adafruit_MotorHAT.py
 */

public class AdafruitMotorHat {

  public static final int MOTOR_FREQUENCY = 1600;

  public static final int FORWARD = 1;
  public static final int BACKWARD = 2;
  public static final int BRAKE = 3;
  public static final int RELEASE = 4;

  public static final int SINGLE = 1;
  public static final int DOUBLE = 2;
  public static final int INTERLEAVE = 3;
  public static final int MICROSTEP = 4;

  private AdafruitPwm pwm;
  private AdafruitDCMotor[] motors;

  public AdafruitMotorHat() {
    pwm = new AdafruitPwm();
    pwm.setPWMFreq(MOTOR_FREQUENCY);
    motors = new AdafruitDCMotor[] {
      new AdafruitDCMotor(this, 0),
      new AdafruitDCMotor(this, 1),
      new AdafruitDCMotor(this, 2),
      new AdafruitDCMotor(this, 3)
    };
  }

  public AdafruitPwm getPwm() {
    return pwm;
  }

  public void setPin(int pin, int value) {
    if ((pin < 0) || (pin > 15)) {
      throw new RuntimeException("PWM pin must be between 0 and 15 inclusive");
    }
    if ((value != 0) && (value != 1)) {
      throw new RuntimeException("Pin value must be 0 or 1!");
    }
    if ((value == 0)) {
      pwm.setPWM(pin, 0, 4096);
    }
    if ((value == 1)) {
      pwm.setPWM(pin, 4096, 0);
    }
  }

  public AdafruitDCMotor getMotor(int num) {
    if ((num < 1) || (num > 4)) {
      throw new RuntimeException("MotorHAT Motor must be between 1 and 4 inclusive");
    }
    return motors[num - 1];
  }

  public void close() {
    pwm.close();
  }
}
