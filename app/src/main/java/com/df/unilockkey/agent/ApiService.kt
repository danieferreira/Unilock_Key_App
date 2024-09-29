package com.df.unilockkey.agent

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>
}

data class LoginRequest(
    val username: String,
    val password: String,
)

data class LoginResponse(
    val token: String,
)

class AuthInterceptor @Inject constructor(): Interceptor {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()

        val token = tokenManager.getToken()
        request.addHeader("Authorization", "Bearer $token")
        request.addHeader("Accept","application/json")
        return chain.proceed(request.build())
    }}

class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    private var prefs: SharedPreferences =
        context.getSharedPreferences("private", Context.MODE_PRIVATE)

    fun saveToken(token: String?) {
        val editor = prefs.edit()

        token?.let {
            editor.putString("bearer", token).apply()
        }
    }

    fun getToken(): String? {
        return prefs.getString("bearer", null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString("refresh", null)
    }

}

