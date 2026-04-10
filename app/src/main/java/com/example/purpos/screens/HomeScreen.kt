package com.example.purpos.screens
import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Publish
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()

    Scaffold(

        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("PURPOS",color = MaterialTheme.colorScheme.primary) },
                actions = {
                    val focusManager= LocalFocusManager.current
                    IconButton(onClick = {focusManager.clearFocus()
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            restoreState=true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home"){
                        popUpTo("home")
                        launchSingleTop=true
                    } },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = currentRoute == "publish",
                    onClick = { navController.navigate("publish"){
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop=true
                        restoreState=true
                    } },
                    icon = { Icon(Icons.Default.Publish, contentDescription = "Publish") },
                    label = { Text("Publish") }
                )

                NavigationBarItem(
                    selected = currentRoute == "analytics",
                    onClick = { navController.navigate("Analytics"){
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop=true
                        restoreState=true
                    } },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )
            }
        }


    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var name by remember { mutableStateOf("") }
            val user = FirebaseAuth.getInstance().currentUser
            val db = FirebaseFirestore.getInstance()

            LaunchedEffect(Unit) {
                user?.uid?.let { uid ->
                    db.collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            name = document.getString("name") ?: ""
                        }
                }
            }

            Text(
                text = "Welcome to PURPOS, \n $name",
                color = MaterialTheme.colorScheme.primary,
                style=MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )


        }
    }
}