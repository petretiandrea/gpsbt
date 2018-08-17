package com.example.petretiandrea.gpsreceiver.util;

import java.util.UUID;

public class Constants {
    public static final UUID MAGIC_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String START_BLUETOOTH_SERVER_SERVICE = "START_BLUETOOTH_SERVER_SERVICE";
    public static final String STOP_BLUETOOTH_SERVER_SERVICE = "STOP_BLUETOOTH_SERVER_SERVICE";
    public static final String BLUETOOTH_SERVER_SERVICE_NAME = "HandlerThreadBluetooth";
    public static final int FOREGROUND_SERVICE_ID = 1001;
    public static final String BT_NAME = "BT_GPS";

    public static final String ACTION_START_BT_SERVICE = "start_bt_service";
    public static final String ACTION_STOP_BT_SERVICE = "start_bt_service";
}
