package com.df.unilockkey.data

import com.df.unilockkey.repository.Settings
import com.df.unilockkey.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

interface KeyReceiverManager {
    val data: MutableSharedFlow<Resource<KeyInfoResult>>

    fun reconnect()
    fun disconnect()
    fun startReceiving()
    fun closeConnection()
    fun sendSettings(setting: Settings)

}