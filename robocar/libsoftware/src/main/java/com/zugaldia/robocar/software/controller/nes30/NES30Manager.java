package com.zugaldia.robocar.software.controller.nes30;

import android.support.annotation.IntDef;
import android.view.KeyEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import timber.log.Timber;

/**
 * Manage NES30 controller key events
 */

public class NES30Manager implements KeyEvent.Callback {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef( {BUTTON_LEFT_CODE, BUTTON_RIGHT_CODE, BUTTON_UP_CODE, BUTTON_DOWN_CODE,
    BUTTON_SELECT_CODE, BUTTON_START_CODE, BUTTON_A_CODE, BUTTON_B_CODE,
    BUTTON_X_CODE, BUTTON_Y_CODE, BUTTON_L_CODE, BUTTON_R_CODE, BUTTON_KONAMI})
  public @interface ButtonCode {
  }

  public static final int BUTTON_LEFT_CODE = KeyEvent.KEYCODE_DPAD_LEFT;
  public static final int BUTTON_RIGHT_CODE = KeyEvent.KEYCODE_DPAD_RIGHT;
  public static final int BUTTON_UP_CODE = KeyEvent.KEYCODE_DPAD_UP;
  public static final int BUTTON_DOWN_CODE = KeyEvent.KEYCODE_DPAD_DOWN;

  public static final int BUTTON_SELECT_CODE = KeyEvent.KEYCODE_BUTTON_11;
  public static final int BUTTON_START_CODE = KeyEvent.KEYCODE_BUTTON_12;

  public static final int BUTTON_A_CODE = KeyEvent.KEYCODE_BUTTON_1;
  public static final int BUTTON_B_CODE = KeyEvent.KEYCODE_BUTTON_2;
  public static final int BUTTON_X_CODE = KeyEvent.KEYCODE_BUTTON_4;
  public static final int BUTTON_Y_CODE = KeyEvent.KEYCODE_BUTTON_5;

  public static final int BUTTON_L_CODE = KeyEvent.KEYCODE_BUTTON_7;
  public static final int BUTTON_R_CODE = KeyEvent.KEYCODE_BUTTON_8;

  public static final int BUTTON_KONAMI = -1;

  private Deque<Integer> history = null;
  private NES30Listener listener = null;

  private Deque<Integer> konami = new ArrayDeque<>(Arrays.asList(
    BUTTON_UP_CODE, BUTTON_UP_CODE, BUTTON_DOWN_CODE, BUTTON_DOWN_CODE,
    BUTTON_LEFT_CODE, BUTTON_RIGHT_CODE, BUTTON_LEFT_CODE, BUTTON_RIGHT_CODE,
    BUTTON_B_CODE, BUTTON_A_CODE));

  public NES30Manager() {
    history = new ArrayDeque<>();
  }

  public NES30Manager(NES30Listener listener) {
    this();
    this.listener = listener;
  }

  public NES30Listener getListener() {
    return listener;
  }

  public void setListener(NES30Listener listener) {
    this.listener = listener;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    addKeyToHistory(keyCode);
    if (listener != null) {
      listener.onKeyPress(keyCode, true);
      return true;
    }

    return false;
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (listener != null) {
      listener.onKeyPress(keyCode, false);
      return true;
    }

    return false;
  }

  @Override
  public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    // NOTE: Doesn't seem to be triggered
    Timber.d("onKeyLongPress: %d", keyCode);
    return false;
  }

  @Override
  public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
    // NOTE: Doesn't seem to be triggered
    Timber.d("onKeyMultiple: %d", keyCode);
    return false;
  }

  private void addKeyToHistory(int keyCode) {
    history.add(keyCode);
    if (history.size() > 10) {
      history.pop();
    }

    if (Arrays.equals(history.toArray(), konami.toArray())) {
      if (listener != null) {
        listener.onKeyPress(NES30Manager.BUTTON_KONAMI, true);
      }
    }
  }
}
