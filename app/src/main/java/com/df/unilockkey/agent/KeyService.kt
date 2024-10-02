package com.df.unilockkey.agent

import android.util.Log
import com.df.unilockkey.repository.Unikey
import com.df.unilockkey.util.ApiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException

class KeyService(private var api: ApiService) {
    private val coroutineScope= CoroutineScope(Dispatchers.Default)
    val data: MutableSharedFlow<ApiEvent<Array<Unikey>>> = MutableSharedFlow()

    suspend fun getKeys() {
        try {
            val response = api.getKeys()
            if (response.isSuccessful) {
                val body = response.body()
                for (key in body!!) {
                    Log.d("Keys:", "Key: " + key.keyNumber.toString())
                }
                coroutineScope.launch {
                    data.emit(
                        ApiEvent.Keys(data = body)
                    )
                }
            } else {
                val code = response.code()
                Log.d("Keys:", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Keys:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Keys:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Keys:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Keys:", e.message.toString())
        }
    }
}