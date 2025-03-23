package com.df.unilockkey.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.df.unilockkey.agent.PhoneService
import com.df.unilockkey.agent.SettingsApiService
import com.df.unilockkey.data.ConnectionState
import com.df.unilockkey.data.KeyReceiverManager
import com.df.unilockkey.repository.AppDatabase
import com.df.unilockkey.repository.DataRepository
import com.df.unilockkey.repository.EventLog
import com.df.unilockkey.repository.Phone
import com.df.unilockkey.repository.Unikey
import com.df.unilockkey.repository.Unilock
import com.df.unilockkey.service.DatabaseSyncService
import com.df.unilockkey.service.EventLogService
import com.df.unilockkey.service.SettingsService
import com.df.unilockkey.util.ApiEvent
import com.df.unilockkey.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val syncDatabase: DatabaseSyncService,
    private val settingsService: SettingsService,
    private val settingsApiService: SettingsApiService
): ViewModel() {
    var debugLog = MutableStateFlow<List<String>>(mutableListOf())
    var initialisingMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var keyId by mutableStateOf<String>("")
    var lockId by mutableStateOf<String>("")
    var routeNames by mutableStateOf<String>("")
    var battVoltage by mutableStateOf<String>("")
    var keyVersion by mutableStateOf<String>("")
    var keyValid by mutableStateOf<String>("")
    var keyStatus by mutableStateOf<String>("")
    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Unitialised)
    private var eventLog: EventLog = EventLog()
    private var event: String = ""
    private var currentPhone: Phone? = null
    private var lockCount = 0
    private var currentLock = 0

    private fun checkKeyLimited(key: Unikey): Boolean {
        if (key.timeLimitEnabled) {
            if ((key.startTime != null) && (key.endTime != null)) {
                val sdf: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val date = LocalDateTime.now()
                val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val startDate = LocalDateTime.parse(key.startTime, sdf)
                val endDate = LocalDateTime.parse(key.endTime, sdf)
                if ((startDate != null) && (endDate != null)) {
                    val startTime = startDate.toLocalTime()
                    val endTime = endDate.toLocalTime()
                    val timeNow = date.toLocalTime()
                    if ((timeNow.isBefore(startTime)) || (timeNow.isAfter(endTime))) {
                        this.event = "Key Time Limited, ${timeFormatter.format(startTime)} - ${timeFormatter.format(endTime)}"
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun checkForcedConnection(): Boolean {
        if (currentPhone != null) {
            if (currentPhone!!.forceConnection) {
                if (lockId.toInt() != currentLock) {
                    currentLock = lockId.toInt()
                    lockCount++
                }
                if (lockCount > currentPhone!!.numberLocks) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkLockDate(lock: Unilock): Boolean {
        //Check Start and End Date

        if ((lock.startDate != null) && (lock.endDate != null)) {
            val sdf: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val date = LocalDateTime.now()
            val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            var startDate = LocalDateTime.parse(lock.startDate, sdf)
            var endDate = LocalDateTime.parse(lock.endDate, sdf)
            if ((startDate != null) && (endDate != null)) {

                startDate = startDate.plus(2, ChronoUnit.HOURS)
                endDate = endDate.plus(2, ChronoUnit.HOURS)
                endDate = endDate.plus(1, ChronoUnit.DAYS)
                startDate = startDate.truncatedTo(ChronoUnit.DAYS)
                endDate = endDate.truncatedTo(ChronoUnit.DAYS)
                if ((date.isAfter(startDate)) && (date.isBefore(endDate))) {
                    return true
                } else {
                    event = "Lock Expired, ${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}"
                }
            }
        }
        return false
    }

    private fun checkValidRoute(lock: Unilock): Boolean {
        if (lock.route != null) {
            val route = appDatabase.routeDao().findById(lock.route.id)
            if (route == null) {
                keyValid = "Route not found"
                event = "Route not found, Route ID: ${lock.route.id} "
            } else {
                //Check if route is on this phone
                if (currentPhone == null) {
                    keyValid = "Phone not found"
                    event = "Phone not found"
                } else {
                    var validRoute = false
                    if (currentPhone!!.routes != null) {
                        for (tmpRoute in currentPhone!!.routes!!) {
                            if (tmpRoute.id == route.id) {
                                //Check if the lock is on this route
                                if (route.locks != null) {
                                    for (tmpLock in route.locks) {
                                        if (tmpLock.lockNumber == lock.lockNumber) {
                                            return true
                                        }
                                    }
                                }
                            }
                        }
                    }
                    event = "${route.name} Route Invalid"
                }
            }
        }
        return false
    }

    private fun checkKeyExpired(lock: Unilock, key: Unikey): Boolean {
        //Check the Key Duration if not zero
        val sdf: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val date = LocalDateTime.now()
        if ((lock.duration != null) && (lock.duration != 0)) {
            var timeLeft = 0L
            if ((lock.activatedDate == null) || (lock.activeKey == null) || (lock.activeKey!!.keyNumber != key.keyNumber)) {
                lock.activatedDate = sdf.format(LocalDateTime.now())
                lock.activeKey = key
                lock.archived = false
                appDatabase.unilockDao().update(lock)
            } else {
                if ((lock.activatedDate != null) && (lock.activeKey != null)) {
                    val activated = LocalDateTime.parse(lock.activatedDate, sdf)
                    if (lock.activeKey!!.keyNumber == key.keyNumber) {
                        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        timeLeft = ChronoUnit.MINUTES.between(activated, date)
                        if (timeLeft >= lock.duration) {
                            event = "Key Expired, ${dateFormatter.format(activated)} - ${lock.duration} minutes"
                            return true
                        }
                    }
                }
            }
            keyValid = "Allowed (" + (lock.duration - timeLeft) + " min)"
            event = "Allowed (" + (lock.duration - timeLeft) + " min)"
        } else {
            keyValid = "Allowed"
            event = "Allowed"
        }
        return false
    }

    private fun CheckKeySettings(keyNumber: Long) {
        //Check if there are new settings for this key
        viewModelScope.launch {
            val setting = settingsService.findKeySetting(keyNumber)
            if (setting != null) {
                Log.d("KeyInfoViewModel", "Sending Key Settings: " +  keyNumber.toString() +"," + setting.id)
                debugLog.value += "Sending Key Settings: " +  keyNumber.toString() +"," + setting.id
                while (keyReceiverManager.isBusy.get()) {
                    delay(100)
                }
                keyReceiverManager.sendKeySettings(setting)
                keyValid = "Configuration"
                event = "Configuration"
            }
        }
    }

    private fun checkLockSettings(lockNumber: Long) {
        //Check if there are new settings for this key
        viewModelScope.launch {
            val setting = settingsService.findLockSetting(lockNumber)
            if (setting != null) {
                Log.d("KeyInfoViewModel", "Sending Lock Settings: " +  lockNumber.toString() +"," + setting.id)
                debugLog.value += "Sending Lock Settings: " +  lockNumber.toString() +"," + setting.id
                while (keyReceiverManager.isBusy.get()) {
                    delay(100)
                }
                keyReceiverManager.sendLockSettings(setting)
                keyValid = "Configuration"
                event = "Configuration"
            }
        }
    }

    private fun subscribeToDebugLog1() {
        viewModelScope.launch {
            syncDatabase.debugLogs.collect{ result ->
                when(result) {
                    is Resource.Success -> {
                        debugLog.value += (result.data.event)
                    }
                    is Resource.Loading -> {
                    }
                    is Resource.Error -> {
                        debugLog.value += (result.errorMessage)
                    }
                }
            }
        }
    }

    private fun subscribeToDebugLog2() {
        viewModelScope.launch {
            settingsApiService.debugLogs.collect{ result ->
                when(result) {
                    is Resource.Success -> {
                        debugLog.value += (result.data.event)
                    }
                    is Resource.Loading -> {
                    }
                    is Resource.Error -> {
                        debugLog.value += (result.errorMessage)
                    }
                }
            }
        }
    }

    private fun subscribeToDebugLog3() {
        viewModelScope.launch {
            keyReceiverManager.debugLogs.collect{ result ->
                when(result) {
                    is Resource.Success -> {
                        debugLog.value += (result.data.event)
                    }
                    is Resource.Loading -> {
                    }
                    is Resource.Error -> {
                        debugLog.value += (result.errorMessage)
                    }
                }
            }
        }
    }

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
                            val keyStatus = result.data.lockStatus
                            val sdf: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            val date = LocalDateTime.now()

                            CheckKeySettings(keyId.toLong())

                            if (result.data.lockId != "") {
                                if (lockId != result.data.lockId) {
                                    lockId = result.data.lockId
                                    checkLockSettings(lockId.toLong())

                                    setKeyEnabled(false)
                                    try {
                                        val key = appDatabase.unikeyDao().findByKeyNumber(keyId.toInt())
                                        if (key == null) {
                                            keyValid = "Key not found"
                                            event = "Key not found"
                                        } else {
                                            if (!key.enabled) {
                                                keyValid = "Key Blocked"
                                                event = "Key Blocked, Key not Enabled"
                                            } else {
                                                //Check the start and end times if requires
                                                var keyLimited = false

                                                if (checkKeyLimited(key)) {
                                                    keyValid = "Key Time Limited"
                                                } else {
                                                    if (checkForcedConnection()) {
                                                        keyValid = "No Connection"
                                                        event = "No Connection after $lockCount locks accessed"
                                                    } else {
                                                        val lock = appDatabase.unilockDao().findByLockNumber(lockId.toInt())
                                                        if (lock == null) {
                                                            keyValid = "Lock not found"
                                                            event = "Lock not found"
                                                        } else {
                                                            if (!checkLockDate(lock)) {
                                                                keyValid = "Lock Expired"
                                                            } else {
                                                                //Check the route
                                                                if (!checkValidRoute(lock)) {
                                                                    keyValid = "Route Invalid"
                                                                } else {
                                                                    //Check if it is sending a setting or not
                                                                    if (checkKeyExpired(lock, key)) {
                                                                        keyValid = "Key Expired"
                                                                    } else {
                                                                        while (keyReceiverManager.isBusy.get()) {
                                                                            delay(100)
                                                                        }
                                                                        setKeyEnabled(true)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (err: Exception) {
                                        Log.d("KeyInfoViewModel", err.message.toString())
                                    }
                                }
                                //Log this event
                                if ((!eventLog.event.equals(event)) ||
                                    (eventLog.lockNumber != lockId.toInt()) ||
                                    eventLog.keyNumber != keyId.toInt() ||
                                    eventLog.status != keyStatus
                                ) {
                                    eventLog.phoneId = currentPhone!!.id
                                    eventLog.event = event
                                    eventLog.keyNumber = keyId.toInt()
                                    eventLog.lockNumber = lockId.toInt()
                                    eventLog.status = keyStatus
                                    eventLog.battery = battVoltage.replace(',', '.')
                                    Log.d("KeyInfoViewModel", "$event - Status: $keyStatus")
                                    eventLogService.logEvent(eventLog)
                                }
                                viewModelScope.launch {
                                    eventLogService.syncEventLogs()
                                    syncDatabase.syncLocks()
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
                        event = ""
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
                                    if (currentPhone!!.routes != null) {
                                        for (route in currentPhone!!.routes!!) {
                                            routeNames += route.name + "\r\n"
                                        }
                                    }
                                }
                                //Reset Lock Count
                                lockCount = 0
                                currentLock = 0
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
        subscribeToDebugLog1()
        subscribeToDebugLog2()
        subscribeToDebugLog3()
        keyReceiverManager.startReceiving()
    }

    fun setKeyEnabled(enabled: Boolean) {
        dataRepository.keyEnabled = enabled
        if (enabled) {
            keyReceiverManager.sendKeyEnabled()
        }
    }

    override fun onCleared() {
        super.onCleared()
        keyReceiverManager.closeConnection()

    }

    fun clearDebugLogs() {
        debugLog.value = mutableListOf()
    }

    private suspend fun createEvent(msg: String) {
        val eventLog = EventLog()
        eventLog.phoneId = "log"
        eventLog.event = msg
        eventLog.keyNumber = 0
        eventLog.lockNumber = 0
        eventLog.battery = "0.0"

        eventLogService.logEvent(eventLog)
    }
}