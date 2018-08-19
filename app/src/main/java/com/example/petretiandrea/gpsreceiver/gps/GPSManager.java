package com.example.petretiandrea.gpsreceiver.gps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.example.petretiandrea.gpsreceiver.R;
import com.example.petretiandrea.gpsreceiver.bluetooth.BTManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class GPSManager {

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(mHandler != null)
                mHandler.obtainMessage(LOCATION_UPDATE, location).sendToTarget();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private OnNmeaMessageListener mNmeaMessageListener = new OnNmeaMessageListener() {
        @Override
        public void onNmeaMessage(String s, long l) {
            if(mBTManager != null)
                mBTManager.send(s);
        }
    };

    public static final int LOCATION_UPDATE = 2;

    /**
     * Android Location manager
     */
    private LocationManager mLocationManager;
    /**
     * BT manager for manage the bt server and client.
     */
    private BTManager mBTManager;

    /**
     * Handler for communicate with service
     */
    private Handler mHandler;

    /**
     * Create and return a new instance of GPSManager.
     * @return A new instance.
     */
    public static GPSManager newInstance(Handler handler)
    {
        return new GPSManager(handler);
    }

    private GPSManager(Handler handler) {
        mHandler = handler;
    }

    /**
     * Start location update, and send the location to bt, using the bt manager.
     * @param context Context for check permissions.
     * @param btManager BT Manager for send location using Bluetooth.
     * @return True if is started, False otherwise.
     */
    public synchronized boolean start(Context context, BTManager btManager) {
        mBTManager = btManager;
        if (mLocationManager == null)
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager != null) {
            // check permissions.
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            // retrive provider from settings
            int provider = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_key_location_provider), "0"));
            mLocationManager.requestLocationUpdates((provider == 0) ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER,
                    0, 0, mLocationListener);
            mLocationManager.addNmeaListener(mNmeaMessageListener);
            return true;
        }
        return false;
    }

    /**
     * Stop the location updates.
     */
    public synchronized void stop() {
        mBTManager = null;
        if(mLocationManager != null) {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationManager.removeNmeaListener(mNmeaMessageListener);
        }
    }
}
