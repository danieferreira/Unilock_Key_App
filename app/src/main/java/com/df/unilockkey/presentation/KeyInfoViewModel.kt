package com.df.unilockkey.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.df.unilockkey.data.ConnectionState
import com.df.unilockkey.data.KeyReceiverManager
import com.df.unilockkey.repository.AppDatabase
import com.df.unilockkey.repository.DataRepository
import com.df.unilockkey.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyInfoViewModel @Inject constructor(
    private val keyReceiverManager: KeyReceiverManager,
    private val dataRepository: DataRepository,
    private val appDatabase: AppDatabase
): ViewModel() {

    var initialisingMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var keyId by mutableStateOf<String>("")
    var lockId by mutableStateOf<String>("")
    var battVoltage by mutableStateOf<String>("")
    var keyVersion by mutableStateOf<String>("")
    var keyValid by mutableStateOf<String>("")
    var checked by mutableStateOf(false)
    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Unitialised)

    private fun subscribeToChanged() {
        viewModelScope.launch {
            keyReceiverManager.data.collect{ result ->
                when(result) {
                    is Resource.Success -> {
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
                                if (key != null) {
                                    for (lock in key.locks) {
                                        if (lock.lockNumber == lockId.toInt()) {
                                            setKeyEnabled(true)
                                            keyValid = "Allowed"
                                            break
                                        }
                                    }
                                }
                            } catch (err: Exception){
                                Log.d("KeyInfoViewModel", err.message.toString())
                            }
                        }
                        if (result.data.keyVersion != "") {
                            keyVersion = result.data.keyVersion
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

    fun disconnect() {
        keyReceiverManager.disconnect()
    }

    fun reconnect() {
        keyReceiverManager.reconnect()
    }

    fun initialiseConnection() {
        errorMessage = null
        subscribeToChanged()
        keyReceiverManager.startReceiving()
    }

    fun setKeyEnabled(enabled: Boolean) {
        dataRepository.keyEnabled = enabled
        checked = enabled
    }

    override fun onCleared() {
        super.onCleared()
        keyReceiverManager.closeConnection()

    }
}