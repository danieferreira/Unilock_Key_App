package com.df.unilockkey.agent

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.df.unilockkey.util.ApiEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.UnknownHostException
import java.util.Timer
import kotlin.concurrent.timerTask

class Authenticate @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) {
    private val coroutineScope= CoroutineScope(Dispatchers.Default)
    private val refreshTimer: Timer = Timer()
    val data: MutableSharedFlow<ApiEvent<String>> = MutableSharedFlow()

    suspend fun login(request: LoginRequest) {
        try {
            tokenManager.saveToken("")
            tokenManager.saveRefreshToken("")
            val phoneId = getAdvertisingId(context)
            if (phoneId != null) {
                request.phoneId = phoneId
                val response = api.login(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        tokenManager.saveToken(body.token)
                        tokenManager.saveRefreshToken(body.refreshToken)
                        refreshTimer.schedule(
                            timerTask()
                            {
                                coroutineScope.launch { refreshLogin()}
                            }, 10*60*1000)
                        Log.d("Login", "User logged in")
                        coroutineScope.launch {
                            data.emit(
                                ApiEvent.LoggedIn(message = "Logged In", data = phoneId)
                            )
                        }
                    }
                } else {
                    val code = response.code()
                    coroutineScope.launch {
                        data.emit(
                            ApiEvent.Error(message = code.toString())
                        )
                    }
                    Log.d("Login", code.toString())
                }
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Login", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Login", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Login", e.message.toString())
        }
    }

    suspend fun refreshLogin() {
        try {
            tokenManager.saveToken("")
            tokenManager.saveRefreshToken("")
            val request = RefreshRequest(tokenManager.getRefreshToken())
            val response = api.refreshLogin(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    tokenManager.saveToken(body.token)
                    tokenManager.saveRefreshToken(body.refreshToken)
                    refreshTimer.schedule(
                        timerTask()
                        {
                            coroutineScope.launch { refreshLogin()}
                        }, 30*60*1000)
                    coroutineScope.launch {
                        data.emit(
                            ApiEvent.LoggedIn(message = "Logged In",)
                        )
                    }
                    Log.d("RefreshLogin", "User refreshed")
                }
            } else {
                val code = response.code()
                coroutineScope.launch {
                    data.emit(
                        ApiEvent.Error(message = code.toString())
                    )
                }
                Log.d("refreshLogin", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("refreshLogin", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("refreshLogin", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("refreshLogin", e.message.toString())
        }
    }


    @SuppressLint("HardwareIds")
    private suspend fun getAdvertisingId(@ApplicationContext context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID)

//                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
//                adInfo.id
            } catch (e: Exception) {
                Log.d("getID", e.toString())
                null
            }
        }
    }
}
