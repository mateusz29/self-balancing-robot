package com.example.bluetoothrobotcontrol

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListView: ListView
    private lateinit var btnEnableBt: Button
    private lateinit var btnScan: Button
    private lateinit var btnForward: Button
    private lateinit var btnBackward: Button
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnStop: Button

    private var bluetoothSocket: BluetoothSocket? = null
    private val UUID_HC05 = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // HC-05 default UUID

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth wasn't enabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        initializeUI()
        setupClickListeners()
    }

    private fun initializeUI() {
        deviceListView = findViewById(R.id.deviceListView)
        btnEnableBt = findViewById(R.id.btnEnableBt)
        btnScan = findViewById(R.id.btnScan)
        btnForward = findViewById(R.id.btnForward)
        btnBackward = findViewById(R.id.btnBackward)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnStop = findViewById(R.id.btnStop)
    }

    private fun setupClickListeners() {
        btnEnableBt.setOnClickListener {
            enableBluetooth()
        }

        btnScan.setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            scanForDevices()
        }

        btnForward.setOnClickListener { sendCommand("F") }
        btnBackward.setOnClickListener { sendCommand("B") }
        btnLeft.setOnClickListener { sendCommand("L") }
        btnRight.setOnClickListener { sendCommand("R") }
        btnStop.setOnClickListener { sendCommand("S") }

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceListView.adapter.getItem(position) as BluetoothDevice
            connectToDevice(device)
        }
    }

    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED
            ) {
                enableBluetoothLauncher.launch(enableBtIntent)
            } else {
                requestBluetoothPermissions()
            }
        } else {
            Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scanForDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetoothPermissions()
            return
        }

        val pairedDevices = bluetoothAdapter.bondedDevices
        val deviceList = ArrayList<BluetoothDevice>()
        pairedDevices.forEach { device ->
            deviceList.add(device)
        }

        val adapter = BluetoothDeviceAdapter(this, deviceList)
        deviceListView.adapter = adapter
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetoothPermissions()
            return
        }

        Thread {
            try {
                bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(UUID_HC05)
                bluetoothSocket?.connect()
                runOnUiThread {
                    Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                }
                bluetoothSocket?.close()
                bluetoothSocket = null
            }
        }.start()
    }

    private fun sendCommand(command: String) {
        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                bluetoothSocket!!.outputStream.write(command.toByteArray())
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to send command", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ),
            1
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothSocket?.close()
    }
}