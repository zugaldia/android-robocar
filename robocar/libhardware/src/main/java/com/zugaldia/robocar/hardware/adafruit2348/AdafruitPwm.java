package com.zugaldia.robocar.hardware.adafruit2348;

import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * A port of `Adafruit_PWM_Servo_Driver` (Adafruit PCA9685 16-Channel PWM Servo
 * Driver) to Android Things. Instead of using `Adafruit_I2C` we're using the
 * `I2cDevice` class shipped with Android Things.
 * <p>
 * https://github.com/adafruit/Adafruit-Motor-HAT-Python-Library/blob/master/Adafruit_MotorHAT/Adafruit_PWM_Servo_Driver.py
 * https://developer.android.com/things/sdk/pio/i2c.html
 */

public class AdafruitPwm {

  private static final String LOG_TAG = AdafruitPwm.class.getSimpleName();

  public static final String I2C_DEVICE_NAME = "I2C1";
  public static final int I2C_ADDRESS = 0x60;

  // Registers
  private static final int __MODE1 = 0x00;
  private static final int __MODE2 = 0x01;
  private static final int __SUBADR1 = 0x02;
  private static final int __SUBADR2 = 0x03;
  private static final int __SUBADR3 = 0x04;
  private static final int __PRESCALE = 0xFE;
  private static final int __LED0_ON_L = 0x06;
  private static final int __LED0_ON_H = 0x07;
  private static final int __LED0_OFF_L = 0x08;
  private static final int __LED0_OFF_H = 0x09;
  private static final int __ALL_LED_ON_L = 0xFA;
  private static final int __ALL_LED_ON_H = 0xFB;
  private static final int __ALL_LED_OFF_L = 0xFC;
  private static final int __ALL_LED_OFF_H = 0xFD;

  // Bits
  private static final int __RESTART = 0x80;
  private static final int __SLEEP = 0x10;
  private static final int __ALLCALL = 0x01;
  private static final int __INVRT = 0x10;
  private static final int __OUTDRV = 0x04;

  private I2cDevice i2c;
  private boolean debug;

  public AdafruitPwm() {
    this(I2C_DEVICE_NAME, I2C_ADDRESS, false);
  }

  public AdafruitPwm(String deviceName, int address, boolean debug) {
    try {
      // Attempt to access the I2C device
      Log.d(LOG_TAG, String.format("Connecting to I2C device %s @ 0x%02X.", deviceName, address));
      PeripheralManagerService manager = new PeripheralManagerService();
      i2c = manager.openI2cDevice(deviceName, address);
    } catch (IOException e) {
      Log.w(LOG_TAG, "Unable to access I2C device:", e);
    }

    this.debug = debug;
    reset();
  }

  private void reset() {
    if (debug) {
      Log.d(LOG_TAG, "Resetting PCA9685 MODE1 (without SLEEP) and MODE2.");
    }

    setAllPWM(0, 0);
    writeRegByteWrapped(__MODE2, (byte) __OUTDRV);
    writeRegByteWrapped(__MODE1, (byte) __ALLCALL);
    sleepWrapped(0.005); // wait for oscillator

    byte mode1 = readRegByteWrapped(__MODE1);
    mode1 = (byte) (mode1 & ~__SLEEP); // wake up (reset sleep)
    writeRegByteWrapped(__MODE1, mode1);
    sleepWrapped(0.005); // wait for oscillator
  }

  public void close() {
    if (i2c != null) {
      try {
        i2c.close();
        i2c = null;
      } catch (IOException e) {
        Log.w(LOG_TAG, "Unable to close I2C device:", e);
      }
    }
  }

  /**
   * Sets the PWM frequency
   */
  public void setPWMFreq(int freq) {
    float prescaleval = 25000000.0f; // 25MHz
    prescaleval /= 4096.0; // 12-bit
    prescaleval /= (float) freq;
    prescaleval -= 1.0;
    if (debug) {
      Log.d(LOG_TAG, String.format("Setting PWM frequency to %d Hz", freq));
      Log.d(LOG_TAG, String.format("Estimated pre-scale: %f", prescaleval));
    }

    double prescale = Math.floor(prescaleval + 0.5);
    if (debug) {
      Log.d(LOG_TAG, String.format("Final pre-scale: %f", prescale));
    }

    byte oldmode = readRegByteWrapped(__MODE1);
    byte newmode = (byte) ((oldmode & 0x7F) | 0x10); // sleep
    writeRegByteWrapped(__MODE1, newmode); // go to sleep
    writeRegByteWrapped(__PRESCALE, (byte) Math.floor(prescale));
    writeRegByteWrapped(__MODE1, oldmode);
    sleepWrapped(0.005);
    writeRegByteWrapped(__MODE1, (byte) (oldmode | 0x80));
  }

  /**
   * Sets a single PWM channel
   */
  public void setPWM(int channel, int on, int off) {
    writeRegByteWrapped(__LED0_ON_L + 4 * channel, (byte) (on & 0xFF));
    writeRegByteWrapped(__LED0_ON_H + 4 * channel, (byte) (on >> 8));
    writeRegByteWrapped(__LED0_OFF_L + 4 * channel, (byte) (off & 0xFF));
    writeRegByteWrapped(__LED0_OFF_H + 4 * channel, (byte) (off >> 8));
  }

  /**
   * Sets a all PWM channels
   */
  private void setAllPWM(int on, int off) {
    writeRegByteWrapped(__ALL_LED_ON_L, (byte) (on & 0xFF));
    writeRegByteWrapped(__ALL_LED_ON_H, (byte) (on >> 8));
    writeRegByteWrapped(__ALL_LED_OFF_L, (byte) (off & 0xFF));
    writeRegByteWrapped(__ALL_LED_OFF_H, (byte) (off >> 8));
  }

  private void sleepWrapped(double seconds) {
    try {
      Thread.sleep((long) (seconds * 1000));
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "sleepWrapped failed: " + e.getMessage());
    }
  }

  private void writeRegByteWrapped(int reg, byte data) {
    try {
      i2c.writeRegByte(reg, data);
    } catch (IOException e) {
      Log.e(LOG_TAG, String.format("writeRegByte to 0x%02X failed: %s", reg, e.getMessage()));
      return;
    }

    if (debug) {
      Log.d(LOG_TAG, String.format("Wrote to register 0x%02X: 0x%02X", reg, data));
    }
  }

  private byte readRegByteWrapped(int reg) {
    byte data = 0;

    try {
      data = i2c.readRegByte(reg);
    } catch (IOException e) {
      Log.e(LOG_TAG, String.format("readRegByte from 0x%02X failed: %s", reg, e.getMessage()));
    }

    if (debug) {
      Log.d(LOG_TAG, String.format("Read from register 0x%02X: 0x%02X", reg, data));
    }

    return data;
  }

}
