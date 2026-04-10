package com.example.purpos.screens

import android.R.attr.padding
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.android.gms.common.util.AndroidUtilsLight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = {
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = false
                        }
                        restoreState=true
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
        var name by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var sector by remember { mutableStateOf("") }
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        LaunchedEffect(Unit) {
            user?.uid?.let { uid ->
                db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            Log.d("Firestore", "Data: ${document.data}")

                            name = document.getString("name") ?: ""
                            location = document.getString("location") ?: ""
                            sector = document.getString("sector") ?: ""
                        }
                    }
            }
        }

        Text(
            text="Name: $name",
            color = MaterialTheme.colorScheme.primary,
            style=MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            )
        Text(
            text="Location: $location",
            color = MaterialTheme.colorScheme.primary,
            style=MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            )
        Text(
            text="Sector: $sector",
            color = MaterialTheme.colorScheme.primary,
            style=MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            )

    }
}