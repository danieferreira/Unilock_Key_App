package com.df.unilockkey.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType.Companion.Password
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController


@Composable
fun StartScreen(
    navController: NavController,
    viewModel: StartScreenViewModel = hiltViewModel()
) {

    var username = remember { mutableStateOf(TextFieldValue()) }
    var password = remember { mutableStateOf(TextFieldValue()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mContext = LocalContext.current
    var snackbarMessage = "Succeed!"
    val showSnackbar = remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(showSnackbar.value) {
        if (showSnackbar.value)
            snackbarHostState.showSnackbar(snackbarMessage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
     ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            //Spacer
            Box(
                modifier = Modifier
                    .size(50.dp)
            ){}
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            //Spacer
            Text(
                "UniLock Key",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            TextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text("Enter Username") },
                modifier = Modifier
                    .padding(all = 16.dp)
                    .fillMaxWidth(),
                enabled = true,
                readOnly = false,
                singleLine = true,
                isError = false,

                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {keyboardController?.hide()})
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            TextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Enter Password") },
                modifier = Modifier
                    .padding(all = 16.dp)
                    .fillMaxWidth(),
                enabled = true,
                readOnly = false,
                singleLine = true,
                isError = false,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = Password,
                    imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {keyboardController?.hide()})
            )
        }

        Row(
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.Cyan, CircleShape)
                    .clickable {
                        var message = "Login Failed";
                        val status = viewModel.login(username.value.text, password.value.text)
                        if (status == 200) {
                            navController.navigate(Screen.KeyInfoScreen.route) {
                                popUpTo(Screen.StartScreen.route) {
                                    inclusive = true
                                }
                            }
                        } else {
                            password.value = TextFieldValue("")
                            username.value = TextFieldValue("")

                            if (status == 406) {
                                message = "Please enable Phone in Unilock Access Manager";
                            }
                            Toast
                                .makeText(mContext, message, Toast.LENGTH_LONG)
                                .show()
                            // show snackbar with it.error
                            snackbarMessage = message
                            showSnackbar.value = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start",
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow
                )
            }
        }
    }
}