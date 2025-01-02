package com.df.unilockkey.agent

import android.content.Context
import android.content.SharedPreferences
import com.df.unilockkey.repository.EventLog
import com.df.unilockkey.repository.Phone
import com.df.unilockkey.repository.Route
import com.df.unilockkey.repository.Unikey
import com.df.unilockkey.repository.Unilock
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>

    @POST("/api/refresh")
    suspend fun refreshLogin(@Body request: RefreshRequest): retrofit2.Response<LoginResponse>

    @GET("/api/key")
    suspend fun getKeys(): retrofit2.Response<Array<Unikey>>

    @GET("/api/lock")
    suspend fun getLocks(): retrofit2.Response<Array<Unilock>>

    @POST("/api/event")
    suspend fun postEventLog(@Body event: EventLog)

    @GET("/api/route")
    suspend fun getRoutes(): retrofit2.Response<Array<Route>>

    @GET("/api/phone/{phoneId}")
    suspend fun getPhone(@Path("phoneId") phoneId: String): retrofit2.Response<Phone>

    @GET("/api/route/{id}")
    suspend fun getRoute(@Path("id") id: Int): retrofit2.Response<Route>

    @GET("/api/lock/{lockNumber}")
    suspend fun getLock(@Path("lockNumber") lockNumber: Int): retrofit2.Response<Unilock>

    @POST("/api/lock")
    suspend fun postLock(@Body lock: Unilock)
}

data class LoginRequest(
    val username: String,
    val password: String,
    var phoneId: String? = null
)

data class RefreshRequest (
    val refreshToken: String?
)

data class LoginResponse(
    val token: String,
    val refreshToken: String?
)

class AuthInterceptor @Inject constructor(): Interceptor {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()

        val token = tokenManager.getToken()
        if ((token != null) && (token != "")) {
            request.addHeader("Authorization", "Bearer $token")
            request.addHeader("Accept", "application/json")
        }
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

    fun saveRefreshToken(refreshToken: String?) {
        val editor = prefs.edit()

        refreshToken?.let {
            editor.putString("refresh", refreshToken).apply()
        }
    }

}

