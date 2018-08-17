package com.example.petretiandrea.gpsreceiver;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.petretiandrea.gpsreceiver.bluetooth.BTManager;
import com.example.petretiandrea.gpsreceiver.service.GService;
import com.example.petretiandrea.gpsreceiver.service.GServiceCallback;
import com.example.petretiandrea.gpsreceiver.util.Constants;
import com.example.petretiandrea.gpsreceiver.util.Utils;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ServiceConnection, GServiceCallback {

    private static final int LOCATION_PERMISSION = 2000;
    private static final int REQUEST_ENABLE_BT = 2001;

    private static final String TAG = MainActivity.class.getName();

    private GService mService;
    private Intent mIntentService;

    /** UI elements **/
    private TextView viewTxtLastFix;
    private TextView viewTxtLongitude;
    private TextView viewTxtLatitude;
    private TextView viewTxtAltitude;
    private TextView viewTxtSpeed;
    private TextView viewTxtAccuray;
    private TextView viewTxtNumberSat;
    private TextView viewTxtStatusBluetooth;
    private ImageView viewStatusBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Init UI elements */
        viewTxtLastFix = findViewById(R.id.txtLastFix);
        viewTxtLongitude = findViewById(R.id.txtLongitude);
        viewTxtLatitude = findViewById(R.id.txtLatitude);
        viewTxtAltitude = findViewById(R.id.txtAltitude);
        viewTxtSpeed = findViewById(R.id.txtSpeed);
        viewTxtAccuray = findViewById(R.id.txtAccuray);
        viewTxtNumberSat = findViewById(R.id.txtNumberSat);
        viewTxtStatusBluetooth = findViewById(R.id.txtBtStatus);
        viewStatusBluetooth = findViewById(R.id.imageStatusBT);

        mIntentService = new Intent(this, GService.class);

        if (BluetoothAdapter.getDefaultAdapter() == null) {
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
        stopService(mIntentService);
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


    public void onClickBTDiscover(View view) {
        // start bt
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        } else {
            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    public void onClickStart(View view) {
        if(view instanceof ToggleButton) {
            ToggleButton btn = (ToggleButton) view;
            if(btn.isChecked()) {
                startGPSService();
            } else {
                stopGPSService();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK) {
                onClickBTDiscover(null);
            }
        }
    }

    /**
     * Start requiring location update.
     */
    private void startGPSService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    LOCATION_PERMISSION);
            return;
        }
        mIntentService.setAction(Constants.ACTION_START_BT_SERVICE);
        startService(mIntentService);
    }

    private void stopGPSService() {
        mIntentService.setAction(Constants.ACTION_STOP_BT_SERVICE);
        startService(mIntentService);
        stopService(mIntentService);
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
                        startGPSService();
                }
                break;
        }
    }

    /**
     * Update the UI for location change.
     * @param location Location fixed.
     */
    private void updateLocationUI(Location location) {
        viewTxtLastFix.setText(Utils.formatDateTime(Utils.localDateFromUTC(location.getTime())));
        viewTxtLongitude.setText(String.format(Locale.getDefault(), "%f", location.getLongitude()));
        viewTxtLatitude.setText(String.format(Locale.getDefault(), "%f", location.getLatitude()));
        viewTxtAccuray.setText(location.hasAccuracy() ? String.format(Locale.getDefault(), "%.5f m", location.getAccuracy()) : getString(R.string.empty));
        viewTxtAltitude.setText(location.hasAltitude() ? String.format(Locale.getDefault(), "%.5f m", location.getAltitude()) : getString(R.string.empty));
        viewTxtSpeed.setText(location.hasSpeed() ? String.format(Locale.getDefault(), "%.5f m/s", location.getSpeed()) : getString(R.string.empty));
        if(location.getExtras() != null)
            viewTxtNumberSat.setText(String.format(Locale.getDefault(), "%d", (int) location.getExtras().get("satellites")));
        else
            viewTxtNumberSat.setText(getString(R.string.empty));

    }

    private void updateBTStatusUI(int newState, String deviceName) {
        switch (newState) {
            case BTManager.STATE_LISTEN:
                viewStatusBluetooth.setImageResource(android.R.color.holo_orange_light);
                viewTxtStatusBluetooth.setText(getString(R.string.label_bt_status_listen));
                break;
            case BTManager.STATE_CONNECTED:
                viewStatusBluetooth.setImageResource(android.R.color.holo_green_light);
                viewTxtStatusBluetooth.setText(String.format(Locale.getDefault(), getString(R.string.label_bt_status_connected), deviceName));
                break;
            case BTManager.STATE_NONE:
                viewStatusBluetooth.setImageResource(android.R.color.holo_red_light);
                viewTxtStatusBluetooth.setText(getString(R.string.label_bt_status_none));
                break;
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mService = ((GService.LocalBinder)iBinder).getService();
        mService.setGServiceCallback(this);
        if(mService.getBTManager() != null)
            updateBTStatusUI(mService.getBTManager().getState(), (mService.getBTManager().getDeviceConnected() != null) ? mService.getBTManager().getDeviceConnected().getName() : null);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mService.setGServiceCallback(null);
        mService = null;
    }

    @Override
    public void onBTStateChange(int newState, String name) {
        updateBTStatusUI(newState, name);
    }

    @Override
    public void onLocationUpdate(Location location) {
        updateLocationUI(location);
    }
}
