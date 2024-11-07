package biz.itonline.trackrecord.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import biz.itonline.trackrecord.R

@Preview(showBackground = true)
@Composable
fun LoginScreen(
    processLogin: (String, String) -> Unit = { _, _ -> }
) {
    var login by remember {
        mutableStateOf("")
    }

    var masked by remember {
        mutableStateOf(true)
    }

    val visualTransformation by remember(masked) {
        if (masked) mutableStateOf(PasswordVisualTransformation(mask = '*'))
        else mutableStateOf(VisualTransformation.None)
    }

    var password by remember {
        mutableStateOf("")
    }

    val enableButton = login.isNotEmpty() && password.length > 4

    val imeAction = if (enableButton) ImeAction.Done else ImeAction.None

    val keyboardOptions = remember {
        mutableStateOf(KeyboardOptions(imeAction = imeAction))
    }

    var isPswdError by remember {
        mutableStateOf(false)
    }

    val controller = LocalSoftwareKeyboardController.current

    val onButtonClick: () -> Unit = {
        if (enableButton) {
            controller?.hide()
            processLogin(login, password)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(modifier = Modifier.padding(10.dp),
                text = "Přihlášení uživatele",
                fontSize = MaterialTheme.typography.headlineLarge.fontSize)
            Image(
                painter = painterResource(id = R.drawable.login),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 30.dp, vertical = 25.dp)
                    .requiredWidth(200.dp)
            )
            OutlinedTextField(
                value = login,
                onValueChange = {
                    login = it
                    keyboardOptions.value = KeyboardOptions(imeAction = imeAction)
                },
                label = { Text(text = "Uživatelské jméno / Email") },
                supportingText = { Text(text = "") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Red,
                    unfocusedBorderColor = Color.LightGray,
                    disabledBorderColor = Color.Gray,
                )
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    keyboardOptions.value = KeyboardOptions(imeAction = imeAction)
                    isPswdError = password.length < 5
                },
                isError = isPswdError,
                label = { Text(text = "Heslo") },
                visualTransformation = visualTransformation,
                trailingIcon = {
                    if (masked) {
                        IconButton(onClick = {
                            masked = false
                        }) { Icon(imageVector = Icons.Filled.VisibilityOff, contentDescription = null) }
                    } else {
                        IconButton(onClick = {
                            masked = true
                        }) {
                            Icon(imageVector = Icons.Filled.Visibility, contentDescription = null)
                        }
                    }
                },
                supportingText = { Text(text = "") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onButtonClick()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    errorBorderColor = Color.Red,
                    unfocusedBorderColor = Color.LightGray,
                    disabledBorderColor = Color.Gray,
                )
            )
            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(
                    enabled = enableButton,
                    onClick = onButtonClick
                ) {
                    Text(text = "Přihlásit")
                }

            }

        }
    }
}