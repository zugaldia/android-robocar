package com.zugaldia.robocar.app;

import android.util.Log;

import com.zugaldia.robocar.hardware.adafruit2348.AdafruitDCMotor;
import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;


/**
 * A port of `DCTest.py` to Android Things. However, instead of an infinite loop,
 * we'll go one by one through the four DC motors.
 * <p>
 * https://github.com/adafruit/Adafruit-Motor-HAT-Python-Library/blob/master/examples/DCTest.py
 */

public class DCTest {

  private static final String LOG_TAG = DCTest.class.getSimpleName();

  private AdafruitMotorHat mh;

  public DCTest() {
    // Create a default object, no changes to I2C address or frequency
    mh = new AdafruitMotorHat();
  }

  public void run(int motorIndex) throws InterruptedException {
    AdafruitDCMotor myMotor = mh.getMotor(motorIndex);

    // Set the speed to start, from 0 (off) to 255 (max speed)
    myMotor.setSpeed(150);
    myMotor.run(AdafruitMotorHat.FORWARD);

    // Turn on motor
    myMotor.run(AdafruitMotorHat.RELEASE);

    Log.d(LOG_TAG, "Forward.");
    myMotor.run(AdafruitMotorHat.FORWARD);

    Log.d(LOG_TAG, "Speed up.");
    for (int i = 0; i < 255; i++) {
      myMotor.setSpeed(i);
      Thread.sleep((long) (0.01 * 1000));
    }

    Log.d(LOG_TAG, "Slow down.");
    for (int i = 254; i >= 0; i--) {
      myMotor.setSpeed(i);
      Thread.sleep((long) (0.01 * 1000));
    }

    Log.d(LOG_TAG, "Backward!");
    myMotor.run(AdafruitMotorHat.BACKWARD);

    Log.d(LOG_TAG, "Speed up.");
    for (int i = 0; i < 255; i++) {
      myMotor.setSpeed(i);
      Thread.sleep((long) (0.01 * 1000));
    }

    Log.d(LOG_TAG, "Slow down.");
    for (int i = 254; i >= 0; i--) {
      myMotor.setSpeed(i);
      Thread.sleep((long) (0.01 * 1000));
    }

    Log.d(LOG_TAG, "Release.");
    myMotor.run(AdafruitMotorHat.RELEASE);
    Thread.sleep((long) (1.0 * 1000));
  }

  /**
   * Recommended for auto-disabling motors on shutdown
   */
  public void close() {
    mh.getMotor(1).run(AdafruitMotorHat.RELEASE);
    mh.getMotor(2).run(AdafruitMotorHat.RELEASE);
    mh.getMotor(3).run(AdafruitMotorHat.RELEASE);
    mh.getMotor(4).run(AdafruitMotorHat.RELEASE);
    mh.close();
  }

}