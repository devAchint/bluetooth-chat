package com.techuntried.bluetooth.domain

import com.techuntried.bluetooth.domain.model.BluetoothDeviceItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceItem>>
    val pairedDevices: StateFlow<List<BluetoothDeviceItem>>
    val isConnected :StateFlow<Boolean>
    val error:SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()

    fun startBluetoothServer(): Flow<ConnectionResult>
    fun connectToDevice(deviceItem: BluetoothDeviceItem):Flow<ConnectionResult>

    fun closeConnection()
    fun release()
}