package com.example.petretiandrea.gpsreceiver;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends AppCompatActivity implements OnNmeaMessageListener, LocationListener {

    private static final int LOCATION_PERMISSION = 1000;
    private LocationManager mLocationManger;

    private TCPServer mTCPServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationManger = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    public void onClickGpsStart(View view) {
        try {
            mTCPServer = new TCPServer();
            mTCPServer.start();
            startRequestUpdateLocation();
        } catch (IOException e) {
            e.printStackTrace();
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

        mLocationManger.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, this);
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
    protected void onDestroy() {
        super.onDestroy();
        if(mTCPServer != null)
            mTCPServer.cancel();
    }

    @Override
    public void onNmeaMessage(String s, long l) {
        //System.out.println("Nmea: " + s);
        if(mTCPServer != null)
            mTCPServer.writNmea(s);
    }

    @Override
    public void onLocationChanged(Location location) {

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


    private class TCPServer extends Thread {

        private ServerSocket mServerSocket;
        private ConcurrentLinkedQueue<Socket> mClientList;

        public TCPServer() throws IOException {
            mServerSocket = new ServerSocket(6000);
            mClientList = new ConcurrentLinkedQueue<>();
        }

        public void writNmea(final String data) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for(Socket client : mClientList) {
                        try {
                            client.getOutputStream().write(data.getBytes());
                            System.out.println("Sended to Client new data");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        @Override
        public void run() {
            super.run();

            while (!mServerSocket.isClosed()) {
                try {
                    System.out.println("Listening...");
                    Socket socket = mServerSocket.accept();
                    System.out.println("Client connected!");
                    mClientList.add(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            interrupt();
        }
    }

}
