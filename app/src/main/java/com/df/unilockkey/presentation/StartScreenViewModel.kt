package com.df.unilockkey.presentation

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.df.unilockkey.agent.Authenticate
import com.df.unilockkey.agent.LoginRequest
import com.df.unilockkey.service.DatabaseSyncService
import com.df.unilockkey.util.ApiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class StartScreenViewModel @Inject constructor(
    private val databaseSyncService: DatabaseSyncService,
    private val auth: Authenticate,
): ViewModel() {

    var errorMessage = MutableLiveData<String?>("");


    private var LoggedIn: Boolean = false;

    fun login(username: String, password: String ): Int {
        var status = 404;
        try {
            runBlocking {
                auth.login(LoginRequest(username, password))
                val result = auth.data.first()
                if (result is ApiEvent.LoggedIn) {
                    LoggedIn = true;
                    status = 200;
                }
                if (result is ApiEvent.Error) {
                    if (result.message == "406") {
                        errorMessage.value = "Please enable Phone in Unilock Access Manager";
                        status = 406
                    }
                    LoggedIn = false;
                }
            }

        } catch (err: Exception) {
            Log.d("StartScreenViewModel", err.message.toString())
        }
        if (LoggedIn) {
            databaseSyncService.startSync()
        }
        return status
    }
}