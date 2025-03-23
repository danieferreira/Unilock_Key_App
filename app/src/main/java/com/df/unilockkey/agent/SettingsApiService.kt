package com.df.unilockkey.agent

import android.util.Log
import com.df.unilockkey.repository.DebugLog
import com.df.unilockkey.repository.Settings
import com.df.unilockkey.util.ApiEvent
import com.df.unilockkey.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timerTask

class SettingsApiService(private var api: ApiService) {
    private val coroutineScope= CoroutineScope(Dispatchers.Default)
    val settings: MutableSharedFlow<ApiEvent<Array<Settings>>> = MutableSharedFlow()
    val setting: MutableSharedFlow<ApiEvent<Settings>> = MutableSharedFlow()
    val debugLogs: MutableSharedFlow<Resource<DebugLog>> = MutableSharedFlow()

    private var isBusy = AtomicBoolean(false)
    private var timeoutTimer: Timer = Timer()

    suspend fun getSettings() {
        try {
            while (isBusy.get()) {
                delay(100)
            }
            setBusy(true);
            val response = api.getSettings()
            if (response.isSuccessful) {
                val body = response.body()
                for (setting in body!!) {
                    if (setting.lock != null) {
                        newDebugLog("Settings:", "Settings for Lock: " + setting.lock.lockNumber+ ", " + setting.id)
                    } else if (setting.key != null) {
                        newDebugLog("Settings:", "Settings for Key: " + setting.key.keyNumber + ", " + setting.id)
                    }
                }
                coroutineScope.launch {
                    settings.emit(
                        ApiEvent.Settings(data = body)
                    )
                }
            } else {
                val code = response.code()
                newDebugLog("Settings:", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                newDebugLog("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                newDebugLog("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            newDebugLog("Settings:", e.message.toString())
        } catch (e: Exception) {
            newDebugLog("Settings:", e.message.toString())
        } finally {
            setBusy(false)
        }
    }

    suspend fun getSettingsByLock(lockNumber: Int) {
        try {
            while (isBusy.get()) {
                delay(100)
            }
            setBusy(true)
            val response = api.getSettingsByLock(lockNumber)
            if (response.isSuccessful) {
                val body = response.body()
                for (setting in body!!) {
                    if (setting.lock != null) {
                        newDebugLog("Settings:", "Settings for Lock:" + setting.lock.lockNumber + ", " + setting.id)
                    } else if (setting.key != null) {
                        newDebugLog("Settings:", "Settings for Key:" + setting.key.keyNumber + ", " + setting.id)
                    }
                }
                coroutineScope.launch {
                    settings.emit(
                        ApiEvent.Settings(data = body)
                    )
                }
            } else {
                val code = response.code()
                newDebugLog("Settings:", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                newDebugLog("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                newDebugLog("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            newDebugLog("Settings:", e.message.toString())
        } catch (e: Exception) {
            newDebugLog("Settings:", e.message.toString())
        } finally {
            setBusy(false)
        }
    }

    suspend fun getSettingsByKey(keyNumber: Int) {
        try {
            while (isBusy.get()) {
                delay(100)
            }
            setBusy(true)
            val response = api.getSettingsByKey(keyNumber)
            if (response.isSuccessful) {
                val body = response.body()
                for (setting in body!!) {
                    if (setting.lock != null) {
                        newDebugLog("Settings:", "Settings for Lock:" + setting.lock.lockNumber + ", " + setting.id)
                    } else if (setting.key != null) {
                        newDebugLog("Settings:", "Settings for Key:" + setting.key.keyNumber + ", " + setting.id)
                    }
                }
                coroutineScope.launch {
                    settings.emit(
                        ApiEvent.Settings(data = body)
                    )
                }
            } else {
                val code = response.code()
                newDebugLog("Settings:", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                newDebugLog("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                newDebugLog("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            newDebugLog("Settings:", e.message.toString())
        } catch (e: Exception) {
            newDebugLog("Settings:", e.message.toString())
        } finally {
            setBusy(false)
        }
    }

    private suspend fun newDebugLog(tag: String, message: String) {
        Log.d(tag, message)
        debugLogs.emit(
            Resource.Success(
                data = DebugLog(
                    phoneId = "",
                    timestamp = System.currentTimeMillis() / 1000,
                    event = message
                )
            )
        )
    }

    private fun setBusy(value: Boolean) {
        isBusy.set(value)
        if (value) {
            timeoutTimer = Timer()
            timeoutTimer.schedule(
                timerTask()
                {
                    isBusy.set(false)
                }, 30*1000)
        } else {
            timeoutTimer.cancel()
        }
    }
}