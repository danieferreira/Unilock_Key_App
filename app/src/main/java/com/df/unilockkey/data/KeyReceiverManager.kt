package com.df.unilockkey.data

import com.df.unilockkey.repository.DebugLog
import com.df.unilockkey.repository.Settings
import com.df.unilockkey.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

interface KeyReceiverManager {
    val isBusy: AtomicBoolean
    val data: MutableSharedFlow<Resource<KeyInfoResult>>
    val debugLogs: MutableSharedFlow<Resource<DebugLog>>

    fun reconnect()
    fun disconnect()
    fun startReceiving()
    fun closeConnection()
    fun sendKeySettings(setting: Settings): Boolean
    fun sendLockSettings(setting: Settings): Boolean
    fun sendKeyEnabled()
    fun sendKeyDate(): Boolean

}