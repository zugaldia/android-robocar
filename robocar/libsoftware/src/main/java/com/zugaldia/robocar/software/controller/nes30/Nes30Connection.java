package com.zugaldia.robocar.software.controller.nes30;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import java.util.Set;

import timber.log.Timber;

/**
 * Manages the Bluetooth connection to the NES30 controller.
 */
public class Nes30Connection {

  private Context context;
  private String deviceAddress;
  private String deviceName;

  private BluetoothAdapter bluetoothAdapter;

  /**
   * Public constructor.
   */
  public Nes30Connection(Context context, String deviceName, String deviceAddress) {
    this.context = context;
    this.deviceName = deviceName;
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
  public BluetoothDevice isPaired() {
    Set<BluetoothDevice> pairedDevices = getPairedDevices();
    for (BluetoothDevice pairedDevice : pairedDevices) {
      if (isKnownDevice(pairedDevice.getName(), pairedDevice.getAddress())) {
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
        if (isKnownDevice(foundName, foundAddress)) {
          createBond(device);
        } else {
          Timber.d("Unknown device, skipping bond attempt.");
        }
      }
    }
  };

  private void registerReceiver() {
    // Register for broadcasts when a device is discovered.
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    context.registerReceiver(receiver, filter);
  }

  public void cancelDiscovery() {
    bluetoothAdapter.cancelDiscovery();
    context.unregisterReceiver(receiver);
  }

  private boolean isKnownDevice(String foundName, String foundAddress) {
    if (!TextUtils.isEmpty(deviceName) && deviceName.equals(foundName)) {
      // Name is set and recognized
      return true;
    } else if (!TextUtils.isEmpty(deviceAddress) && deviceAddress.equals(foundAddress)) {
      // MAC address is set and recognized
      return true;
    }

    return false;
  }

  /**
   * Pair with the specific device.
   */
  public boolean createBond(BluetoothDevice device) {
    boolean result = device.createBond();
    Timber.d("Creating bond with: %s/%s/%b", device.getName(), device.getAddress(), result);
    return result;
  }

}
