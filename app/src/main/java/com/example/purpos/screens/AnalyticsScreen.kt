package com.example.purpos.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

fun getAllCsvFiles(context: Context): List<String> {
    return context.filesDir
        .listFiles()
        ?.filter { it.isFile && it.extension == "csv" }
        ?.map { it.nameWithoutExtension }
        ?.sorted()
        ?: emptyList()
}

@Composable
fun AnalyticsScreen(navController: NavController) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home"){
                        popUpTo("home")
                        launchSingleTop=true
                        restoreState=true
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

        val context = LocalContext.current
        val csvFiles = remember {getAllCsvFiles(context)}

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Text(
                text = "Saved CSV Files",
                color=MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier= Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(24.dp))

            csvFiles.forEach { fileName ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = fileName,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}