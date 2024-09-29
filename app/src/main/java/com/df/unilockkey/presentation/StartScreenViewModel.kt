package com.df.unilockkey.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.df.unilockkey.agent.Authenticate
import com.df.unilockkey.agent.KeyService
import com.df.unilockkey.agent.LockService
import com.df.unilockkey.util.ApiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartScreenViewModel @Inject constructor(
    private val authenticate: Authenticate,
    private val keyService: KeyService,
    private val lockService: LockService,
) : ViewModel() {

    init {
        subscribeToAuthenticate()
        subscribeToKeyService()
        subscribeToLockService()
    }

    private fun subscribeToAuthenticate() {
        viewModelScope.launch {
            authenticate.data.collect{ result ->
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
        viewModelScope.launch {
            keyService.data.collect{ result ->
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

    private fun subscribeToLockService() {
        viewModelScope.launch {
            lockService.data.collect{ result ->
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

    private fun syncKeys() {
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            keyService.getKeys()
        }
    }

    private fun syncLocks() {
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            lockService.getLocks()
        }
    }
}