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
    private lateinit var btnSendBalancePoint: Button
    private lateinit var btnSendP: Button
    private lateinit var btnSendI: Button
    private lateinit var btnSendD: Button
    private lateinit var btnScan: Button
    private lateinit var btnForward: Button
    private lateinit var btnBackward: Button
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnStop: Button
    private lateinit var btnResetBalancePoint: Button
    private lateinit var btnResetP: Button
    private lateinit var btnResetI: Button
    private lateinit var btnResetD: Button

    private lateinit var seekBarBalancePoint: SeekBar
    private lateinit var seekBarP: SeekBar
    private lateinit var seekBarI: SeekBar
    private lateinit var seekBarD: SeekBar
    private lateinit var tvBalancePoint: TextView
    private lateinit var tvP: TextView
    private lateinit var tvI: TextView
    private lateinit var tvD: TextView

    private var bluetoothSocket: BluetoothSocket? = null
    private val UUID_HC05 = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        const val DEFAULT_BALANCE_POINT_PROGRESS = 20
        const val DEFAULT_P_PROGRESS = 20
        const val DEFAULT_I_PROGRESS = 180
        const val DEFAULT_D_PROGRESS = 12
        const val DEFAULT_BALANCE_POINT_VALUE = -5.0
    }

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
        btnSendBalancePoint = findViewById(R.id.btnSendBalancePoint)
        btnSendP = findViewById(R.id.btnSendP)
        btnSendI = findViewById(R.id.btnSendI)
        btnSendD = findViewById(R.id.btnSendD)
        btnForward = findViewById(R.id.btnForward)
        btnBackward = findViewById(R.id.btnBackward)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnStop = findViewById(R.id.btnStop)
        btnResetBalancePoint = findViewById(R.id.btnResetBalancePoint)
        btnResetP = findViewById(R.id.btnResetP)
        btnResetI = findViewById(R.id.btnResetI)
        btnResetD = findViewById(R.id.btnResetD)

        seekBarBalancePoint = findViewById(R.id.seekBarBalancePoint)
        seekBarP = findViewById(R.id.seekBarP)
        seekBarI = findViewById(R.id.seekBarI)
        seekBarD = findViewById(R.id.seekBarD)

        tvBalancePoint = findViewById(R.id.tvBalancePoint)
        tvP = findViewById(R.id.tvP)
        tvI = findViewById(R.id.tvI)
        tvD = findViewById(R.id.tvD)

        seekBarBalancePoint.max = 40
        seekBarBalancePoint.progress = DEFAULT_BALANCE_POINT_PROGRESS
        seekBarP.max = 100
        seekBarP.progress = DEFAULT_P_PROGRESS
        seekBarI.max = 500
        seekBarI.progress = DEFAULT_I_PROGRESS
        seekBarD.max = 50
        seekBarD.progress = DEFAULT_D_PROGRESS
    }

    private fun setupClickListeners() {
        btnScan.setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            scanForDevices()
        }

        btnForward.setOnClickListener { sendCommand("F\n") }
        btnBackward.setOnClickListener { sendCommand("B\n") }
        btnLeft.setOnClickListener { sendCommand("L\n") }
        btnRight.setOnClickListener { sendCommand("R\n") }
        btnStop.setOnClickListener { sendCommand("S\n") }

        btnResetBalancePoint.setOnClickListener {
            seekBarBalancePoint.progress = DEFAULT_BALANCE_POINT_PROGRESS
            sendCommand("A${DEFAULT_BALANCE_POINT_VALUE}\n")
        }

        btnSendBalancePoint.setOnClickListener {
            val balance_point = (seekBarBalancePoint.progress - 20) / 2.0 - 5
            sendCommand("A$balance_point\n")
        }

        btnResetP.setOnClickListener {
            seekBarP.progress = DEFAULT_P_PROGRESS
            sendCommand("P${DEFAULT_P_PROGRESS}\n")
        }

        btnSendP.setOnClickListener {
            sendCommand("P${seekBarP.progress}\n")
        }

        btnResetI.setOnClickListener {
            seekBarI.progress = DEFAULT_I_PROGRESS
            sendCommand("I${DEFAULT_I_PROGRESS}\n")
        }

        btnSendI.setOnClickListener {
            sendCommand("I${seekBarI.progress}\n")
        }

        btnResetD.setOnClickListener {
            seekBarD.progress = DEFAULT_D_PROGRESS
            sendCommand("D${DEFAULT_D_PROGRESS / 10.0}\n")
        }

        btnSendD.setOnClickListener {
            val dValue = seekBarD.progress / 10.0
            sendCommand("D%.1f\n".format(dValue))
        }

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceListView.adapter.getItem(position) as BluetoothDevice
            connectToDevice(device)
        }
    }

    private fun setupSliderListeners() {
        seekBarBalancePoint.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val balance_point = (progress - 20) / 2.0
                tvBalancePoint.text = getString(R.string.balance_point_text, balance_point)
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