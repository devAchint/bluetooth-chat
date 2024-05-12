package com.techuntried.bluetooth.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import com.techuntried.bluetooth.domain.BluetoothController
import com.techuntried.bluetooth.domain.ConnectionResult
import com.techuntried.bluetooth.domain.model.BluetoothDeviceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOError
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(private val context: Context) : BluetoothController {

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceItem>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceItem>>
        get() = _pairedDevices.asStateFlow()

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    override val error: SharedFlow<String>
        get() = _error.asSharedFlow()

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null


    private val deviceFoundReceiver = DeviceFoundReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = BluetoothDeviceItem(device.name, device.address)
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    private val bluetoothStateReceiver = BluetoothStateReceiver { connected, device ->
        if (bluetoothAdapter?.bondedDevices?.contains(device) == true) {
            _isConnected.update { connected }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _error.emit("can't connect to a non paired device")
            }

        }
    }

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            }
        )
    }

    override fun startDiscovery() {
        if (!hasPermissions(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.d("MYDEBUG", "return")
            return
        }
        context.registerReceiver(deviceFoundReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
        Log.d("MYDEBUG", "start")
    }

    override fun stopDiscovery() {
        if (!hasPermissions(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            if (!hasPermissions(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("permission not granted")
            }

            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "bluetooth_app",
                UUID.fromString(Service_UUID)
            )

            var shouldLoop = true
            while (shouldLoop) {
                currentClientSocket = try {
                    currentServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
            }
            emit(ConnectionResult.ConnectionEstablished)
            currentClientSocket?.let {
                currentServerSocket?.close()
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(deviceItem: BluetoothDeviceItem): Flow<ConnectionResult> {
        return flow {
            if (!hasPermissions(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("permission not granted")
            }

            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceItem.address)
            currentClientSocket = bluetoothDevice
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(Service_UUID)
                )

            stopDiscovery()

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished)
                } catch (e: IOException) {
                    socket.close()
                    currentServerSocket = null
                    emit(ConnectionResult.Error(""))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun closeConnection() {
        currentServerSocket?.close()
        currentClientSocket?.close()
        currentServerSocket = null
        currentClientSocket = null
    }

    override fun release() {
        context.unregisterReceiver(deviceFoundReceiver)
        context.unregisterReceiver(bluetoothStateReceiver)
        closeConnection()
    }


    private fun updatePairedDevices() {
        if (!hasPermissions(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.d("MYDEBUG", " update return")
            return
        }
        bluetoothAdapter?.bondedDevices?.map {
            BluetoothDeviceItem(
                name = it.name,
                address = it.address
            )
        }?.also { devices ->
            _pairedDevices.update { devices }

        }
        Log.d("MYDEBUG", "update")
    }

    private fun hasPermissions(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val Service_UUID = "7fc132e0-d08e-4e0d-b5d3-524fd92d1fd7"
    }
}