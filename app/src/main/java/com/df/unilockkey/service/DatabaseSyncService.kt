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
import javax.inject.Inject

class DatabaseSyncService @Inject constructor(
    private val auth: Authenticate,
    private val keyService: KeyService,
    private val lockService: LockService,
    private val appDatabase: AppDatabase
) {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    fun SyncDatabase() {
        subscribeToAuthenticate()
        subscribeToKeyService()
        subscribeToLockService()

        loginUser(auth);
    }

    private fun loginUser(auth: Authenticate) {

        scope.launch {
            auth.login(LoginRequest("Danie", "1234"))
        }
    }

    private fun subscribeToAuthenticate() {
        scope.launch {
            auth.data.collect{ result ->
                when(result) {
                    is ApiEvent.LoggedIn -> {
                        keyService.getKeys()
                    }
                    is ApiEvent.Keys -> { }

                    is ApiEvent.Locks -> { }
                }
            }
        }
    }

    private fun subscribeToKeyService() {
        scope.launch {
            keyService.data.collect{ result ->
                when(result) {
                    is ApiEvent.LoggedIn -> { }
                    is ApiEvent.Keys -> {
                            try {
                                for (key in result.data) {
                                    appDatabase.unikeyDao().insert(key)
                                }
                                lockService.getLocks()
                            } catch (err: Exception) {
                                Log.d("DatabaseSyncService", err.message.toString())
                            }
                    }
                    is ApiEvent.Locks -> { }
                }
            }
        }
    }

    private fun subscribeToLockService() {
        scope.launch {
            lockService.data.collect{ result ->
                when(result) {
                    is ApiEvent.LoggedIn -> { }
                    is ApiEvent.Keys -> { }
                    is ApiEvent.Locks -> {
                        for (lock in result.data) {
                            appDatabase.unilockDao().insertAll(lock)
                        }
                    }
                }
            }
        }
    }
}