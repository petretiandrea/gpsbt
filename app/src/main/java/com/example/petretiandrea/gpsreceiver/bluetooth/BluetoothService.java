package com.example.petretiandrea.gpsreceiver.bluetooth;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.petretiandrea.gpsreceiver.R;
import com.example.petretiandrea.gpsreceiver.util.Constants;
import com.example.petretiandrea.gpsreceiver.util.NotificationHelper;

public class BluetoothService extends Service {

    private static final String TAG = BluetoothAdapter.class.getName();

    private IBinder mBinder = new LocalBinder();

    private HandlerThread mHandlerThread;

    private Handler mHandler;

    private BTServer mBTServer;

    private Notification mNotification;

    private BluetoothAdapter mBluetoothAdapter;
    @Override
    public void onCreate() {
        super.onCreate();
        mNotification = new NotificationHelper(this)
                .getNotificationOne(getString(R.string.app_name), "Waiting Connection!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        // if the bluetooth is not enabled, stop the service.
        if(!mBluetoothAdapter.isEnabled()) {
            stopSelf();
        }

        if(mHandlerThread == null) {
            startService();
        }

        return Service.START_NOT_STICKY;
    }

    private void startService() {
        Log.d(TAG, "Starting service...");

        if(mHandlerThread == null) {
            mHandlerThread = new HandlerThread(Constants.BLUETOOTH_SERVER_SERVICE_NAME);
            // New connection server socket handler.
            mBTServer = new BTServer(mBluetoothAdapter);
            // start handler thread looper.
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());

            // execute the method run of Bluetooth Connection Handler
            mHandler.post(mBTServer);
            startForeground(Constants.FOREGROUND_SERVICE_ID, mNotification);
        }
    }

    private void stopService() {
        Log.d(TAG, "Stopping Service");
        if(mBTServer != null)
            mBTServer.cancel();
        if(mHandler != null) {
           mHandler.removeCallbacks(mBTServer);
           mHandler = null;
           mHandlerThread.quit();
        }
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopService();
    }

    public BTServer getBTServer() {
        return mBTServer;
    }

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }
}
