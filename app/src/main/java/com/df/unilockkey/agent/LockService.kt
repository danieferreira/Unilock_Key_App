package com.df.unilockkey.agent

import android.util.Log
import com.df.unilockkey.util.ApiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException

class LockService(private var api: ApiService) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    val data: MutableSharedFlow<ApiEvent<Array<UniLock>>> = MutableSharedFlow()

    suspend fun getLocks() {
        try {
            val response = api.getLocks()
            if (response.isSuccessful) {
                val body = response.body()
                for (lock in body!!) {
                    Log.d("Locks:", "Lock: " + lock.lockNumber.toString())
                }
                coroutineScope.launch {
                    data.emit(
                        ApiEvent.Locks(data = body)
                    )
                }
            } else {
                val code = response.code()
                Log.d("Locks:", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Locks:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Locks:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Locks:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Locks:", e.message.toString())
        }
    }
}