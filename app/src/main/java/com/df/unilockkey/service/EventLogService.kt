package com.df.unilockkey.service

import android.util.Log
import com.df.unilockkey.agent.ApiService
import com.df.unilockkey.agent.Authenticate
import com.df.unilockkey.repository.AppDatabase
import com.df.unilockkey.repository.EventLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.concurrent.timerTask

class EventLogService  @Inject constructor(
    private val appDatabase: AppDatabase,
    private var api: ApiService,
    private val auth: Authenticate,
){
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private var isBusy = AtomicBoolean(false)
    private var timeoutTimer: Timer = Timer()

    suspend fun logEvent(event: String, keyNumber: Int, lockNumber: Int, battery: String) {
        val eventLog = EventLog(
            timestamp = System.currentTimeMillis()/1000,
            event = event,
            keyNumber = keyNumber,
            lockNumber = lockNumber,
            battery = battery.replace(',','.'),
            archived = false)
        appDatabase.eventLogDao().insertAll(eventLog)
        scope.launch {
            syncEventLogs()
        }
    }

    suspend fun logEvent(eventLog: EventLog) {
        eventLog.timestamp = System.currentTimeMillis()/1000
        eventLog.archived = false
        appDatabase.eventLogDao().insertAll(eventLog)
//        scope.launch {
//            syncEventLogs()
//        }
    }

    suspend fun syncEventLogs() {
        if (!isBusy.get()) {
            try {
                setBusy(true)
                val logs = appDatabase.eventLogDao().getAllByArchive(false)
                for (log in logs) {
                    log.archived = true
                    api.postEventLog(log)
                    appDatabase.eventLogDao().update(log)
                    Log.d("EventLogService:", "Archive: " + log.id + ":" + log.event)
                }
            } catch (e: HttpException) {
                val response = e.response()
                val errorCode = e.code()
                if (response != null) {
                    Log.d("EventLogService:", response.message() + ":" + errorCode.toString())
                } else {
                    Log.d("EventLogService:", errorCode.toString())
                }
                scope.launch { auth.refreshLogin()}
            } catch (e: UnknownHostException) {
                Log.d("EventLogService:", e.message.toString())
            } catch (e: Exception) {
                Log.d("EventLogService:", e.message.toString())
            } finally {
                setBusy(false);
            }
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