package com.zugaldia.robocar.software.controller.nes30;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

/**
 * Manages the Bluetooth connection to the NES30 controller.
 */
public class Nes30Connection {

  private Context context;
  private String deviceAddress;

  private BluetoothAdapter bluetoothAdapter;

  /**
   * Public constructor.
   */
  public Nes30Connection(Context context, String deviceAddress) {
    this.context = context;
    this.deviceAddress = deviceAddress;
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) {
      Timber.e("This device does not support Bluetooth.");
    } else if (!isEnabled()) {
      Timber.d("Bluetooth isn't enabled, enabling: %b", bluetoothAdapter.enable());
    }
  }

  public boolean isEnabled() {
    return (bluetoothAdapter != null && bluetoothAdapter.isEnabled());
  }

  public Set<BluetoothDevice> getPairedDevices() {
    return bluetoothAdapter.getBondedDevices();
  }

  /**
   * Checks whether the device is already paired.
   */
  public BluetoothDevice getSelectedDevice() {
    Set<BluetoothDevice> pairedDevices = getPairedDevices();
    for (BluetoothDevice pairedDevice : pairedDevices) {
      if (isSelectedDevice(pairedDevice.getAddress())) {
        return pairedDevice;
      }
    }

    return null;
  }

  public boolean startDiscovery() {
    registerReceiver();
    return bluetoothAdapter.startDiscovery();
  }

  // Create a BroadcastReceiver for ACTION_FOUND.
  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      // Discovery has found a device.
      if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        // Get the BluetoothDevice object and its info from the Intent.
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int bondState = device.getBondState();
        String foundName = device.getName();
        String foundAddress = device.getAddress(); // MAC address
        Timber.d("Discovery has found a device: %d/%s/%s", bondState, foundName, foundAddress);
        if (isSelectedDevice(foundAddress)) {
          createBond(device);
        } else {
          Timber.d("Unknown device, skipping bond attempt.");
        }
      } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
        switch (state) {
          case BluetoothDevice.BOND_NONE:
            Timber.d("The remote device is not bonded.");
            break;
          case BluetoothDevice.BOND_BONDING:
            Timber.d("Bonding is in progress with the remote device.");
            break;
          case BluetoothDevice.BOND_BONDED:
            Timber.d("The remote device is bonded.");
            break;
          default:
            Timber.d("Unknown remote device bonding state.");
            break;
        }
      }
    }
  };

  private void registerReceiver() {
    // Register for broadcasts when a device is discovered.
    IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothDevice.ACTION_FOUND);
    filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    context.registerReceiver(receiver, filter);
  }

  public void cancelDiscovery() {
    bluetoothAdapter.cancelDiscovery();
    context.unregisterReceiver(receiver);
  }

  private boolean isSelectedDevice(String foundAddress) {
    // MAC address is set and recognized
    return !TextUtils.isEmpty(deviceAddress) && deviceAddress.equals(foundAddress);
  }

  /**
   * Pair with the specific device.
   */
  public boolean createBond(BluetoothDevice device) {
    boolean result = device.createBond();
    Timber.d("Creating bond with: %s/%s/%b", device.getName(), device.getAddress(), result);
    return result;
  }

  /**
   * Remove bond with the specific device.
   */
  public void removeBond(BluetoothDevice device) {
    try {
      Timber.w("Removing bond.");
      Method m = device.getClass().getMethod("removeBond", (Class[]) null);
      m.invoke(device, (Object[]) null);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      Timber.e(e, "Failed to remove bond.");
    }
  }
}
