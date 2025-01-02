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
import com.df.unilockkey.repository.Phone
import com.df.unilockkey.service.DatabaseSyncService
import com.df.unilockkey.service.EventLogService
import com.df.unilockkey.util.ApiEvent
import com.df.unilockkey.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class KeyInfoViewModel @Inject constructor(
    private val keyReceiverManager: KeyReceiverManager,
    private val dataRepository: DataRepository,
    private val appDatabase: AppDatabase,
    private val eventLogService: EventLogService,
    private val phoneService: PhoneService,
    private val syncDatabase: DatabaseSyncService
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
    private var currentPhone: Phone? = null

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
                            if (result.data.keyVersion != "") {
                                keyVersion = result.data.keyVersion
                            }
                            val sdf: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            val date = LocalDateTime.now()

                            if (result.data.lockId != "") {
                                lockId = result.data.lockId
                                setKeyEnabled(false)
                                try {
                                    val key = appDatabase.unikeyDao().findByKeyNumber(keyId.toInt())
                                    if (key == null) {
                                        keyValid = "Key not found"
                                    } else {
                                        if (!key.enabled) {
                                            keyValid = "Key Blocked"
                                        } else {
                                            //Check the start and end times if requires
                                            var keyLimited = false
                                            if (key.timeLimitEnabled) {
                                                if ((key.startTime != null) && (key.endTime != null)) {
                                                    val startDate = LocalDateTime.parse(key.startTime, sdf)
                                                    val endDate = LocalDateTime.parse(key.endTime, sdf)
                                                    if ((startDate != null) && (endDate != null)) {
                                                        val startTime = startDate.plus(2, ChronoUnit.HOURS).toLocalTime()
                                                        val endTime = endDate.plus(2, ChronoUnit.HOURS).toLocalTime()
                                                        val timeNow = date.toLocalTime()
                                                        if ((timeNow.isBefore(startTime)) || (timeNow.isAfter(endTime))) {
                                                            keyLimited = true
                                                        }
                                                    }
                                                }
                                            }
                                            if (keyLimited) {
                                                keyValid = "Key Time Limited"
                                            } else {
                                                val lock = appDatabase.unilockDao().findByLockNumber(lockId.toInt())
                                                if (lock == null) {
                                                    keyValid = "Lock not found"
                                                } else {
                                                    //Check Start and End Date
                                                    var validDate = false

                                                    if ((lock.startDate != null) && (lock.endDate != null)) {
                                                        var startDate = LocalDateTime.parse(lock.startDate, sdf)
                                                        var endDate = LocalDateTime.parse(lock.endDate, sdf)
                                                        if ((startDate != null) && (endDate != null)) {
                                                            startDate = startDate.plus(2, ChronoUnit.HOURS)
                                                            endDate = endDate.plus(2, ChronoUnit.HOURS)
                                                            endDate = endDate.plus(1, ChronoUnit.DAYS)
                                                            startDate = startDate.truncatedTo(ChronoUnit.DAYS)
                                                            endDate = endDate.truncatedTo(ChronoUnit.DAYS)
                                                            if (date.isAfter(startDate)) {
                                                                if (date.isBefore(endDate)) {
                                                                    validDate = true
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (!validDate) {
                                                        keyValid = "Lock Expired"
                                                    } else {
                                                        //Check the route
                                                        if (lock.route != null) {
                                                            val route = appDatabase.routeDao().findById(lock.route.id)
                                                            if (route == null) {
                                                                keyValid = "Route not found"
                                                            } else {
                                                                //Check if route is on this phone
                                                                if (currentPhone == null) {
                                                                    keyValid = "Phone not found"
                                                                } else {
                                                                    var validRoute = false
                                                                    for (tmpRoute in currentPhone!!.routes) {
                                                                        if (tmpRoute.id == route.id) {
                                                                            //Check if the lock is on this route
                                                                            for (tmpLock in route.locks) {
                                                                                if (tmpLock.lockNumber == lock.lockNumber) {
                                                                                    validRoute = true
                                                                                    //Check the Key Duration if not zero
                                                                                    var keyExpired = false
                                                                                    if ((lock.duration != null) && (lock.duration != 0)) {
                                                                                        if ((lock.activatedDate == null) || (lock.activeKey == null) || (lock.activeKey!!.keyNumber != key.keyNumber)) {
                                                                                            lock.activatedDate = sdf.format(LocalDateTime.now())
                                                                                            lock.activeKey = key
                                                                                            lock.archived = false
                                                                                            appDatabase.unilockDao().update(lock)
                                                                                            syncDatabase.syncLocks()
                                                                                        } else {
                                                                                            val activated = LocalDateTime.parse(lock.activatedDate, sdf)
                                                                                            if ((lock.activatedDate != null) && (lock.activeKey != null)) {
                                                                                                if (lock.activeKey!!.keyNumber == key.keyNumber) {
                                                                                                    val timeLeft = ChronoUnit.MINUTES.between(activated, date)
                                                                                                    if (timeLeft >= lock.duration) {
                                                                                                        keyExpired = true
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    if (keyExpired) {
                                                                                        keyValid = "Key Expired"
                                                                                    } else {
                                                                                        setKeyEnabled(true)
                                                                                        keyValid = "Allowed"
                                                                                    }
                                                                                    break
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    if (!validRoute) {
                                                                        keyValid = "Route Invalid"
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
                                        }
                                    }
                                    //Log this event
                                    if ((!eventLog.event.equals(keyValid)) ||
                                        (eventLog.lockNumber != lockId.toInt()) ||
                                        eventLog.keyNumber != keyId.toInt()) {

                                        eventLog.phoneId = currentPhone!!.id
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
                                if (eventLog.keyNumber != keyId.toInt()) {
                                    //Key without a lock
                                    eventLog.phoneId = currentPhone!!.id
                                    eventLog.event = "Connected"
                                    eventLog.keyNumber = keyId.toInt()
                                    eventLog.lockNumber = 0
                                    eventLog.battery = battVoltage.replace(',', '.')

                                    eventLogService.logEvent(eventLog)
                                }
                            }

                        } catch (err: Exception) {
                            Log.d("KeyInfoViewModel", err.message.toString())
//                            eventLog.event = err.message.toString()
//                            eventLogService.logEvent(eventLog)
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
                        eventLog = EventLog()
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
                                currentPhone = result.data
                                routeNames = ""
                                if (currentPhone != null) {
                                    for (route in currentPhone!!.routes) {
                                        routeNames += route.name + "\r\n"
                                    }
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