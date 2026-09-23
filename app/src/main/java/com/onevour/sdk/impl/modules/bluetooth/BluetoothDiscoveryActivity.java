package com.onevour.sdk.impl.modules.bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.onevour.core.utilities.commons.RefSession;
import com.onevour.core.utilities.helper.UIHelper;
import com.onevour.sdk.impl.databinding.ActivityBluetoothDiscoveryBinding;
import com.onevour.sdk.impl.modules.bluetooth.components.AdapterDeviceBluetooth;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class BluetoothDiscoveryActivity extends AppCompatActivity implements AdapterDeviceBluetooth.HolderDeviceBluetoothListener {

    private static final String TAG = BluetoothDiscoveryActivity.class.getSimpleName();

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final RefSession refSession = new RefSession();

    private final AdapterDeviceBluetooth adapter = new AdapterDeviceBluetooth();

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothSocket socket = null;

    private ActivityBluetoothDiscoveryBinding binding;


    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (Objects.nonNull(device)) {
                    assert device != null;
                    toast("receive bt device: " + device.getName());
                    adapter.addMore(device);
                }
            }
        }

    };

    private final Runnable socketErrorRunnable = new Runnable() {

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            toast("Cannot establish connection");
            bluetoothAdapter.startDiscovery();
        }
    };



    private void toast(String message) {
        handler.post(() -> Toast.makeText(BluetoothDiscoveryActivity.this, "" + message, Toast.LENGTH_SHORT).show());
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBluetoothDiscoveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIHelper.initRecyclerView(binding.rvBluetooth, adapter, this);
        discoveryBluetoothDevices();
    }

    private void discoveryBluetoothDevices() {
        try {
            int registerDevice = initDevicesList();
            if (0 > registerDevice) {
                finish();
            }
            proceedDiscovery();
            // pair device
            if (Objects.nonNull(bluetoothAdapter)) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice bt : pairedDevices) {
                    adapter.addMore(bt);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "error bt", ex);
            //finish();
            toast(ex.getMessage());
        }
    }

    private int initDevicesList() {
        try {
            flushData();
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                toast("Bluetooth not supported!!");
                return -1;
            }
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 100);
            toast("Getting all available Bluetooth Devices");
            return 0;
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            return -2;
        }
    }

    private void flushData() {
        try {
            if (Objects.nonNull(socket)) {
                socket.close();
                socket = null;
            }

            if (Objects.nonNull(bluetoothAdapter)) {
                bluetoothAdapter.cancelDiscovery();
            }

            //finalize();
        } catch (Exception ex) {
            toast(ex.getMessage());
        }

    }


    protected void proceedDiscovery() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        registerReceiver(bluetoothReceiver, filter);
        bluetoothAdapter.startDiscovery();

    }

    UUID uuidSender = UUID.fromString("00000000-0000-1000-8000-00805f9b34fb");

    @Override
    public void onSelectedBluetoothDevice(BluetoothDevice device) {
        toast("selected device " + device.getName() + " | " + device.getAddress());
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (Objects.nonNull(device)) {
            refSession.saveString("device_bt_name", device.getName());
            refSession.saveString("device_bt_address", device.getAddress());
            toast("Connecting to " + device.getName() + "," + device.getAddress());
        } else {
            toast("No device detected!");
        }
        Thread connectThread = new Thread(() -> {
            try {
                if (Objects.isNull(device)) {
                    toast("No bluetooth device detected!");
                    return;
                }
                if (Objects.isNull(device.getUuids()) || device.getUuids().length == 0) {
                    toast("No bluetooth device uuid detected!");
                    return;
                }
                if (Objects.nonNull(socket) && socket.isConnected()) {
                    socket.close();
                }
                //UUID uuid = device.getUuids()[0].getUuid();

                socket = device.createRfcommSocketToServiceRecord(uuidSender);
                //socket = device.createRfcommSocketToServiceRecord(MY_UUID);

                socket.connect();
                toast("Connection establish");
            } catch (IOException ex) {
                boolean reconnect = false;
                if (Objects.requireNonNull(ex.getMessage()).contains("closed or timeout")) {
                    reconnect = connectionWithHiddenApi(device);
                }
                if (!reconnect) {
                    runOnUiThread(socketErrorRunnable);
                    try {
                        if (Objects.nonNull(socket)) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, ex.getMessage());
                    }
                    socket = null;
                }
            } finally {
                flushData();
                runOnUiThread(this::finish);
            }
        });
        connectThread.start();
        proceedDiscovery();
    }

    private boolean connectionWithHiddenApi(BluetoothDevice device) {
        try {
            socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
            assert socket != null;
            socket.connect();
            toast("Success attempt secondary connection");
            return true;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException e) {
            // toast("HttpErrorResponse attempt connection");
            return false;
        }
    }
}