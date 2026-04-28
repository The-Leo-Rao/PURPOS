package com.example.purpos.screens
import android.R
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.DatasetLinked
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

suspend fun createAndUploadCsv(name: String, content: String) {
    val ref = FirebaseStorage.getInstance().reference
        .child("${userStoragePath()}/$name.csv")
    ref.putBytes(content.toByteArray()).await()
}
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
                    selected = currentRoute == "My Data",
                    onClick = { navController.navigate("My Data"){
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop=true
                        restoreState=true
                    } },
                    icon = { Icon(Icons.Default.DatasetLinked, contentDescription = "My Data") },
                    label = { Text("My Data") }
                )
            }
        }


    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
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
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            Box(
                modifier = Modifier
                    .height(screenHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome to PURPOS, \n $name",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    Icon(
                        imageVector = Icons.Default.ArrowCircleDown,
                        contentDescription = "Move Down"
                    )
                }
            }
            fun createCsvContent(columns: List<String>): String {
                return columns.joinToString(",")
            }

            var showDialog by remember { mutableStateOf(false) }
            var step by remember { mutableStateOf(1) }
            var columnCount by remember { mutableStateOf(1) }
            var columnNames by remember { mutableStateOf(List(1) { "" }) }
            val context = LocalContext.current
            val focusManager=LocalFocusManager.current

            Button(onClick = {
                showDialog = true
                step = 1
            }) {
                Text(
                    text="Create a Dataframe",
                    color= MaterialTheme.colorScheme.secondary,
                    style= MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier= Modifier.height(24.dp))

            Text("Our PURPOS", color= MaterialTheme.colorScheme.primary, style= MaterialTheme.typography.titleMedium,
                modifier= Modifier.fillMaxWidth().padding(start=16.dp), textAlign = TextAlign.Start)

            Spacer(modifier= Modifier.height(8.dp))

            Text("PURPOS was made with the vision of connecting NGO's to volunteers, facilitating the process of matching requirements to availabilities.\nWe intelligently match volunteers with NGOs based on skills, availability, and interests. Our tools simplify the volunteering process by providing a seamless and intuitive platform for discovery and engagement.It also enables NGO's to leverage data analytics to better understand volunteer engagement and optimize their outreach.\n\n", style= MaterialTheme.typography.bodyMedium,
                modifier= Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp))

            Spacer(modifier= Modifier.height(8.dp))

            Text("Built for NGO's. Powered by purpose.\n", color= MaterialTheme.colorScheme.primary, style= MaterialTheme.typography.titleMedium)

            Spacer(modifier= Modifier.height(24.dp))

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {},
                    title = {
                        Text(if (step == 1) "Select Columns" else "Enter Column Names")
                    },
                    text = {

                        if (step == 1) {
                            var expanded by remember { mutableStateOf(false) }
                            Column {
                                Text("Number of columns")

                                Box {
                                    OutlinedTextField(
                                        value = columnCount.toString(),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Columns") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        (1..9).forEach {
                                            DropdownMenuItem(
                                                text = { Text("$it") },
                                                onClick = {
                                                    columnCount = it
                                                    columnNames = List(it) { "" }
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { expanded = true }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(onClick = { step = 2 }) {
                                    Text("Next",color= MaterialTheme.colorScheme.tertiary)
                                }
                            }

                        } else {
                            Column {
                                val allFilled = columnNames.all { it.trim().isNotEmpty() }
                                repeat(columnCount) { index ->
                                    OutlinedTextField(
                                        value = columnNames[index],
                                        onValueChange = { newValue ->
                                            columnNames = columnNames.toMutableList().apply {
                                                this[index] = newValue
                                                this[index] = newValue
                                            }
                                        },
                                        label = { Text("Column ${index + 1}") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (!allFilled){
                                    Text(
                                        text = "All fields must be filled",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row {
                                    Button(onClick = { step = 1 }) {
                                        Text("Back",color= MaterialTheme.colorScheme.tertiary)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val scope = rememberCoroutineScope()
                                    var uploading by remember { mutableStateOf(false) }

                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            val fileName = "csv_${System.currentTimeMillis()}"
                                            val csv = createCsvContent(columnNames)
                                            uploading = true
                                            scope.launch {
                                                try {
                                                    createAndUploadCsv(fileName, csv)
                                                    showDialog = false
                                                } catch (e: Exception) {
                                                    // show a snackbar or message if needed
                                                } finally {
                                                    uploading = false
                                                }
                                            }
                                        },
                                        enabled = allFilled && !uploading
                                    ) {
                                        if (uploading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        } else {
                                            Text(
                                                text = "Create",
                                                color = MaterialTheme.colorScheme.secondary,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }




        }
    }
}