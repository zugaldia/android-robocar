package com.zugaldia.robocar.app;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.zugaldia.robocar.app.autonomous.CameraDriver;
import com.zugaldia.robocar.app.autonomous.TensorFlowTrainer;
import com.zugaldia.robocar.app.manual.RCDriver;
import com.zugaldia.robocar.app.manual.LocalhostDriver;
import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;
import com.zugaldia.robocar.software.controller.nes30.Nes30Connection;
import com.zugaldia.robocar.software.controller.nes30.Nes30Listener;
import com.zugaldia.robocar.software.controller.nes30.Nes30Manager;
import com.zugaldia.robocar.software.webserver.LocalWebServer;
import com.zugaldia.robocar.software.webserver.HTTPRequestListener;
import com.zugaldia.robocar.software.webserver.models.RobocarMove;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;
import com.zugaldia.robocar.software.webserver.models.RobocarStatus;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements Nes30Listener, HTTPRequestListener {

  private Nes30Manager nes30Manager;
  private Nes30Connection nes30Connection;

  private AdafruitMotorHat motorHat;

  private RCDriver rcDriver;
  private LocalhostDriver localhostDriver;

  private CameraDriver cameraDriver;
  private TensorFlowTrainer tensorFlowTrainer;

  // I2C Name
  public static final String I2C_DEVICE_NAME = "I2C1";
  // Adafruit Motor Hat
  private static final int MOTOR_HAT_I2C_ADDRESS = 0x60;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Motors
    motorHat = new AdafruitMotorHat(I2C_DEVICE_NAME, MOTOR_HAT_I2C_ADDRESS, false);

    // Remote control (for RCDriver)
    setupBluetooth();

    // Local web server (for LocalhostDriver)
    setupWebServer();

    // Manual drivers (always available)
    rcDriver = new RCDriver(motorHat);
    localhostDriver = new LocalhostDriver(motorHat);
  }


  private void setupWebServer() {
    LocalWebServer localWebServer = new LocalWebServer(this);
    try {
      localWebServer.start();
    } catch (IOException e) {
      Timber.e(e, "Failed to start local web server.");
    }
  }

  private void setupBluetooth() {
    nes30Manager = new Nes30Manager(this);
    nes30Connection = new Nes30Connection(this, RobocarConstants.NES30_MAC_ADDRESS);
    Timber.d("BT status: %b", nes30Connection.isEnabled());
    Timber.d("Paired devices: %d", nes30Connection.getPairedDevices().size());

    BluetoothDevice nes30device = nes30Connection.getSelectedDevice();
    if (nes30device == null) {
      Timber.d("Starting discovery: %b", nes30Connection.startDiscovery());
    } else {
      Timber.d("Creating bond: %b", nes30Connection.createBond(nes30device));
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    rcDriver.release();
    motorHat.close();
    nes30Connection.cancelDiscovery();
  }

  /*
   * Handle keyboard (controller) events
   */

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return nes30Manager.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    return nes30Manager.onKeyLongPress(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return nes30Manager.onKeyUp(keyCode, event);
  }

  @Override
  public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
    return nes30Manager.onKeyMultiple(keyCode, count, event);
  }

  /*
   * Implements Nes30Listener
   */

  @Override
  public void onKeyPress(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
    switch (keyCode) {
      case Nes30Manager.BUTTON_UP_CODE:
        rcDriver.moveForward(keyCode, isDown);
        break;
      case Nes30Manager.BUTTON_DOWN_CODE:
        rcDriver.moveBackward(keyCode, isDown);
        break;
      case Nes30Manager.BUTTON_LEFT_CODE:
        rcDriver.turnLeft(keyCode, isDown);
        break;
      case Nes30Manager.BUTTON_RIGHT_CODE:
        rcDriver.turnRight(keyCode, isDown);
        break;
      case Nes30Manager.BUTTON_X_CODE:
        if (isDown) {
          Timber.d("Starting camera session for single pics.");
        }
        break;
      case Nes30Manager.BUTTON_Y_CODE:
        if (isDown) {
          Timber.d("Starting camera session for multiple pics.");
          if (tensorFlowTrainer == null) {
            tensorFlowTrainer = new TensorFlowTrainer(this, motorHat);
          }
          tensorFlowTrainer.startSession();
        }
        break;
      case Nes30Manager.BUTTON_A_CODE:
        if (isDown) {
          Timber.d("Stopping camera session.");
          tensorFlowTrainer.endSession();
        }
        break;
      case Nes30Manager.BUTTON_B_CODE:
        Timber.d("Button B pressed.");
        break;
      case Nes30Manager.BUTTON_L_CODE:
        if (cameraDriver == null) {
          cameraDriver = new CameraDriver(this, motorHat);
        }
        cameraDriver.start();
        break;
      case Nes30Manager.BUTTON_R_CODE:
        if (cameraDriver != null) {
          cameraDriver.stop();
        }
        break;
      case Nes30Manager.BUTTON_SELECT_CODE:
        Timber.d("Select button pressed.");
        break;
      case Nes30Manager.BUTTON_START_CODE:
        Timber.d("Start button pressed.");
        break;
      case Nes30Manager.BUTTON_KONAMI:
        // Do your magic here ;-)
        break;
    }
  }

  /*
   * Implement RequestListener (web server)
   */

  @Override
  public void onRequest(NanoHTTPD.IHTTPSession session) {
    LocalWebServer.logSession(session);
  }

  @Override
  public RobocarStatus onStatus() {
    return new RobocarStatus(200, "OK");
  }

  @Override
  public RobocarResponse onMove(RobocarMove move) {
    return new RobocarResponse(200, "TODO");
  }

  @Override
  public RobocarResponse onSpeed(RobocarSpeed speed) {
    if (speed == null) {
      return new RobocarResponse(400, "Bad Request");
    }

    localhostDriver.changeSpeed(speed);
    return new RobocarResponse(200, "OK");
  }
}
