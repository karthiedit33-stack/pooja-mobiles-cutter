package com.poojamobiles.cutter;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final UUID SKY_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID SKY_CHAR_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    
    BluetoothSocket btSocket = null;
    OutputStream outputStream = null;
    EditText txtWidth, txtHeight, txtText;
    Button btnConnect, btnCut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtWidth = findViewById(R.id.txtWidth);
        txtHeight = findViewById(R.id.txtHeight);
        txtText = findViewById(R.id.txtText);
        btnConnect = findViewById(R.id.btnConnect);
        btnCut = findViewById(R.id.btnCut);

        btnConnect.setOnClickListener(v -> connectBluetooth());
        btnCut.setOnClickListener(v -> sendCutCommand());
    }

    private void connectBluetooth() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        // Connection logic for paired Skycut printer
        for (BluetoothDevice device : btAdapter.getBondedDevices()) {
            if (device.getName() != null && (device.getName().contains("SC") || device.getName().contains("Skycut") || device.getName().contains("BLE"))) {
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    btSocket = device.createRfcommSocketToServiceRecord(SKY_UUID);
                    btSocket.connect();
                    outputStream = btSocket.getOutputStream();
                    Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_LONG).show();
                    break;
                } catch (Exception e) {
                    Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void sendCutCommand() {
        if (outputStream == null) {
            Toast.makeText(this, "Please connect Bluetooth first!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String wStr = txtWidth.getText().toString();
            String hStr = txtHeight.getText().toString();
            float widthMm = wStr.isEmpty() ? 50f : Float.parseFloat(wStr);
            float heightMm = hStr.isEmpty() ? 50f : Float.parseFloat(hStr);

            // HPGL Scaling: 1 mm = 40 plotter units
            int wUnits = (int) (widthMm * 40.0f);
            int hUnits = (int) (heightMm * 40.0f);

            // Command format derived from reverse engineering
            String cmd = ";BD:100,10,20;BD:101,9;PU0,0;PD" + wUnits + ",0;PD" + wUnits + "," + hUnits + ";PD0," + hUnits + ";PD0,0;PU0,0;@";
            outputStream.write(cmd.getBytes());
            Toast.makeText(this, "Cutting Sent Successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error sending data", Toast.LENGTH_SHORT).show();
        }
    }
}
