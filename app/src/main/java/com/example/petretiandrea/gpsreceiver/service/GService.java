package com.example.petretiandrea.gpsreceiver.service;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.petretiandrea.gpsreceiver.R;
import com.example.petretiandrea.gpsreceiver.bluetooth.BTManager;
import com.example.petretiandrea.gpsreceiver.gps.GPSManager;
import com.example.petretiandrea.gpsreceiver.util.Constants;
import com.example.petretiandrea.gpsreceiver.util.NotificationHelper;

public class GService extends Service {

    private static final String TAG = GService.class.getName();

    private IBinder mBinder = new LocalBinder();

    private boolean mRunning = false;
    private Notification mNotification;
    private BTManager mBTManager;
    private GPSManager mGPSManager;

    private GServiceCallback mGServiceCallback;


    public BTManager getBTManager() {
        return mBTManager;
    }

    public GPSManager getGPSManager() {
        return mGPSManager;
    }

    public synchronized void setGServiceCallback(GServiceCallback serviceCallback) {
        mGServiceCallback = serviceCallback;
    }

    private synchronized GServiceCallback getGServiceCallback() {
        return mGServiceCallback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotification = new NotificationHelper(this)
                .getNotificationOne(getString(R.string.app_name), "In esecuzione...")
                .setSmallIcon(R.drawable.ic_my_location_black_24dp)
                .build();

        mRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        String action = intent.getAction();
        synchronized (this) {
            if(action != null && action.equals(Constants.ACTION_START_BT_SERVICE)) {
                if (!mRunning) {
                    Log.d(TAG, "Starting GPS Service...");
                    start();
                }
            } else if(action != null && action.equals(Constants.ACTION_STOP_BT_SERVICE)) {
                Log.d(TAG, "Stop GPS Service...");
                stop();
            }
        }
        return Service.START_NOT_STICKY;
    }

    public synchronized void start() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if(mBTManager == null)
                mBTManager = new BTManager(mHandler);
            if(mGPSManager == null)
                mGPSManager = GPSManager.newInstance(mHandler);
            if(mBTManager.getState() == BTManager.STATE_NONE) {
                mRunning = true;
                mBTManager.start();
            }
            startForeground(Constants.FOREGROUND_SERVICE_ID, mNotification);
        } else if(bluetoothAdapter != null) { // request enable bt.
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        }
    }

    public synchronized void stop() {
        mRunning = false;
        if(mBTManager != null) {
            mBTManager.stop();
            mBTManager = null;
        }
        if(mGPSManager != null) {
            mGPSManager.stop();
            mGPSManager = null;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stop();
    }


    public class LocalBinder extends Binder {
        public GService getService() {
            return GService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case BTManager.STATE_CHANGED: {
                    switch (message.arg1) {
                        case BTManager.STATE_NONE:
                            Log.d(TAG, "State NONE, stop gps");
                            if (mGPSManager != null) mGPSManager.stop();
                            break;
                        case BTManager.STATE_LISTEN:
                            Log.d(TAG, "STATE LISTEN: stop gps,");
                            if (mGPSManager != null) mGPSManager.stop();
                            break;
                        case BTManager.STATE_CONNECTED:
                            Log.d(TAG, "STATE CONNECTED: start gps");
                            if (mGPSManager != null) mGPSManager.start(GService.this, mBTManager);
                            break;
                        default:
                            break;
                    }
                    String deviceName =  (message.arg1 == BTManager.STATE_CONNECTED && message.obj != null) ?
                            ((BTManager)message.obj).getDeviceConnected().getName() : null;
                    if(getGServiceCallback() != null) getGServiceCallback().onBTStateChange(message.arg1, deviceName);
                    break;
                }
                case GPSManager.LOCATION_UPDATE: {
                    if(getGServiceCallback() != null) getGServiceCallback().onLocationUpdate((Location) message.obj);
                    break;
                }
            }
            return true;
        }
    });
}
