package com.df.unilockkey.service

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
                        syncKeys()
                    }
                    is ApiEvent.Keys -> {
                        syncLocks()
                    }

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
                        for (key in result.data) {
                            appDatabase.unikeyDao().insertAll(key)
                        }
                        syncLocks()
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

    private fun syncKeys() {
        scope.launch {
            keyService.getKeys()
        }
    }

    private fun syncLocks() {
        scope.launch {
            lockService.getLocks()
        }
    }

}