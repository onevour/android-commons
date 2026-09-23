package com.onevour.sdk.impl.modules.bluetooth.services.v1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import android.os.Binder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.onevour.core.utilities.commons.RefSession;
import com.onevour.sdk.impl.modules.bluetooth.PrinterManager;

// https://sahityakumarsuman.medium.com/android-service-communication-with-activity-2c01e537ab03

public class BluetoothSDKService extends Service {

    private final LocalBinder binder = new LocalBinder();

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final UUID uuid = UUID.fromString("00000000-0000-1000-8000-00805f9b34fb");

    private final String TAG = BluetoothSDKService.class.getSimpleName();

    private Service Binder;

    // Bluetooth stuff
    private BluetoothAdapter bluetoothAdapter;

    private Set<BluetoothDevice> pairedDevices;

    private BluetoothDevice connectedDevice;

    private final int RESULT_INTENT = 15;

    // Bluetooth connections
    public AcceptThread acceptThread;

    public ConnectThread connectThread;

    public ConnectedThread connectedThread;

    private int status = 0;

    @SuppressLint("MissingPermission")
    private final BluetoothSDKListener listener = new BluetoothSDKListener() {

        @Override
        public void onDiscoveryStarted() {

        }

        @Override
        public void onDiscoveryStopped() {

        }

        @Override
        public void onDeviceDiscovered(BluetoothDevice device) {
            Log.d(TAG, "receive discovered " + device.getName());
        }


        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            if (Objects.isNull(device)) {
                Log.d(TAG, "service listener receive connected as service");
            } else {
                Log.d(TAG, "service listener receive connected as client " + device.getName());
            }

        }

        @Override
        public void onMessageReceived(BluetoothDevice device, String message) {
            Log.d(TAG, "service listener receive message on listener " + message);
        }

        @Override
        public void onMessageSent(BluetoothDevice device) {

        }

        @Override
        public void onError(String message) {
            Log.d(TAG, "service listener receive error " + message);
            if ("Input stream was disconnected".equalsIgnoreCase(message)) {
                status = 0;
                acceptThread = new AcceptThread(bluetoothAdapter);
                acceptThread.start();
            }
        }

        @Override
        public void onDeviceDisconnected() {
            Log.d(TAG, "service listener receive disconnected");
        }
    };

    /**
     * BLUETOOTH_CONNECT (API 31+) gates almost every BluetoothAdapter call. Starting
     * the service without it throws SecurityException and crashes the app.
     */
    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "service created");
        if (!hasBluetoothConnectPermission()) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission, service will not start");
            pushBroadcastMessage(BluetoothUtils.ACTION_CONNECTION_ERROR, null, "Missing Bluetooth permission");
            stopSelf();
            return;
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothSDKListenerHelper.registerBluetoothSDKListener(getApplicationContext(), listener);
        if (Objects.isNull(bluetoothAdapter)) {
            Log.e(TAG, "Bluetooth not supported on this device, service will not start");
            pushBroadcastMessage(BluetoothUtils.ACTION_CONNECTION_ERROR, null, "Bluetooth not supported on this device");
            stopSelf();
            return;
        }
        acceptThread = new AcceptThread(bluetoothAdapter);
        acceptThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            BluetoothSDKListenerHelper.unregisterBluetoothSDKListener(this, listener);
        } catch (Exception e) {
            // already unregistered
        }
        Log.d(TAG, "service destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private synchronized void startConnectedThread(BluetoothSocket bluetoothSocket) {
        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
    }

    private void pushBroadcastMessage(String action, BluetoothDevice device, String message) {
        Intent intent = new Intent(action);
        if (Objects.nonNull(device)) {
            intent.putExtra(BluetoothUtils.EXTRA_DEVICE, device);
        }
        if (Objects.nonNull(message)) {
            intent.putExtra(BluetoothUtils.EXTRA_MESSAGE, message);
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public void write(String message) {
        if (Objects.nonNull(connectedThread) && status == 1) {
            connectedThread.write(message.getBytes());
        } else {
            pushBroadcastMessage(BluetoothUtils.ACTION_DEVICE_DISCONNECTED, null, "disconnected");
            // try connect to server
            connectToServer();
        }

    }

    private final RefSession refSession = new RefSession();

    @SuppressLint("MissingPermission")
    public void connectToServer() {
        if (Objects.isNull(bluetoothAdapter)) {
            pushBroadcastMessage(BluetoothUtils.ACTION_CONNECTION_ERROR, null, "Bluetooth not supported on this device");
            return;
        }
        String configPrinter = refSession.findString("device_bt_address");
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (null == device.getAddress()) continue;
            Log.d(TAG, "device name " + device.getName());
            if (device.getAddress().equals(configPrinter)) {
                connectThread = new ConnectThread(device);
                connectThread.start();
                break;
            } else {

            }
        }
    }

    public String getConnectionStatus() {
        return 1 == status ? "Connected" : "Disconnect";
    }

    public int getConnectionStatusCode() {
        return status;
    }

    @SuppressLint("MissingPermission")
    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(BluetoothAdapter bluetoothAdapter) {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("OnevourBtChat", uuid);
            } catch (IOException e) {
                Log.d(TAG, "service start error", e);
            }
            this.mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    Log.d(TAG, "waiting receive connection");
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (Objects.nonNull(socket)) {
                    Log.d(TAG, "new connection accept");
                    // Do work to manage the connection (in a separate thread)
                    status = 1;
                    pushBroadcastMessage(BluetoothUtils.ACTION_DEVICE_CONNECTED, null, "connect as server");
                    manageConnectedSocketConnected(socket);

                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {

                    }
                    break;
                }
            }
            Log.d(TAG, "AcceptThread is done");
        }

        public void cancel() {
            try {
                if (Objects.nonNull(mmServerSocket)) {
                    mmServerSocket.close();
                }
            } catch (IOException e) {

            }
        }
    }

    @SuppressLint("MissingPermission")
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;

        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                status = 1;
                pushBroadcastMessage(BluetoothUtils.ACTION_DEVICE_CONNECTED, mmDevice, "connect as client");
                manageConnectedSocketConnected(mmSocket);
            } catch (IOException connectException) {
                status = 0;
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;

        private final InputStream mmInStream;

        private final OutputStream mmOutStream;

        private final byte[] mmBuffer = new byte[1024]; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                pushBroadcastMessage(BluetoothUtils.ACTION_CONNECTION_ERROR, null, "HttpErrorResponse getting streams");
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            int numBytes; // bytes returned from read()
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);

                    String message = new String(mmBuffer, 0, numBytes);
                    Log.d(TAG, "receive message " + message);

                    pushBroadcastMessage(BluetoothUtils.ACTION_MESSAGE_RECEIVED, mmSocket.getRemoteDevice(), message);
                } catch (IOException e) {
                    status = 0;
                    pushBroadcastMessage(BluetoothUtils.ACTION_CONNECTION_ERROR, null, "Input stream was disconnected");
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                // Send to broadcast the message
                pushBroadcastMessage(BluetoothUtils.ACTION_MESSAGE_SENT, mmSocket.getRemoteDevice(), null);
            } catch (IOException e) {
                status = 0;
                pushBroadcastMessage(BluetoothUtils.ACTION_DEVICE_DISCONNECTED, null, "diconnected");
                pushBroadcastMessage(BluetoothUtils.ACTION_CONNECTION_ERROR, null, "HttpErrorResponse occurred when sending data");
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                pushBroadcastMessage(BluetoothUtils.ACTION_CONNECTION_ERROR, null, "Could not close the connect socket");
            }
        }

        // Body
    }

    private void manageConnectedSocketConnected(BluetoothSocket socket) {
        if (Objects.nonNull(connectedThread)) connectedThread.cancel();
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    /**
     * Class used for the client Binder.
     */
    @SuppressLint("MissingPermission")
    public class LocalBinder extends Binder {


        /**
         * Enable the discovery, registering a BroadcastReceiver {@link discoveryBroadcastReceiver}
         * The discovery is filtered by LABELER_SERVER_TOKEN_NAME
         */

        public void startDiscovery(Context context) {
            if (Objects.isNull(bluetoothAdapter)) {
                pushBroadcastMessage(BluetoothUtils.ACTION_CONNECTION_ERROR, null, "Bluetooth not supported on this device");
                return;
            }
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//            registerReceiver(discoveryBroadcastReceiver, filter);
            bluetoothAdapter.startDiscovery();
            pushBroadcastMessage(BluetoothUtils.ACTION_DISCOVERY_STARTED, null, null);
        }

        /**
         * Stop discovery
         */
        public void stopDiscovery() {
            if (Objects.isNull(bluetoothAdapter)) return;
            bluetoothAdapter.cancelDiscovery();
            pushBroadcastMessage(BluetoothUtils.ACTION_DISCOVERY_STOPPED, null, null);
        }

        public BluetoothSDKService getService() {
            return BluetoothSDKService.this;
        }

    }

}