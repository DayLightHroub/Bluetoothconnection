package com.example.bluetoothconnection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BlueToothConnectionService {

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    private int mState;
    private static final String TAG = "ConnectionService";
    private static final String APP_NAME = "BTAPP";

    private static final UUID MY_UUID = UUID.fromString("476af1b5-0587-4457-81f6-af514dc8354b");

    private final BluetoothAdapter mBloutoothAdapter;
    Context context;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;


    public BlueToothConnectionService(Context context) {
        this.context = context;
        mBloutoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mState = STATE_NONE;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }


        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }

    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.d(TAG, "connected method startewd");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {

        mState = STATE_NONE;

        // Start the service over to restart listening mode
        BlueToothConnectionService.this.start();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity

        mState = STATE_NONE;
        // Update UI title


        // Start the service over to restart listening mode
        BlueToothConnectionService.this.start();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);


        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }


        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public int getState() {
        return mState;
    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        private AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBloutoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        APP_NAME, MY_UUID);

                Log.d(TAG, "AcceptingTrhead: setting up server");
            } catch (IOException e) {
                Log.d(TAG, "Accepting Thread IO Exception: " + e.getMessage());
            }

            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            Log.d(TAG, "AcceptingThread runing");
            BluetoothSocket socket = null;


            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception

                    Log.d(TAG, "RFCOM server socket start");
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "RFCOM server socket accepted");
                } catch (IOException e) {
                    Log.d(TAG, "Accepting Thread RUN IO Exception: " + e.getMessage());
                    break;
                }
            }


            // If a connection was accepted
            if (socket != null) {
                synchronized (BlueToothConnectionService.this) {
                    switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.d(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                    }
                }
            }

            Log.d(TAG, "END mAcceptThread");

            Log.d(TAG, "End run method");
        }

        public void cancel() {
            Log.d(TAG, "Cancling acceptthread");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancling acceptedthreadh io exception = " + e.getMessage());
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private ConnectThread(BluetoothDevice device) {
            Log.d(TAG, "ConnectThread started");
            mmDevice = device;


            BluetoothSocket tmp = null;
            try {
                Log.d(TAG, "ConnectThread creaitng RFCOM SOCKET");
                tmp = device.createInsecureRfcommSocketToServiceRecord(
                        MY_UUID);
            } catch (IOException e) {
                Log.d(TAG, "ConnectThread IOException = " + e.getMessage());
            }

            mmSocket = tmp;
            mState = STATE_CONNECTING;

        }

        public void run() {
            Log.d(TAG, "ConnectThread: Run method started");
            mBloutoothAdapter.cancelDiscovery();

            try {
                Log.d(TAG, "ConnectThread: connecting to socket");
                mmSocket.connect();
            } catch (IOException e) {
                Log.d(TAG, "ConnectThread: IOEXCEPTION connecting to socket = " + e.getMessage());
                cancel();
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BlueToothConnectionService.this) {
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {

            Log.d(TAG, "ConnectThread: close socket");
            try {
                mmSocket.close();
            } catch (IOException e1) {
                Log.d(TAG, "ConnectThread closing failed = " + e1.getMessage());
            }

        }

    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread creating");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            Log.d(TAG, "ConnectedThread getting input/output socket streams");
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;

        }

        public void run() {
            Log.d(TAG, "ConnectewdThread run method started: ");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    //mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                    //      .sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "ConnectedThread run IOexception disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                Log.d(TAG, new String(buffer, Charset.defaultCharset()));
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                //mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                //     .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread Write , Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }


    }


}
