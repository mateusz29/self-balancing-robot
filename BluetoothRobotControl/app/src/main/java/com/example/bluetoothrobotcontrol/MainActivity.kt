package com.example.bluetoothrobotcontrol

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListView: ListView
    private lateinit var btnScan: Button
    private lateinit var btnForward: Button
    private lateinit var btnBackward: Button
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnStop: Button

    private lateinit var seekBarAngle: SeekBar
    private lateinit var seekBarP: SeekBar
    private lateinit var seekBarI: SeekBar
    private lateinit var seekBarD: SeekBar
    private lateinit var tvAngle: TextView
    private lateinit var tvP: TextView
    private lateinit var tvI: TextView
    private lateinit var tvD: TextView

    private var bluetoothSocket: BluetoothSocket? = null
    private val UUID_HC05 = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        initializeUI()
        setupClickListeners()
        setupSliderListeners()
    }

    private fun initializeUI() {
        deviceListView = findViewById(R.id.deviceListView)
        btnScan = findViewById(R.id.btnScan)
        btnForward = findViewById(R.id.btnForward)
        btnBackward = findViewById(R.id.btnBackward)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnStop = findViewById(R.id.btnStop)

        seekBarAngle = findViewById(R.id.seekBarAngle)
        seekBarP = findViewById(R.id.seekBarP)
        seekBarI = findViewById(R.id.seekBarI)
        seekBarD = findViewById(R.id.seekBarD)

        tvAngle = findViewById(R.id.tvAngle)
        tvP = findViewById(R.id.tvP)
        tvI = findViewById(R.id.tvI)
        tvD = findViewById(R.id.tvD)

        seekBarAngle.max = 60
        seekBarAngle.progress = 24
        seekBarP.max = 100
        seekBarP.progress = 20
        seekBarI.max = 500
        seekBarI.progress = 200
        seekBarD.max = 50
        seekBarD.progress = 12
    }

    private fun setupClickListeners() {
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

        findViewById<Button>(R.id.btnSendAngle).setOnClickListener {
            val angle = seekBarAngle.progress - 30
            sendCommand("A$angle")
        }

        findViewById<Button>(R.id.btnSendP).setOnClickListener {
            val p = seekBarP.progress
            sendCommand("P1$p")
        }

        findViewById<Button>(R.id.btnSendI).setOnClickListener {
            val i = seekBarI.progress
            sendCommand("P2$i")
        }

        findViewById<Button>(R.id.btnSendD).setOnClickListener {
            val dValue = seekBarD.progress / 10.0
            sendCommand("P3%.1f".format(dValue))
        }


        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceListView.adapter.getItem(position) as BluetoothDevice
            connectToDevice(device)
        }
    }

    private fun setupSliderListeners() {
        seekBarAngle.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val angle = progress - 30
                tvAngle.text = getString(R.string.angle_text, angle)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarP.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvP.text = getString(R.string.p_text, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarI.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvI.text = getString(R.string.i_text, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarD.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val dValue = progress / 10.0
                tvD.text = getString(R.string.d_text, dValue)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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