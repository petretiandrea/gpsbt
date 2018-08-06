package com.example.petretiandrea.gpsreceiver.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.petretiandrea.gpsreceiver.util.Constants;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class BTServer implements Runnable {

    public enum Status {
        WAITING_CONNECTION,
        CONNECTED
    }

    private static final String TAG = BTServer.class.getName();
    private final BluetoothServerSocket mServerSocket;
    private volatile boolean mRunning;

    private BTClient mBTClientConnected;
    private Status mStatus;

    private ReentrantLock mLock;


    public BTServer(BluetoothAdapter bluetoothAdapter) {
        BluetoothServerSocket tmp = null;
        try {
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("SS", UUID.fromString(Constants.MAGIC_UUID));
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Listen() Bluetooth Failed");
        }
        mServerSocket = tmp;
        mRunning = true;
        mBTClientConnected = null;
        mStatus = Status.WAITING_CONNECTION;
        mLock = new ReentrantLock();
    }

    public Status getStatus() {
        return mStatus;
    }

    public BTClient getBTClientConnected() {
        return mBTClientConnected;
    }

    @Override
    public void run() {
        BluetoothSocket socket = null;
        while (mRunning) {
            try {
                System.out.println("Waiting Socket connect!");
                socket = mServerSocket.accept();
                System.out.println("uiuuu");
            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                mRunning = false;
            }

            if (socket != null) {
                manageConnectedSocket(socket);
                try {
                    mServerSocket.close();
                    mRunning = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "BT Server Thread End");
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        Log.d(TAG, "cancel BT Server Called!");
        try {
            if(mBTClientConnected != null) mBTClientConnected.cancel();
            mRunning = false;
            mServerSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, "Could not close the socket", ex);
        }
    }

    // handle incoming bluetooth connection
    private void manageConnectedSocket(BluetoothSocket socket) {
        mBTClientConnected = new BTClient(socket);
    }
}
