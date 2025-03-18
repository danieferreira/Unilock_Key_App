package com.df.unilockkey.data

import com.df.unilockkey.repository.DebugLog
import com.df.unilockkey.repository.Settings
import com.df.unilockkey.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

interface KeyReceiverManager {
    val isBusy: Boolean
    val data: MutableSharedFlow<Resource<KeyInfoResult>>
    val debugLogs: MutableSharedFlow<Resource<DebugLog>>

    fun reconnect()
    fun disconnect()
    fun startReceiving()
    fun closeConnection()
    fun sendKeySettings(setting: Settings)
    fun sendLockSettings(setting: Settings)
    fun sendKeyEnabled()
    fun sendKeyDate()

}