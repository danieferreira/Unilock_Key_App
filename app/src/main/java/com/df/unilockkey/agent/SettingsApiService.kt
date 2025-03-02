package com.df.unilockkey.agent

import android.util.Log
import com.df.unilockkey.repository.Settings
import com.df.unilockkey.util.ApiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException

class SettingsApiService(private var api: ApiService) {
    private val coroutineScope= CoroutineScope(Dispatchers.Default)
    val settings: MutableSharedFlow<ApiEvent<Array<Settings>>> = MutableSharedFlow()
    val setting: MutableSharedFlow<ApiEvent<Settings>> = MutableSharedFlow()

    private var busy: Boolean = false

    suspend fun getSettings() {
        try {
            if (!busy) {
                busy = true;
                val response = api.getSettings()
                if (response.isSuccessful) {
                    val body = response.body()
                    for (setting in body!!) {
                        if (setting.lock != null) {
                            Log.d("Settings:", "Settings for " + setting.lock.lockNumber)
                        } else if (setting.key != null) {
                            Log.d("Settings:", "Settings for " + setting.key.keyNumber + ". " + setting.id)
                        }
                    }
                    coroutineScope.launch {
                        settings.emit(
                            ApiEvent.Settings(data = body)
                        )
                    }
                } else {
                    val code = response.code()
                    Log.d("Settings:", code.toString())
                }
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Settings:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Settings:", e.message.toString())
        } finally {
            busy = false;
        }
    }

    suspend fun getSettingsByLock(lockNumber: Int) {
        try {
            if (!busy) {
                busy = true;
                val response = api.getSettingsByLock(lockNumber)
                if (response.isSuccessful) {
                    val body = response.body()
                    for (setting in body!!) {
                        if (setting.lock != null) {
                            Log.d("Settings:", "Settings for Lock:" + setting.lock.lockNumber)
                        } else if (setting.key != null) {
                            Log.d("Settings:", "Settings for Key:" + setting.key.keyNumber + ", " + setting.id)
                        }
                    }
                    coroutineScope.launch {
                        settings.emit(
                            ApiEvent.Settings(data = body)
                        )
                    }
                } else {
                    val code = response.code()
                    Log.d("Settings:", code.toString())
                }
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Settings:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Settings:", e.message.toString())
        } finally {
            busy = false;
        }
    }

    suspend fun getSettingsByKey(keyNumber: Int) {
        try {
            if (!busy) {
                busy = true;
                val response = api.getSettingsByKey(keyNumber)
                if (response.isSuccessful) {
                    val body = response.body()
                    for (setting in body!!) {
                        if (setting.lock != null) {
                            Log.d("Settings:", "Settings for Lock:" + setting.lock.lockNumber)
                        } else if (setting.key != null) {
                            Log.d("Settings:", "Settings for Key:" + setting.key.keyNumber + ", " + setting.id
                            )
                        }
                    }
                    coroutineScope.launch {
                        settings.emit(
                            ApiEvent.Settings(data = body)
                        )
                    }
                } else {
                    val code = response.code()
                    Log.d("Settings:", code.toString())
                }
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Settings:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Settings:", e.message.toString())
        } finally {
            busy = false;
        }
    }

    suspend fun getSetting(id: Int) {
        try {
            if (!busy) {
                busy = true;
                val response = api.getSetting(id)
                if (response.isSuccessful) {
                    val thisSetting = response.body()
                    if (thisSetting != null) {
                        Log.d("Settings:", "Setting: " + thisSetting.id)
                        coroutineScope.launch {
                            setting.emit(
                                ApiEvent.Settings(data = thisSetting)
                            )
                        }
                    }
                } else {
                    val code = response.code()
                    Log.d("Settings:", code.toString())
                }
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Settings:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Settings:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Settings:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Settings:", e.message.toString())
        } finally {
            busy = false
        }
    }
}