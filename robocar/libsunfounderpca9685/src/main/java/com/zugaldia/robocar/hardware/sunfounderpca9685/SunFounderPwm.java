package com.zugaldia.robocar.hardware.sunfounderpca9685;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

import timber.log.Timber;

/**
 * A PWM control class for PCA9685. This is a port of
 * https://github.com/sunfounder/SunFounder_PCA9685/blob/master/PCA9685.py
 * to Android Things.
 */
public class SunFounderPwm {

  // RPI_REVISION_3 = ["a02082", "a22082"]

  public static final String I2C_DEVICE_NAME = "I2C1"; // Bus 1
  public static final int I2C_ADDRESS = 0x40;

  // Registers
  private static final int _MODE1 = 0x00;
  private static final int _MODE2 = 0x01;
  private static final int _SUBADR1 = 0x02;
  private static final int _SUBADR2 = 0x03;
  private static final int _SUBADR3 = 0x04;
  private static final int _PRESCALE = 0xFE;
  private static final int _LED0_ON_L = 0x06;
  private static final int _LED0_ON_H = 0x07;
  private static final int _LED0_OFF_L = 0x08;
  private static final int _LED0_OFF_H = 0x09;
  private static final int _ALL_LED_ON_L = 0xFA;
  private static final int _ALL_LED_ON_H = 0xFB;
  private static final int _ALL_LED_OFF_L = 0xFC;
  private static final int _ALL_LED_OFF_H = 0xFD;

  // Bits
  private static final int _RESTART = 0x80;
  private static final int _SLEEP = 0x10;
  private static final int _ALLCALL = 0x01;
  private static final int _INVRT = 0x10;
  private static final int _OUTDRV = 0x04;

  private String deviceName;
  private int address;
  private boolean debug;
  private I2cDevice i2c;

  private int frequency;

  public SunFounderPwm() {
    this(I2C_DEVICE_NAME, I2C_ADDRESS, true);
  }

  public SunFounderPwm(String deviceName, int address, boolean debug) {
    this.deviceName = deviceName;
    this.address = address;
    this.debug = debug;

    try {
      // Attempt to access the I2C device
      Timber.d("Connecting to I2C device %s @ 0x%02X.", deviceName, address);
      PeripheralManagerService manager = new PeripheralManagerService();
      i2c = manager.openI2cDevice(deviceName, address);
    } catch (IOException e) {
      Timber.w(e, "Unable to access I2C device.");
    }

    reset();
  }

  public void test() {
    setFrequency(60);
    for (int i = 0; i < 16; i++) {
      sleepWrapped(0.5);
      Timber.d("Channel %d.", i);
      sleepWrapped(0.5);
      for (int j = 0; j < 4096; j++) {
        write(i, 0, j);
        Timber.d("PWM value: %d", j);
        sleepWrapped(0.0003);
      }
    }
  }

  public int getFrequency() {
    return frequency;
  }

  /**
   * Set PWM frequency.
   */
  public void setFrequency(int frequency) {
    if (debug) {
      Timber.d("Set frequency to %d", frequency);
    }

    this.frequency = frequency;

    double prescaleValue = 25000000.0;
    prescaleValue /= 4096.0;
    prescaleValue /= (float) frequency;
    prescaleValue -= 1.0;

    if (debug) {
      Timber.d("Setting PWM frequency to %d Hz.", frequency);
      Timber.d("Estimated pre-scale: %d", prescaleValue);
    }

    double prescale = Math.floor(prescaleValue + 0.5);
    byte oldMode = readRegByteWrapped(_MODE1);
    byte newMode = (byte) ((oldMode & 0x7F) | 0x10);
    writeRegByteWrapped(_MODE1, newMode);
    writeRegByteWrapped(_PRESCALE, (byte) Math.floor(prescale));
    writeRegByteWrapped(_MODE1, oldMode);
    sleepWrapped(0.005);
    writeRegByteWrapped(_MODE1, (byte) (oldMode | 0x80));
  }

  /**
   * Init the class with bus_number and address.
   */
  private void reset() {
    if (debug) {
      Timber.d("Reseting PCA9685 MODE1 (without SLEEP) and MODE2.");
    }

    setAllPwm(0, 0);
    writeRegByteWrapped(_MODE2, (byte) _OUTDRV);
    writeRegByteWrapped(_MODE1, (byte) _ALLCALL);
    sleepWrapped(0.005); // wait for oscillator

    byte mode1 = readRegByteWrapped(_MODE1);
    mode1 = (byte) (mode1 & ~_SLEEP); // wake up (reset sleep)
    writeRegByteWrapped(_MODE1, mode1);
    sleepWrapped(0.005); // wait for oscillator
    setFrequency(60);
  }

  /**
   * To map the value from arange to another.
   */
  public int map(int x, int inMin, int inMax, int outMin, int outMax) {
    return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
  }

  /**
   * Close the device.
   */
  public void close() {
    if (i2c != null) {
      try {
        i2c.close();
        i2c = null;
      } catch (IOException e) {
        Timber.w(e, "Unable to close I2C device.");
      }
    }
  }

  /**
   * Set on and off value on specific channel.
   */
  public void write(int channel, int on, int off) {
    if (debug) {
      Timber.d("Set channel %d to value %d.", channel, off);
    }

    writeRegByteWrapped(_LED0_ON_L + 4 * channel, (byte) (on & 0xFF));
    writeRegByteWrapped(_LED0_ON_H + 4 * channel, (byte) (on >> 8));
    writeRegByteWrapped(_LED0_OFF_L + 4 * channel, (byte) (off & 0xFF));
    writeRegByteWrapped(_LED0_OFF_H + 4 * channel, (byte) (off >> 8));
  }

  /**
   * Set on and off value on all channel.
   */
  private void setAllPwm(int on, int off) {
    if (debug) {
      Timber.d("Set all channel to value %d", off);
    }

    writeRegByteWrapped(_ALL_LED_ON_L, (byte) (on & 0xFF));
    writeRegByteWrapped(_ALL_LED_ON_H, (byte) (on >> 8));
    writeRegByteWrapped(_ALL_LED_OFF_L, (byte) (off & 0xFF));
    writeRegByteWrapped(_ALL_LED_OFF_H, (byte) (off >> 8));
  }

  public static void sleepWrapped(double seconds) {
    try {
      Thread.sleep((long) (seconds * 1000));
    } catch (InterruptedException e) {
      Timber.e(e, "sleepWrapped failed.");
    }
  }

  private void writeRegByteWrapped(int reg, byte data) {
    try {
      i2c.writeRegByte(reg, data);
    } catch (IOException e) {
      Timber.e(e, "writeRegByte to 0x%02X failed.", reg);
      return;
    }

    if (debug) {
      Timber.d("Wrote to register 0x%02X: 0x%02X", reg, data);
    }
  }

  private byte readRegByteWrapped(int reg) {
    byte data = 0;

    try {
      data = i2c.readRegByte(reg);
    } catch (IOException e) {
      Timber.d(e, "readRegByte from 0x%02X failed.", reg);
    }

    if (debug) {
      Timber.d("Read from register 0x%02X: 0x%02X", reg, data);
    }

    return data;
  }
}
