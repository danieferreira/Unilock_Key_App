package com.df.unilockkey.agent

import android.util.Log
import com.df.unilockkey.repository.Unilock
import com.df.unilockkey.util.ApiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timerTask

class LockService(private var api: ApiService) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    val locks: MutableSharedFlow<ApiEvent<Array<Unilock>>> = MutableSharedFlow()
    val lock: MutableSharedFlow<ApiEvent<Unilock>> = MutableSharedFlow()
    private var isBusy = AtomicBoolean(false)
    private var timeoutTimer: Timer = Timer()

    suspend fun getLocks() {
        try {
            if (!isBusy.get()) {
                setBusy(true)
                val response = api.getLocks()
                if (response.isSuccessful) {
                    val body = response.body()
                    for (lock in body!!) {
                        Log.d("Locks:", "Lock: " + lock.lockNumber.toString())
                    }
                    coroutineScope.launch {
                        locks.emit(
                            ApiEvent.Locks(data = body)
                        )
                    }
                } else {
                    val code = response.code()
                    Log.d("Locks:", code.toString())
                }
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
        } finally {
            setBusy(false)
        }
    }

    suspend fun getLock(id: Int) {
        try {
            if (!isBusy.get()) {
                setBusy(true)
                val response = api.getLock(id)
                if (response.isSuccessful) {
                    val thisLock = response.body()
                    if (thisLock != null) {
                        Log.d("Lock:", "Lock: " + thisLock.lockNumber+","+thisLock.activatedDate)
                        coroutineScope.launch {
                            lock.emit(
                                ApiEvent.Lock(data = thisLock)
                            )
                        }
                    }
                } else {
                    val code = response.code()
                    Log.d("Lock:", code.toString())
                }
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Lock:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Lock:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Lock:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Lock:", e.message.toString())
        } finally {
            setBusy(false)
        }
    }

    private fun setBusy(value: Boolean) {
        isBusy.set(value)
        if (value) {
            timeoutTimer = Timer()
            timeoutTimer.schedule(
                timerTask()
                {
                    isBusy.set(false)
                }, 10*1000)
        } else {
            timeoutTimer.cancel()
        }
    }
}