package com.df.unilockkey.agent

import android.util.Log
import retrofit2.HttpException
import java.net.UnknownHostException

class Authenticate(private var api: ApiService, private val tokenManager: TokenManager) {
    suspend fun login(request: LoginRequest) {
        try {
            val response = api.login(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    tokenManager.saveToken(body.token)
                }
            } else {
                val code = response.code()
                Log.d("Login:", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Login:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Login:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Login:", e.message.toString())
        }
    }
}