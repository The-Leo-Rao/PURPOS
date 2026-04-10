package com.example.purpos.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.util.Patterns
import androidx.compose.foundation.background
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.purpos.ui.theme.PURPOSTheme

@Composable
fun SignUpUI(
    email: String,
    password: String,
    passwordVisible: Boolean,
    errMessage: String,

    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Sign Up"
            ,color = MaterialTheme.colorScheme.primary
            ,style= MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", color = MaterialTheme.colorScheme.primary) },
            singleLine = true
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
                IconButton(onClick =onTogglePassword) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(errMessage, color = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(16.dp))
        val focusManager=LocalFocusManager.current
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.tertiary
            ),
            onClick ={onSignUpClick()
                focusManager.clearFocus()}
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick =onLoginClick) {
            Text("Already have an account?", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SignUpScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errMessage by remember { mutableStateOf("") }

    SignUpUI(
        email = email,
        password = password,
        passwordVisible = passwordVisible,
        errMessage = errMessage,

        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onTogglePassword = { passwordVisible = !passwordVisible },

        onSignUpClick = {

            when {
                email.isBlank() -> {
                    errMessage = "Email cannot be empty"
                }

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    errMessage = "Invalid email format"
                }

                password.length < 8 -> {
                    errMessage = "Password must be at least 8 characters"
                }

                else -> {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                navController.navigate("addn") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            } else {
                                errMessage = task.exception?.message ?: "Signup failed"
                            }
                        }
                }
            }
        },

        onLoginClick = {
            navController.navigate("login")
        }
    )
}


@Preview(showBackground = true)
@Composable
fun SignUpPreview() {
    PURPOSTheme {
        SignUpUI(
            email = "",
            password = "",
            passwordVisible = false,
            errMessage = "",

            onEmailChange = {},
            onPasswordChange = {},
            onTogglePassword = {},
            onSignUpClick = {},
            onLoginClick = {}
        )
    }
}