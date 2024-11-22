package com.example.bluetoothrobotcontrol

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.app.ActivityCompat

class BluetoothDeviceAdapter(
    context: Context,
    private val devices: List<BluetoothDevice>
) : ArrayAdapter<BluetoothDevice>(context, android.R.layout.simple_list_item_1, devices) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        val device = devices[position]
        val textView = view.findViewById<TextView>(android.R.id.text1)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            textView.text = "${device.name ?: "Unknown Device"} (${device.address})"
        }

        return view
    }

    override fun getItem(position: Int): BluetoothDevice {
        return devices[position]
    }
}