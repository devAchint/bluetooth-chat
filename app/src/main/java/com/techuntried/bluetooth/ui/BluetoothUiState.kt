package com.techuntried.bluetooth.ui

import com.techuntried.bluetooth.domain.model.BluetoothDeviceItem

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceItem> = emptyList(),
    val pairedDevices:List<BluetoothDeviceItem> = emptyList(),
)
