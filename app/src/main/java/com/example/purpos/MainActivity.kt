package com.example.purpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.purpos.ui.theme.PURPOSTheme
import com.example.purpos.navigation.AppNavigation
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyBSmbh9UgusHX6zpLAKZ6649iZ9uM4mjgY") }

        val options = FirebaseOptions.Builder()
            .setProjectId("ngo-project-2eaec")
            .setApplicationId("1:554387372544:android:0cb62513fe33e2ce68b6e5")
            .setApiKey("AIzaSyAWd3mhxXKpvtaKJ0gn6CWO2l1nGV0JV-E")
            .setGcmSenderId("554387372544")
            .build()

        val secondApp = try {
            FirebaseApp.getInstance("VOLUNTEER_APP")
        } catch (e: Exception) {
            FirebaseApp.initializeApp(
                applicationContext,
                options,
                "VOLUNTEER_APP"
            )!!
        }

        setContent {
            PURPOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}