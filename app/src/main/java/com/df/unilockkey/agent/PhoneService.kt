package com.df.unilockkey.agent

import android.util.Log
import com.df.unilockkey.repository.Phone
import com.df.unilockkey.util.ApiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException

class PhoneService(private var api: ApiService) {
    private val coroutineScope= CoroutineScope(Dispatchers.Default)
    private var busy: Boolean = false
    val data: MutableSharedFlow<ApiEvent<Phone>> = MutableSharedFlow()

    suspend fun getPhone(phoneId: String) {
        try {
            if (!busy) {
                busy = true;
                val response = api.getPhone(phoneId)
                if (response.isSuccessful) {
                    val phone = response.body()
                    if (phone != null) {
                        Log.d("Phone:", "Phone: " + phone.number)
                        coroutineScope.launch {
                            data.emit(
                                ApiEvent.Phone(data = phone)
                            )
                        }
                    }
                } else {
                    val code = response.code()
                    Log.d("Phone:", code.toString())
                }
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Phone:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Phone:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Phone:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Phone:", e.message.toString())
        } finally {
            busy = false
        }
    }
}