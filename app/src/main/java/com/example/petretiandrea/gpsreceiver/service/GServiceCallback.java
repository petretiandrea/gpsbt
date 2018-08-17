package com.example.petretiandrea.gpsreceiver.service;

import android.bluetooth.BluetoothDevice;
import android.location.Location;

public interface GServiceCallback {
    void onBTStateChange(int newState, String name);
    void onLocationUpdate(Location location);
}
