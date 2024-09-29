package com.df.unilockkey

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.df.unilockkey.agent.ApiService
import com.df.unilockkey.agent.Authenticate
import com.df.unilockkey.agent.LoginRequest
import com.df.unilockkey.presentation.Navigation
import com.df.unilockkey.ui.theme.UnilockKeyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter
    @Inject
    lateinit var api: ApiService
    @Inject
    lateinit var auth: Authenticate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnilockKeyTheme {
                Navigation(
                    onBluetoothStateChanged = {
                        showBluetoothDialog()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loginUser(auth)
        showBluetoothDialog()
    }

    private var isBluetoothDialogAlreadyShown = false
    private fun showBluetoothDialog() {
        if (!bluetoothAdapter.isEnabled) {
            if (!isBluetoothDialogAlreadyShown) {
                val bluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startBluetoothIntentForResults.launch(bluetoothIntent)
                isBluetoothDialogAlreadyShown = true
            }
        }
    }

    private val startBluetoothIntentForResults =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            isBluetoothDialogAlreadyShown = false
            if (result.resultCode != Activity.RESULT_OK) {
                showBluetoothDialog()
            }
        }

    private fun loginUser(auth: Authenticate) {

        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            auth.login(LoginRequest("Danie", "1234"))
        }
    }
}



