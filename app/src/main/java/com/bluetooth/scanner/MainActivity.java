package com.bluetooth.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SimpleBluetooth";
    private BluetoothAdapter bluetoothAdapter;
    private String btScan = android.Manifest.permission.BLUETOOTH_SCAN;
    private String btConnect = android.Manifest.permission.BLUETOOTH_CONNECT;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvResult = findViewById(R.id.tv_result);
        new Handler(Looper.getMainLooper()).postDelayed(this::requestPermission, 1000);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, btScan) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, btConnect) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{btScan, btConnect}, 99);
            } else {
                setUpBluetooth();
            }
        } else {
            setUpBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        setUpBluetooth();
    }

    private void setUpBluetooth() {
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            findBluetoothDevices();
        } else {
            if (ActivityCompat.checkSelfPermission(this, btConnect) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }
    }

    private void findBluetoothDevices() {
        if (ActivityCompat.checkSelfPermission(this, btConnect) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, "deviceName : " + deviceName);
                Log.d(TAG, "deviceHardwareAddress : " + deviceHardwareAddress);
                if (deviceName.equals("BarCode Scanner spp")) {
                    try {
                        ParcelUuid[] uuid = device.getUuids();
                        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid[0].getUuid());
                        socket.connect();
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                new MyBluetoothService(socket, new MyBluetoothService.Listener() {
                                    @Override
                                    public void onReceive(String data) {
                                        showResult(data);
                                    }

                                    @Override
                                    public void onSend(String data) {
                                        showResult(data);
                                    }
                                });
                            }
                        }.start();
                        Toast.makeText(this, "Socket connected", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private void showResult(String data) {
        runOnUiThread(() -> {
            Log.d(TAG, "message : " + data);
            Toast.makeText(MainActivity.this, "message : " + data, Toast.LENGTH_SHORT).show();
            tvResult.setText(data);
        });
    }
}