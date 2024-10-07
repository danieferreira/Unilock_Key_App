package com.df.unilockkey.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import com.df.unilockkey.data.ConnectionState
import com.df.unilockkey.data.KeyInfoResult
import com.df.unilockkey.data.KeyReceiverManager
import com.df.unilockkey.repository.DataRepository
import com.df.unilockkey.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class KeyBLEReceiverManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context,
    private val dataRepository: DataRepository
): KeyReceiverManager {
    private val SERVICE_UUID = "000000ff-0000-1000-8000-00805f9b34fb"
    private val CHAR1_UUID = "0000ff01-0000-1000-8000-00805f9b34fb"
    private val CHAR2_UUID = "0000ff02-0000-1000-8000-00805f9b34fb"
    private val CHAR3_UUID = "0000ff03-0000-1000-8000-00805f9b34fb"


    override val data: MutableSharedFlow<Resource<KeyInfoResult>> = MutableSharedFlow()

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var gatt: BluetoothGatt? = null
    private var isScanning = false
    private val coroutineScope= CoroutineScope(Dispatchers.Default)

    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            //Log.d("Scanning", "${result.device.name}")
            if(result.device.name == "UNILOCK_KEY") {
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Connecting..."))
                }
                if (isScanning) {
                    result.device.connectGatt(context, true, gattCallback)
                    isScanning = false
                    bleScanner.stopScan(this)
                }
            }
        }
    }

    private var currentConnectionAttempt = 1

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    coroutineScope.launch {
                        data.emit(Resource.Loading(message = "Discovering..."))
                    }
                    gatt.discoverServices()
                    this@KeyBLEReceiverManager.gatt = gatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    coroutineScope.launch {
                        data.emit(
                            Resource.Success(
                                data = KeyInfoResult(
                                    keyId = "",
                                    lockId = "",
                                    battVoltage = 0.0,
                                    keyVersion = "",
                                    date = null,
                                    ConnectionState.Disconnected
                                )
                            )
                        )
                    }
                    gatt.close()
                }
            } else {
                gatt.close()
                startReceiving()
//                currentConnectionAttempt += 1
//                coroutineScope.launch {
//                    data.emit(Resource.Loading(message = "Retry ($currentConnectionAttempt)..."))
//                }
//                if (currentConnectionAttempt <= 5) {
//                    startReceiving()
//                } else {
//                    coroutineScope.launch {
//                        data.emit(Resource.Error("No Connection"))
//                    }
//                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                printGattTable()
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Adjusting MTU..."))
                }
                //Maximum no bytes
                gatt.requestMtu(517)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val characteristic = findCharacteristic(SERVICE_UUID, CHAR1_UUID)
            if (characteristic == null) {
                coroutineScope.launch {
                    data.emit(Resource.Error("No Info"))
                }
                return
            }
            coroutineScope.launch {
                data.emit(Resource.Loading(message = "Waiting..."))
            }
            //Set the Datetime with CHAR3_UUID
            coroutineScope.launch {
                setKeyDate(gatt)
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated(
            "Used natively in Android 12 and lower",
            ReplaceWith("onCharacteristicChanged(gatt, characteristic, characteristic.value)")
        )
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) = onCharacteristicChanged(gatt, characteristic, characteristic.value)

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            with(characteristic) {
                when (uuid) {
                    UUID.fromString(CHAR1_UUID) -> {
                        coroutineScope.launch {
                            data.emit(
                                Resource.Success(
                                    data = KeyInfoResult(
                                        keyId = "",
                                        lockId = String(value, Charsets.UTF_8),
                                        battVoltage = 0.0,
                                        keyVersion = "",
                                        date = null,
                                        ConnectionState.Connected
                                    )
                                )
                            )
                        }
                        //Enable the Key to open Lock
                        setKeyEnabled(gatt)
                    }
                    else -> {
                        Log.d("BLEReceiverManager", uuid.toString())
                        Unit
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated(
            "Used natively in Android 12 and lower",
            ReplaceWith("onCharacteristicRead(gatt, characteristic, characteristic.value, status)")
        ) override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) = onCharacteristicRead(gatt, characteristic, characteristic.value, status)

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.uuid == UUID.fromString(CHAR2_UUID)) {
                    //Key Number
                    val valueStr = String(value, Charsets.UTF_8)
                    Log.d("BLEReceiverManager", "Read CHAR2 success: ${valueStr}")

                    val keyNoStr = valueStr.substring(1, 9)
                    val keyNo = keyNoStr.toLong(16)

                    val battVoltStr = valueStr.substring(11, 15)
                    var battVolt = 0.0;
                    if (battVoltStr.isNotEmpty()) {
                        battVolt = battVoltStr.toDouble();
                    }
                    val version = valueStr.substring(16)

                    coroutineScope.launch {
                        data.emit(
                            Resource.Success(
                                data = KeyInfoResult(
                                    keyId = keyNo.toString(),
                                    lockId = "",
                                    battVoltage = battVolt,
                                    keyVersion = version,
                                    date = null,
                                    ConnectionState.Connected
                                )
                            )
                        )
                    }
                }
                if (characteristic.uuid == UUID.fromString(CHAR3_UUID)) {
                    //Key Date
                    coroutineScope.launch {
                        data.emit(
                            Resource.Success(
                                data = KeyInfoResult(
                                    keyId = "",
                                    lockId = "",
                                    battVoltage = 0.0,
                                    keyVersion = "",
                                    date = getKeyDate(value),
                                    ConnectionState.Connected
                                )
                            )
                        )
                    }
                }
                enableNotification()
            } else {
                Log.d("BLEReceiverManager", "Bad Read")
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Bad Read!"))
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLEReceiverManager", "Write Date success")
//                coroutineScope.launch {
//                    data.emit(
//                        Resource.Loading(message = "Key Time set...")
//                    )
//                }
                readKeyNumber(gatt)
            } else {
                Log.d("BLEReceiverManager", "Write Date Failed!!")
            }
        }
    }

    private fun setKeyEnabled(gatt: BluetoothGatt) {
        if (dataRepository.keyEnabled) {

            val char1Service = findCharacteristic(SERVICE_UUID, CHAR1_UUID)
            if (char1Service != null) {
                val data = ByteArray(1)
                data[0] = 1;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        char1Service,
                        data,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                } else {
                    char1Service.setValue(data)
                    gatt.writeCharacteristic(char1Service)
                }
            }
        }
    }

    private fun setKeyDate(gatt: BluetoothGatt) {
        val char3Service = findCharacteristic(SERVICE_UUID, CHAR3_UUID)
        if (char3Service != null) {
            val calendar = Calendar.getInstance()
            val data = ByteArray(6)
            data[0] = (calendar.get(Calendar.YEAR) - 2000).toByte()
            data[1] = calendar.get(Calendar.MONTH).toByte()
            data[2] = calendar.get(Calendar.DAY_OF_MONTH).toByte()
            data[3] = calendar.get(Calendar.HOUR_OF_DAY).toByte()
            data[4] = calendar.get(Calendar.MINUTE).toByte()
            data[5] = calendar.get(Calendar.SECOND).toByte()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    char3Service,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                char3Service.setValue(data)
                gatt.writeCharacteristic(char3Service)
            }
        }
    }

    private fun getKeyDate(data: ByteArray): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, (2000 + data[0]))
        calendar.set(Calendar.MONTH, data[1].toInt())
        calendar.set(Calendar.DAY_OF_MONTH, data[2].toInt())
        calendar.set(Calendar.HOUR_OF_DAY, data[3].toInt())
        calendar.set(Calendar.MINUTE, data[4].toInt())
        calendar.set(Calendar.SECOND, data[5].toInt())
        return calendar.time
    }

    private fun readKeyNumber(gatt: BluetoothGatt) {
        val characteristic = findCharacteristic(SERVICE_UUID, CHAR2_UUID)
        if (characteristic != null) {
            gatt.readCharacteristic(characteristic)
        }
    }

    private fun enableNotification() {
        val characteristic = findCharacteristic(SERVICE_UUID, CHAR1_UUID)
        if (characteristic == null) {
            Log.d("BLEReceiverManager", "Characteristic not found")
            return
        }
        val payLoad = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.d("BLEReceiverManager", "Set Characteristic notification failed")
                return
            }
            writeDescription(cccdDescriptor, payLoad)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payLoad: ByteArray) {
        gatt?.let {gatt ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, payLoad)
            } else {
                descriptor.setValue(payLoad)
                if (gatt.writeDescriptor(descriptor) == false) {
                    Log.d("BLEReceiverManager", "Write descriptor failed")
                } else {
                    Log.d("BLEReceiverManager", "Write descriptor success")
                }
            }?: error("Not connected to a Unilock Key")
        }
    }

    private fun findCharacteristic(serviceUUID: String, characteristicsUUID: String): BluetoothGattCharacteristic? {
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }

    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Scanning..,"))
        }
        isScanning = true
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    override fun disconnect() {
        coroutineScope.launch {
            data.emit(
                Resource.Success(
                    data = KeyInfoResult(
                        keyId = "",
                        lockId = "",
                        battVoltage = 0.0,
                        keyVersion = "",
                        date = null,
                        ConnectionState.Disconnected
                    )
                )
            )
        }
        gatt?.disconnect()
    }

    override fun reconnect() {
        gatt?.connect()
    }

    private fun disconnectCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let {cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.d("BLEReceiverManager", "Set Characteristic notification failed")
                return
            }
            writeDescription(cccdDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        }
    }

    override fun closeConnection() {
        bleScanner.stopScan((scanCallback))
        val characteristic = findCharacteristic(SERVICE_UUID, CHAR1_UUID)
        if (characteristic != null) {
            disconnectCharacteristic(characteristic)
        }
        gatt?.close()
    }
}