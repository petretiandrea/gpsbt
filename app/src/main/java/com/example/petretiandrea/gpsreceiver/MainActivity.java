package com.example.petretiandrea.gpsreceiver;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.petretiandrea.gpsreceiver.bluetooth.BTClient;
import com.example.petretiandrea.gpsreceiver.bluetooth.BluetoothService;
import com.example.petretiandrea.gpsreceiver.bluetooth.BroadcastBluetooth;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends AppCompatActivity implements OnNmeaMessageListener,
        LocationListener, ServiceConnection {

    private static final int LOCATION_PERMISSION = 2000;
    private static final int REQUEST_ENABLE_BT = 2001;

    private static final String TAG = MainActivity.class.getName();
    private LocationManager mLocationManger;


    private BluetoothService mBTService;
    private Intent mIntentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationManger = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mIntentService = new Intent(this, BluetoothService.class);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this,"This device not support Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(mIntentService, this, BIND_AUTO_CREATE);
        Log.d(TAG, "onStart() called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(this);
        Log.d(TAG, "onStop() called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManger.removeNmeaListener(this);
        mLocationManger.removeUpdates(this);
        stopService(new Intent(this, BluetoothService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickGpsStart(View view) {
        // start bt
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }

        startRequestUpdateLocation();
        /*try {
            mTCPServer = new TCPServer();
            mTCPServer.start();

        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK) {
                BroadcastBluetooth broadcastBluetooth = new BroadcastBluetooth(new BroadcastBluetooth.BluetoothChangeListener() {
                    @Override
                    public void onBluetoothStatusChange(int status) {

                    }
                });
            }
        }
    }

    /**
     * Start requiring location update.
     */
    private void startRequestUpdateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    LOCATION_PERMISSION);
            return;
        }

        startService(new Intent(this, BluetoothService.class));
        // retrive provider from settings
        int provider = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_key_location_provider), "1"));
        // provider set to 1 is GPS_PROVIDER ONLY, 0 is NETWORK_PROVIDER ONLY.
        mLocationManger.requestLocationUpdates((provider == 1) ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER,
                3000, 0, this);
        mLocationManger.addNmeaListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case LOCATION_PERMISSION:
                if(grantResults.length > 0) {
                    if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(this, "Ho bisogno dei permessi per poter funzionare :(", Toast.LENGTH_SHORT).show();
                        finish();
                    } else
                        startRequestUpdateLocation();
                }
                break;
        }
    }


    @Override
    public void onNmeaMessage(String s, long l) {
        //System.out.println("Nmea: " + s);
        /*if(mBTService != null)
            System.out.println(mBTService.getBTServer().getBTsConnected().size());
            for (BTClient btClient : mBTService.getBTServer().getBTsConnected())
                btClient.write(s.getBytes());*/
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d(TAG, "onProviderEnabled " + s);
    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "Service Bind!");
        mBTService = ((BluetoothService.LocalBinder)iBinder).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "Service disconnected!");
    }
}
