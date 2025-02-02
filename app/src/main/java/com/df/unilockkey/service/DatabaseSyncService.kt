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
import com.df.unilockkey.repository.AppDatabase
import com.df.unilockkey.repository.EventLog
import com.df.unilockkey.util.ApiEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
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
    private var api: ApiService,
    @ApplicationContext private val context: Context
) {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private var lockBusy = false;
    var LoggedIn: Boolean = false;
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
                getPhone(phoneId)
            }, 30000, 30000)
    }

    fun syncDatabase() {
        subscribeToPhoneService()
        subscribeToRouteService()
        subscribeToKeyService()
        subscribeToLockService()
        getPhone(phoneId)
    }

    private fun getPhone(phoneId: String?) {
        scope.launch {
            try {
                if (phoneId != null) {
                    phoneService.getPhone(phoneId)
                }
            } catch (err: Exception) {
                Log.d("DatabaseSyncService", err.message.toString())
            }
        }
    }

    private fun syncEventLogs() {
        scope.launch {
            try {
                syncLocks()
                logEventService.syncEventLogs()
            } catch (err: Exception) {
                Log.d("DatabaseSyncService", err.message.toString())
            }
        }
    }

    private fun getKeys() {
        scope.launch {
            try {
                keyService.getKeys()
            } catch (err: Exception) {
                Log.d("DatabaseSyncService", err.message.toString())
            }
        }
    }

    fun loginUser(username: String, password: String ) {
        subscribeToAuthenticate()
        scope.launch {
            try {
                auth.login(LoginRequest(username, password))
            } catch (err: Exception) {
                Log.d("DatabaseSyncService", err.message.toString())
            }
        }
    }

    private fun subscribeToAuthenticate() {
        scope.launch {
            auth.data.collect{ result ->
                when(result) {
                    is ApiEvent.LoggedIn -> {
                        LoggedIn = true;
                        getPhone(phoneId)
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
                            }
                        } catch (err: Exception) {
                            Log.d("DatabaseSyncService", err.message.toString())
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
                            Log.d("DatabaseSyncService", err.message.toString())
                        }
                        syncEventLogs()
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
                                Log.d("DatabaseSyncService", err.message.toString())
                        }
                        syncEventLogs()
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
                                    }
                                }
                            }
                        } catch (err: Exception) {
                            Log.d("DatabaseSyncService", err.message.toString())
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
                            Log.d("DatabaseSyncService", err.message.toString())
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
                            Log.d("DatabaseSyncService", err.message.toString())
                        }
                        getKeys();
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
                    Log.d("syncLocks:", "Archive: " +  lock.lockNumber.toString())
                }
            } catch (e: HttpException) {
                val response = e.response()
                val errorCode = e.code()
                if (response != null) {
                    Log.d("syncLocks:", response.message() + ":" + errorCode.toString())
                } else {
                    Log.d("syncLocks:", "ErrorCode: $errorCode")
                }
            } catch (e: Exception) {
                Log.d("syncLocks:", e.message.toString())
                createEvent("syncLocks: " + e.message.toString())
            } finally {
                lockBusy = false
            }
        }
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