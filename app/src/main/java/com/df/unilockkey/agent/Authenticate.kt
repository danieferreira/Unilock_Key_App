package com.df.unilockkey.agent

import android.util.Log
import com.df.unilockkey.util.ApiEvent
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException

class Authenticate @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) {
    private val coroutineScope= CoroutineScope(Dispatchers.Default)
    val data: MutableSharedFlow<ApiEvent<String>> = MutableSharedFlow()

    suspend fun login(request: LoginRequest) {
        try {
            tokenManager.saveToken("")
            val response = api.login(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    tokenManager.saveToken(body.token)
                    Log.d("Login","User logged in")
                    coroutineScope.launch {
                        data.emit(
                            ApiEvent.LoggedIn(message = "Logged In")
                        )
                    }
                }
            } else {
                val code = response.code()
                data.emit(
                    ApiEvent.Error(message = code.toString())
                )
                Log.d("Login", code.toString())
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
}
