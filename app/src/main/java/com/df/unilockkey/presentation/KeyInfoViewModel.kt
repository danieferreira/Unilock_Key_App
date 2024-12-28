package com.df.unilockkey.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.df.unilockkey.agent.PhoneService
import com.df.unilockkey.data.ConnectionState
import com.df.unilockkey.data.KeyReceiverManager
import com.df.unilockkey.repository.AppDatabase
import com.df.unilockkey.repository.DataRepository
import com.df.unilockkey.repository.EventLog
import com.df.unilockkey.service.EventLogService
import com.df.unilockkey.util.ApiEvent
import com.df.unilockkey.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyInfoViewModel @Inject constructor(
    private val keyReceiverManager: KeyReceiverManager,
    private val dataRepository: DataRepository,
    private val appDatabase: AppDatabase,
    private val eventLogService: EventLogService,
    private val phoneService: PhoneService
): ViewModel() {

    var initialisingMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var keyId by mutableStateOf<String>("")
    var lockId by mutableStateOf<String>("")
    var routeNames by mutableStateOf<String>("")
    var battVoltage by mutableStateOf<String>("")
    var keyVersion by mutableStateOf<String>("")
    var keyValid by mutableStateOf<String>("")
    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Unitialised)
    private var eventLog: EventLog = EventLog()

    private fun subscribeToChanged() {
        viewModelScope.launch {
            keyReceiverManager.data.collect{ result ->
                when(result) {
                    is Resource.Success -> {
                        try {
                            connectionState = result.data.connectionState
                            if (result.data.keyId != "") {
                                keyId = result.data.keyId
                            }
                            if (result.data.battVoltage > 1.0) {
                                battVoltage = "%.${2}f".format(result.data.battVoltage)
                            }
                            if (result.data.lockId != "") {
                                lockId = result.data.lockId
                                setKeyEnabled(false)
                                keyValid = "Blocked"
                                try {
                                    val key = appDatabase.unikeyDao().findByKeyNumber(keyId.toInt())
                                    if (key == null) {
                                        keyValid = "Key not found"
                                    } else {
                                        if (!key.enabled) {
                                            keyValid = "Key Blocked"
                                        } else {
                                            //TODO: Check the start and end times if requires
                                            if (key.timeLimitEnabled) {

                                            }
                                            val locks = appDatabase.unilockDao().getAll()
                                            for (tmp in locks) {
                                                Log.d("Lock", tmp.lockNumber.toString() + "")
                                            }
                                            val lock = appDatabase.unilockDao().findByLockNumber(lockId.toInt())
                                            if (lock == null) {
                                                keyValid = "Lock not found"
                                            } else {
                                                //TODO: Check Start and End Date

                                                //TODO: Check if the lock is on this route
                                                if (lock.route != null) {
                                                    val route = appDatabase.routeDao().findById(lock.route.id)
                                                    if (route == null) {
                                                        keyValid = "Route not found"
                                                    } else {
                                                        for (tmpLock in route.locks) {
                                                            if (tmpLock.lockNumber == lock.lockNumber) {
                                                                setKeyEnabled(true)
                                                                keyValid = "Allowed"
                                                                break
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    setKeyEnabled(true)
                                                    keyValid = "Allowed"
                                                }
                                            }
                                        }
                                    }
                                    //Log this event
                                    if ((!eventLog.event.equals(keyValid)) ||
                                        (eventLog.lockNumber != lockId.toInt()) ||
                                        eventLog.keyNumber != keyId.toInt()) {

                                        eventLog.event = keyValid
                                        eventLog.keyNumber = keyId.toInt()
                                        eventLog.lockNumber = lockId.toInt()
                                        eventLog.battery = battVoltage.replace(',','.')

                                        eventLogService.logEvent(eventLog)
                                    }
                                } catch (err: Exception) {
                                    Log.d("KeyInfoViewModel", err.message.toString())
                                }
                            } else {
                                if (eventLog.lockNumber != lockId.toInt()) {
                                    eventLog.event = "Connected"
                                    eventLog.keyNumber = keyId.toInt()
                                    eventLog.lockNumber = 0
                                    eventLog.battery = battVoltage.replace(',','.')

                                    eventLogService.logEvent(eventLog)
                                }
                            }
                            if (result.data.keyVersion != "") {
                                keyVersion = result.data.keyVersion
                            }
                        } catch (err: Exception) {
                            Log.d("KeyInfoViewModel", err.message.toString())
                        }
                    }
                    is Resource.Loading -> {
                        connectionState = ConnectionState.CurrentlyInitialising
                        initialisingMessage = result.message
                        keyId = ""
                        lockId = ""
                        battVoltage = ""
                        keyVersion = ""
                        keyValid = ""
                        setKeyEnabled(false)
                    }
                    is Resource.Error -> {
                        connectionState = ConnectionState.Unitialised
                        errorMessage = result.errorMessage
                    }
                }
            }
        }
    }

    private fun subscribeToPhoneService() {
        viewModelScope.launch {
            phoneService.data.collect{ result ->
                when(result) {
                    is ApiEvent.Phone -> {
                        try {
                            if (result.data != null) {
                                val phone = result.data
                                routeNames = "";
                                for (route in phone.routes)
                                {
                                    routeNames += route.name + "\r\n"
                                }
                            }
                        } catch (err: Exception) {
                            errorMessage = err.message.toString()
                            Log.d("KeyInfoViewModel", err.message.toString())
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun disconnect() {
        keyReceiverManager.disconnect()
    }

    fun reconnect() {
        keyReceiverManager.reconnect()
    }

    fun initialiseConnection() {
        errorMessage = null
        subscribeToChanged()
        subscribeToPhoneService()
        keyReceiverManager.startReceiving()
    }

    fun setKeyEnabled(enabled: Boolean) {
        dataRepository.keyEnabled = enabled
    }

    override fun onCleared() {
        super.onCleared()
        keyReceiverManager.closeConnection()

    }
}