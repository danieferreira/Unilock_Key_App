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

class SettingsApiService(private var api: ApiService) {
    private val coroutineScope= CoroutineScope(Dispatchers.Default)
    val settings: MutableSharedFlow<ApiEvent<Array<Settings>>> = MutableSharedFlow()
    val setting: MutableSharedFlow<ApiEvent<Settings>> = MutableSharedFlow()
    val debugLogs: MutableSharedFlow<Resource<DebugLog>> = MutableSharedFlow()

    private var busy: Boolean = false

    suspend fun getSettings() {
        try {
            while (busy) {
                delay(100)
            }
            busy = true;
            val response = api.getSettings()
            if (response.isSuccessful) {
                val body = response.body()
                for (setting in body!!) {
                    if (setting.lock != null) {
                        //NewDebugLog("Settings:", "Settings for " + setting.lock.lockNumber)
                    } else if (setting.key != null) {
                        NewDebugLog("Settings:", "Settings for " + setting.key.keyNumber + ". " + setting.id)
                    }
                }
                coroutineScope.launch {
                    settings.emit(
                        ApiEvent.Settings(data = body)
                    )
                }
            } else {
                val code = response.code()
                NewDebugLog("Settings:", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                NewDebugLog("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                NewDebugLog("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            NewDebugLog("Settings:", e.message.toString())
        } catch (e: Exception) {
            NewDebugLog("Settings:", e.message.toString())
        } finally {
            busy = false;
        }
    }

    suspend fun getSettingsByLock(lockNumber: Int) {
        try {
            while (busy) {
                delay(100)
            }
            busy = true;
            val response = api.getSettingsByLock(lockNumber)
            if (response.isSuccessful) {
                val body = response.body()
                for (setting in body!!) {
                    if (setting.lock != null) {
                        NewDebugLog("Settings:", "Settings for Lock:" + setting.lock.lockNumber + ", " + setting.id)
                    } else if (setting.key != null) {
                        NewDebugLog("Settings:", "Settings for Key:" + setting.key.keyNumber + ", " + setting.id)
                    }
                }
                coroutineScope.launch {
                    settings.emit(
                        ApiEvent.Settings(data = body)
                    )
                }
            } else {
                val code = response.code()
                NewDebugLog("Settings:", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                NewDebugLog("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                NewDebugLog("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            NewDebugLog("Settings:", e.message.toString())
        } catch (e: Exception) {
            NewDebugLog("Settings:", e.message.toString())
        } finally {
            busy = false;
        }
    }

    suspend fun getSettingsByKey(keyNumber: Int) {
        try {
            while (busy) {
                delay(100)
            }
            busy = true;
            val response = api.getSettingsByKey(keyNumber)
            if (response.isSuccessful) {
                val body = response.body()
                for (setting in body!!) {
                    if (setting.lock != null) {
                        NewDebugLog("Settings:", "Settings for Lock:" + setting.lock.lockNumber + ", " + setting.id)
                    } else if (setting.key != null) {
                        NewDebugLog("Settings:", "Settings for Key:" + setting.key.keyNumber + ", " + setting.id)
                    }
                }
                coroutineScope.launch {
                    settings.emit(
                        ApiEvent.Settings(data = body)
                    )
                }
            } else {
                val code = response.code()
                NewDebugLog("Settings:", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                NewDebugLog("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                NewDebugLog("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            NewDebugLog("Settings:", e.message.toString())
        } catch (e: Exception) {
            NewDebugLog("Settings:", e.message.toString())
        } finally {
            busy = false;
        }
    }

    private suspend fun NewDebugLog(tag: String, message: String) {
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
}