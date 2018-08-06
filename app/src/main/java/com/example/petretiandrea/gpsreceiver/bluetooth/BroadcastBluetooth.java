package com.example.petretiandrea.gpsreceiver.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.petretiandrea.gpsreceiver.util.Constants;

public class BroadcastBluetooth extends BroadcastReceiver {

    private static final String TAG = BroadcastBluetooth.class.getName();

    public interface BluetoothChangeListener {
        void onBluetoothStatusChange(int status);
    }

    private BluetoothChangeListener mListener;

    public BroadcastBluetooth(BluetoothChangeListener changeListener) {
        mListener = changeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if(action != null) {
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                mListener.onBluetoothStatusChange(state);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "Bluetooth Turning OFF");
                        context.stopService(new Intent(context, BluetoothService.class));
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth ON");
                        Intent i = new Intent(context, BluetoothService.class);
                        i.putExtra(Constants.START_BLUETOOTH_SERVER_SERVICE, Constants.START_BLUETOOTH_SERVER_SERVICE);
                        context.startService(i);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "Bluetooth Turning ON");
                        break;
                }
            }
        }
    }
}
