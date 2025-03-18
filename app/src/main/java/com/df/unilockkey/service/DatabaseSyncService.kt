package com.df.unilockkey.service

import android.content.Context
import android.util.Log
import com.df.unilockkey.agent.ApiService
import com.df.unilockkey.agent.Authenticate
import com.df.unilockkey.agent.KeyService
import com.df.unilockkey.agent.LockService
import com.df.unilockkey.agent.LoginRequest
import com.df.unilockkey.agent.PhoneService
import com.df.unilockkey.agent.RouteService
import com.df.unilockkey.agent.SettingsApiService
import com.df.unilockkey.repository.AppDatabase
import com.df.unilockkey.repository.DebugLog
import com.df.unilockkey.repository.EventLog
import com.df.unilockkey.util.ApiEvent
import com.df.unilockkey.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timerTask


class DatabaseSyncService @Inject constructor(
    private val auth: Authenticate,
    private val keyService: KeyService,
    private val lockService: LockService,
    private val routeService: RouteService,
    private val phoneService: PhoneService,
    private val appDatabase: AppDatabase,
    private val logEventService: EventLogService,
    private val settingsService: SettingsApiService,
    private var api: ApiService,
    @ApplicationContext private val context: Context
) {

    val debugLogs: MutableSharedFlow<Resource<DebugLog>> = MutableSharedFlow()

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private var lockBusy = false
    private var syncSettingBusy = false
    var LoggedIn: Boolean = false
    var phoneId: String? = null

    fun startSync(thisPhoneId: String?) {
        phoneId = thisPhoneId
        lockBusy = false;
        LoggedIn = false;
        syncDatabase()
        val funtimer: Timer = Timer()
        funtimer.schedule(
            timerTask()
            {
                syncEventLogs()
            }, 30000, 30000)
    }

    fun syncDatabase() {
        subscribeToPhoneService()
        subscribeToRouteService()
        subscribeToKeyService()
        subscribeToLockService()
        subscribeToSettingsService()
        syncEventLogs()
    }

    private fun getPhone(phoneId: String?) {
        scope.launch {
            try {
                if (phoneId != null) {
                    phoneService.getPhone(phoneId)
                }
            } catch (err: Exception) {
                NewDebugLog("DatabaseSyncService", err.message.toString())
            }
        }
    }

    private fun syncEventLogs() {
        scope.launch {
            try {
                //NewDebugLog("DatabaseSyncService", "Synchronise Database")
                syncLocks()
                syncSettings();
                logEventService.syncEventLogs()
                getPhone(phoneId)
            } catch (err: Exception) {
                NewDebugLog("DatabaseSyncService", err.message.toString())

            }
        }
    }

    private fun getKeys() {
        scope.launch {
            try {
                keyService.getKeys()
            } catch (err: Exception) {
                NewDebugLog("DatabaseSyncService", err.message.toString())
            }
        }
    }

    fun loginUser(username: String, password: String ) {
        subscribeToAuthenticate()
        scope.launch {
            try {
                auth.login(LoginRequest(username, password))
            } catch (err: Exception) {
                NewDebugLog("DatabaseSyncService", err.message.toString())
            }
        }
    }

    private fun subscribeToAuthenticate() {
        scope.launch {
            auth.data.collect{ result ->
                when(result) {
                    is ApiEvent.LoggedIn -> {
                        LoggedIn = true;
                        syncEventLogs()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun subscribeToKeyService() {
        scope.launch {
            keyService.data.collect{ result ->
                when(result) {
                    is ApiEvent.Keys -> {
                        try {
                            for (key in result.data) {
                                val tmpkey = appDatabase.unikeyDao().findByKeyNumber(key.keyNumber)
                                if (tmpkey == null) {
                                    appDatabase.unikeyDao().insert(key)
                                } else {
                                    appDatabase.unikeyDao().update(key)
                                }
                                settingsService.getSettingsByKey(key.keyNumber)
                            }
                        } catch (err: Exception) {
                            NewDebugLog("DatabaseSyncService", err.message.toString())
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun subscribeToLockService() {
        scope.launch {
            lockService.lock.collect{ result ->
                when(result) {
                    is ApiEvent.Lock -> {
                        try {
                            if(result.data != null) {
                                val lock = result.data
                                val tmpLock = appDatabase.unilockDao().findByLockNumber(lock.lockNumber)
                                if (tmpLock == null) {
                                    appDatabase.unilockDao().insert(lock)
                                } else {
                                    appDatabase.unilockDao().update(lock)
                                }
                            }
                        } catch (err: Exception) {
                            NewDebugLog("DatabaseSyncService", err.message.toString())
                        }
                    }
                    else -> {}
                }
            }
        }
        scope.launch {
            lockService.locks.collect{ result ->
                when(result) {
                    is ApiEvent.Locks -> {
                        try {
                            for (lock in result.data) {
                                val tmpLock = appDatabase.unilockDao().findByLockNumber(lock.lockNumber)
                                if (tmpLock == null) {
                                    appDatabase.unilockDao().insertAll(lock)
                                } else {
                                    appDatabase.unilockDao().update(lock)
                                }
                            }
                        } catch (err: Exception) {
                                NewDebugLog("DatabaseSyncService", err.message.toString())
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun subscribeToRouteService() {
        scope.launch {
            routeService.route.collect { result ->
                when (result) {
                    is ApiEvent.Route -> {
                        try {
                            if (result.data != null) {
                                val route = result.data
                                val tempRoute = appDatabase.routeDao().findById(route.id)
                                if (tempRoute == null) {
                                    appDatabase.routeDao().insert(route)
                                } else {
                                    appDatabase.routeDao().update(route)
                                }
                                if (route.locks != null) {
                                    for (lock in route.locks) {
                                        lockService.getLock(lock.lockNumber)
                                        settingsService.getSettingsByLock(lock.lockNumber)
                                    }
                                }
                            }
                        } catch (err: Exception) {
                            NewDebugLog("DatabaseSyncService", err.message.toString())
                        }
                    }
                    else -> {}
                }
            }
        }

        scope.launch {
            routeService.routes.collect { result ->
                when (result) {
                    is ApiEvent.Routes -> {
                        try {
                            for (route in result.data) {
                                val tempRoute = appDatabase.routeDao().findById(route.id)
                                if (tempRoute == null) {
                                    appDatabase.routeDao().insert(route)
                                } else {
                                    appDatabase.routeDao().update(route)
                                }
                            }
                        } catch (err: Exception) {
                            NewDebugLog("DatabaseSyncService", err.message.toString())
                        }
                    }
                    else -> {}
                }
            }
        }
     }

    private fun subscribeToPhoneService() {
        scope.launch {
            phoneService.data.collect{ result ->
                when(result) {
                    is ApiEvent.Phone -> {
                        try {
                            if (result.data != null) {
                                val phone = result.data
                                val tempPhone = appDatabase.phoneDao().findById(phone.id)
                                if (tempPhone == null) {
                                    appDatabase.phoneDao().insert(phone)
                                } else {
                                    appDatabase.phoneDao().update(phone)
                                }
                                if (phone.routes != null) {
                                    for (route in phone.routes) {
                                        routeService.getRoute(route.id)
                                    }
                                }
                            }
                        } catch (err: Exception) {
                            NewDebugLog("DatabaseSyncService", err.message.toString())
                        }
                        getKeys();
                    }
                    else -> {}
                }
            }
        }
    }

    private fun subscribeToSettingsService() {
        scope.launch {
            settingsService.setting.collect { result ->
                when (result) {
                    is ApiEvent.Settings -> {
                        try {
                            if (result.data != null) {
                                val setting = result.data
                                val tmpSetting = appDatabase.settingsDao().findById(setting.id)
                                if (tmpSetting == null) {
                                    appDatabase.settingsDao().insert(setting)
                                } else {
                                    appDatabase.settingsDao().update(setting)
                                }
                            }
                        } catch (err: Exception) {
                            NewDebugLog("DatabaseSyncService", err.message.toString())
                        }
                    }
                    else -> {}
                }
            }
        }

        scope.launch {
            settingsService.settings.collect { result ->
                when (result) {
                    is ApiEvent.Settings -> {
                        try {
                            for (setting in result.data) {
                                val tmpSetting = appDatabase.settingsDao().findById(setting.id)
                                if (tmpSetting == null) {
                                    appDatabase.settingsDao().insert(setting)
                                } else {
                                    appDatabase.settingsDao().update(setting)
                                }
                            }
                        } catch (err: Exception) {
                            NewDebugLog("DatabaseSyncService", err.message.toString())
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    suspend fun syncLocks() {
        if (!lockBusy) {
            try {
                lockBusy = true
                val locks = appDatabase.unilockDao().getAllByArchive(false)
                for (lock in locks) {
                    lock.archived = true
                    api.putLock(lock.lockNumber, lock)
                    appDatabase.unilockDao().update(lock)
                    NewDebugLog("syncLocks:", "Archive: " +  lock.lockNumber.toString())
                }
            } catch (e: HttpException) {
                val response = e.response()
                val errorCode = e.code()
                if (response != null) {
                    NewDebugLog("syncLocks:", response.message() + ":" + errorCode.toString())
                } else {
                    NewDebugLog("syncLocks:", "ErrorCode: $errorCode")
                }
            } catch (e: Exception) {
                NewDebugLog("syncLocks:", e.message.toString())
                createEvent("syncLocks: " + e.message.toString())
            } finally {
                lockBusy = false
            }
        }
    }

    suspend fun syncSettings() {
        if (!syncSettingBusy) {
            try {
                syncSettingBusy = true
                val settings = appDatabase.settingsDao().getAllByArchive(false, true)
                for (setting in settings) {
                    setting.archived = true
                    api.putSettings(setting.id, setting)
                    appDatabase.settingsDao().update(setting)
                    if (setting.key != null) {
                        NewDebugLog("syncSettings:", "Archive: Key Setting: " + setting.key.keyNumber + "," + setting.id)
                    }
                    if (setting.lock != null) {
                        NewDebugLog("syncSettings:", "Archive: Lock Setting: " + setting.lock.lockNumber + "," + setting.id)
                    }
                }
            } catch (e: HttpException) {
                val response = e.response()
                val errorCode = e.code()
                if (response != null) {
                    NewDebugLog("syncSettings:", response.message() + ":" + errorCode.toString())
                } else {
                    NewDebugLog("syncSettings:", errorCode.toString())
                }
                scope.launch { auth.refreshLogin()}
            } catch (e: UnknownHostException) {
                NewDebugLog("syncSettings:", e.message.toString())
            } catch (e: Exception) {
                NewDebugLog("syncSettings:", e.message.toString())
            } finally {
                syncSettingBusy = false;
            }
        }
    }

    private fun NewDebugLog(tag: String, message: String) {
        Log.d(tag, message)
        //scope.launch {
        //    debugLogs.emit(
        //        Resource.Success(
        //            data = DebugLog(
        //                phoneId = phoneId,
        //                timestamp = System.currentTimeMillis() / 1000,
        //                event = message
        //            )
        //        )
        //    )
        //}
    }

    private suspend fun createEvent(msg: String) {
        val eventLog = EventLog()
        eventLog.phoneId = "log"
        eventLog.event = msg
        eventLog.keyNumber = 0
        eventLog.lockNumber = 0
        eventLog.battery = "0.0"
        logEventService.logEvent(eventLog)
    }

}