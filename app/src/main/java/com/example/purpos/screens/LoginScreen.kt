package com.example.purpos.screens

import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.purpos.ui.theme.PURPOSTheme


@Composable
fun LoginUI(
    email: String,
    password: String,
    passwordVisible: Boolean,
    errMessage: String,

    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onForgotClick: ()-> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Login"
            ,color = MaterialTheme.colorScheme.primary
            ,style= MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", color = MaterialTheme.colorScheme.primary) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password", color = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            visualTransformation = if (passwordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password"
                    )
                }
            }
        )
        Box(
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            TextButton(
                onClick = onForgotClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("forgot password?"
                    ,color = MaterialTheme.colorScheme.primary
                    ,style= MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(errMessage, color = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.tertiary
            ),
            onClick = onLoginClick
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSignupClick) {
            Text("Go to Signup", color = MaterialTheme.colorScheme.primary)
        }
    }
}
@Composable
fun LoginScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errMessage by remember { mutableStateOf("") }

    LoginUI(
        email = email,
        password = password,
        passwordVisible = passwordVisible,
        errMessage = errMessage,

        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onTogglePassword = { passwordVisible = !passwordVisible },

        onLoginClick = {
            if (email.isBlank() || password.isBlank()) {
                errMessage = "Please enter email and password"
                return@LoginUI
            }
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        errMessage = "Invalid Credentials"
                    }
                }
        },

        onSignupClick = {
            navController.navigate("signup")
        },
        onForgotClick={navController.navigate("forgotp")}
    )
}


@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    PURPOSTheme {
        LoginUI(
            email = "",
            password = "",
            passwordVisible = false,
            errMessage = "",

            onEmailChange = {},
            onPasswordChange = {},
            onTogglePassword = {},
            onLoginClick = {},
            onSignupClick = {},
            onForgotClick = {}
        )
    }
}