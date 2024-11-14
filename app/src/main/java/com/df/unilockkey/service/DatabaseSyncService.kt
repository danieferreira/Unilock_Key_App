package com.df.unilockkey.service

import android.util.Log
import com.df.unilockkey.agent.Authenticate
import com.df.unilockkey.agent.KeyService
import com.df.unilockkey.agent.LockService
import com.df.unilockkey.agent.LoginRequest
import com.df.unilockkey.repository.AppDatabase
import com.df.unilockkey.util.ApiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timerTask

class DatabaseSyncService @Inject constructor(
    private val auth: Authenticate,
    private val keyService: KeyService,
    private val lockService: LockService,
    private val appDatabase: AppDatabase,
    private val logEventService: EventLogService
) {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    var LoggedIn: Boolean = false;

    fun startSync() {
        val funtimer: Timer = Timer()
        funtimer.schedule(
            timerTask()
            {
                syncDatabase()
            }, 100, 30000)
    }

    fun syncDatabase() {
        subscribeToKeyService()
        subscribeToLockService()
        getKeys()
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
                        keyService.getKeys()
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
                        lockService.getLocks()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun subscribeToLockService() {
        scope.launch {
            lockService.data.collect{ result ->
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
                        logEventService.syncEventLogs()
                    }
                    else -> {}
                }
            }
        }
    }
}