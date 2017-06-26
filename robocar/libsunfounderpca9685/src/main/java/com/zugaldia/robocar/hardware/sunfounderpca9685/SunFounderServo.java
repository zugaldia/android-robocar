package com.zugaldia.robocar.hardware.sunfounderpca9685;

import java.util.Locale;

import timber.log.Timber;

/**
 * Servo driver class.
 */
public class SunFounderServo {

  private final static int _MIN_PULSE_WIDTH = 600;
  private final static int _MAX_PULSE_WIDTH = 2400;
  private final static int _DEFAULT_PULSE_WIDTH = 1500;
  private final static int _frequency = 60;

  private boolean debug;

  private int channel;
  private int offset;
  private boolean lock;

  private SunFounderPwm pwm;

  private int frequency;

  public SunFounderServo(int channel, int offset, boolean lock) {
    if (channel < 0 || channel > 16) {
      throw new IllegalArgumentException(String.format(Locale.US,
          "Servo channel (%d) is not in (0, 15).", channel));
    }

    this.debug = true;
    this.channel = channel;
    this.offset = offset;
    this.lock = lock;
    this.pwm = new SunFounderPwm();
    write(90);
  }

  public int getFrequency() {
    return frequency;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
    pwm.setFrequency(frequency);
  }

  /**
   * Calculate 12-bit analog value from giving angle.
   */
  private int angleToAnalog(int angle) {
    int pulseWide = pwm.map(angle, 0, 180, _MIN_PULSE_WIDTH, _MAX_PULSE_WIDTH);
    int analogValue = (int) ((float) (pulseWide) / 1000000 * frequency * 4096);
    if (debug) {
      Timber.d("Angle %d equals Analog_value %d", angle, analogValue);
    }
    return analogValue;
  }

  /**
   * Turn the servo with giving angle.
   */
  private void write(int angle) {
    if (lock) {
      if (angle > 180) {
        angle = 180;
      }
      if (angle < 0) {
        angle = 0;
      }
    } else if (angle < 0 || angle > 180) {
      throw new IllegalArgumentException(String.format(Locale.US,
          "Servo %d turn angle %d is not in (0, 180).", channel, angle));
    }

    int val = angleToAnalog(angle);
    val += offset;
    pwm.write(channel, 0, val);
    if (debug) {
      Timber.d("Turn angle = %d", angle);
    }
  }

  /**
   * Servo driver test on channel 1.
   */
  private static void test() {
    SunFounderServo servo = new SunFounderServo(1, 0, true);
    for (int i = 0; i < 180; i += 5) {
      Timber.d("Angle: %d.", i);
      servo.write(i);
      SunFounderPwm.sleepWrapped(0.1);
    }

    for (int i = 180; i > 0; i -= 5) {
      Timber.d("Angle: %d.", i);
      servo.write(i);
      SunFounderPwm.sleepWrapped(0.1);
    }

    for (int i = 0; i < 91; i += 2) {
      Timber.d("Angle: %d.", i);
      servo.write(i);
      SunFounderPwm.sleepWrapped(0.05);
    }
  }

  private static void install() {
    SunFounderServo[] allServos = new SunFounderServo[16];
    for (int i = 0; i < 16; i++) {
      allServos[i] = new SunFounderServo(i, 0, true);
    }
    for (SunFounderServo servo : allServos) {
      servo.write(90);
    }
  }
}
