package com.techuntried.bluetooth.domain

import com.techuntried.bluetooth.domain.model.BluetoothDeviceItem
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceItem>>
    val pairedDevices: StateFlow<List<BluetoothDeviceItem>>

    fun startDiscovery()
    fun stopDiscovery()
    fun release()
}