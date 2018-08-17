package com.example.petretiandrea.gpsreceiver.bluetooth;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.example.petretiandrea.gpsreceiver.util.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BTManager {

    private static final String TAG = BTManager.class.getName();
    private static final String SERVICE_NAME = Constants.BT_NAME;
    private static final UUID SERVICE_UUID = Constants.MAGIC_UUID;

    /**
     * Handler what reason, when the state of service change
     */
    public static final int STATE_CHANGED = 1;
    /**
     * State of service when a BT Client is connected.
     */
    public static final int STATE_CONNECTED = 3;
    /**
     * State of service when a BT Client is connecting.
     */
    public static final int STATE_CONNECTING = 2;
    /**
     * State of service when the BT server are ready for receive connections.
     */
    public static final int STATE_LISTEN = 1;
    /**
     * State of service when is stopped.
     */
    public static final int STATE_NONE = 0;
    /**
     * Current state.
     */
    private int mState;
    /**
     * Thead that accept incoming BT connections.
     */
    private AcceptThread mAcceptThread;
    /**
     * Thread for manage the connected BT client.
     */
    private ConnectedThread mConnectedThread;
    /**
     * Handler for send message to ui or to background service.
     */
    private final Handler mHandler;

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private boolean justDie;

    /**
     * Constructor for BTManager.
     * @param handler Handler for communicate.
     */
    public BTManager(Handler handler) {
        mHandler = handler;
        mState = STATE_NONE;
        justDie = false;
    }

    /**
     * Get actual state.
     * @return The current state of BT.
     */
    public synchronized int getState() {
        return this.mState;
    }

    /**
     * Change the current state, and notify the change using handler.
     * @param state New state.
     */
    private synchronized void setState(int state) {
        mState = state;
        mHandler.obtainMessage(STATE_CHANGED, state, -1, this).sendToTarget();
    }

    /**
     * Start the BT server, for wait new connections.
     */
    public synchronized void start() {
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    /**
     * Stop the BT server, and notify the event, using the handler.
     */
    public synchronized void stop() {
        justDie = true;
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Send a string to BT Client connected.
     * @param message Message to be send.
     */
    public void send(String message) {
        write(message.getBytes());
    }

    /**
     * Get the BT Device connected
     * @return BluetoothDevice object for currente device connected.
     */
    public BluetoothDevice getDeviceConnected() {
        if(mConnectedThread != null && mConnectedThread.isAlive())
            return mConnectedThread.mBluetoothDevice;
        return null;
    }


    /**
     * Start the thread for manage the single client, and notify using handler the bt device name.
     * @param socket BluetoothSocket for communicate with client.
     * @param device Bluetooth Device of connected client.
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        mConnectedThread = new ConnectedThread(socket, device);
        mConnectedThread.start();
        setState(STATE_CONNECTED);
    }

    /**
     * Write to connected Client.
     * @param out Byte to be send.
     */
    private void write(byte[] out) {
        synchronized (this) {
            if (mState == STATE_CONNECTED) {
                mConnectedThread.write(out);
            }
        }
    }

    /**
     * Called when the connection of BT Client is lost, automatically restart the BT server.
     */
    private void connectionLost() {
        if (!justDie) {
            setState(STATE_LISTEN);
            start();
        }
    }

    /**
     * Thread for manage incoming BT connections.
     */
    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mServerSocket;

        AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(BTManager.SERVICE_NAME, BTManager.SERVICE_UUID);
            } catch (IOException e) {
                Log.e(BTManager.TAG, "Socket listen() failed", e);
            }
            mServerSocket = tmp;
            setName("AcceptThread");
        }

        public void run() {
            while (mState != STATE_CONNECTED) {
                try {
                    BluetoothSocket socket = mServerSocket.accept();
                    if (socket != null) {
                        synchronized (BTManager.this) {
                            switch (mState) {
                                case STATE_NONE:
                                case STATE_CONNECTED: {
                                    try {
                                        socket.close();
                                    } catch (IOException e) {
                                        Log.e(BTManager.TAG, "Could not close unwanted socket", e);
                                    }
                                    break;
                                }
                                case STATE_LISTEN:
                                case STATE_CONNECTING:
                                    connected(socket, socket.getRemoteDevice());
                                    break;
                            }
                        }
                    }
                } catch (Exception e2) {
                    Log.e(BTManager.TAG, "Socket accept() failed", e2);
                    return;
                }
            }
            return;
        }

        void cancel() {
            try {
                if (mServerSocket != null)
                    mServerSocket.close();
            } catch (IOException e) {
                Log.e(BTManager.TAG, "Socket close() of server failed", e);
            }
        }
    }


    /**
     * Thread for manage the single BT Client connection.
     */
    private class ConnectedThread extends Thread {
        private final InputStream mInStream;
        private final OutputStream mOutStream;
        private final BluetoothSocket mSocket;

        private BluetoothDevice mBluetoothDevice;

        ConnectedThread(BluetoothSocket socket, BluetoothDevice bluetoothDevice) {
            mSocket = socket;
            mBluetoothDevice = bluetoothDevice;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(BTManager.TAG, "temp sockets not created", e);
            }
            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public BluetoothDevice getBluetoothDevice() {
            return mBluetoothDevice;
        }

        public void run() {
            boolean running = true;
            while (running) {
                try {
                    mInStream.read(new byte[1024]);
                } catch (IOException e) {
                    Log.e(BTManager.TAG, "disconnected", e);
                    connectionLost();
                    running = false;
                }
            }
        }

        void write(byte[] buffer) {
            try {
                mOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(BTManager.TAG, "Exception during write", e);
            }
        }

        void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(BTManager.TAG, "close() of connect socket failed", e);
            }
        }
    }
}
