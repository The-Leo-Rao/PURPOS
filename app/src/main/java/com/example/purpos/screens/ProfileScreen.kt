package com.example.purpos.screens

import android.R
import android.R.attr.padding
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.android.gms.common.util.AndroidUtilsLight
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    Box(modifier = Modifier.fillMaxWidth()) {
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
                .padding(
                    top = 45.dp,
                    start = 15.dp
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        var showDialog by remember { mutableStateOf(false) }
        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = 45.dp,
                    end = 15.dp
                )
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "About Us"
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(onClick = {
                        showDialog = false
                    }) {
                        Text("Close")
                    }
                },
                title = {
                    Text(
                        text = "About Us",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    val scrollState = rememberScrollState()
                    var bod =
                        "PURPOS was made with the vision of connecting NGO's to volunteers, facilitating the process of matching requirements to availabilities.\nWe intelligently matches volunteers with NGOs based on skills, availability, and interests. Our tools simplify the volunteering process by providing a seamless and intuitive platform for discovery and engagement.It also enables NGO's to leverage data analytics to better understand volunteer engagement and optimize their outreach."
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = bod,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        fun updateSingleField(field: String, value: String) {
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .update(field, value)
                    .addOnSuccessListener {
                        Log.d("Firestore", "$field updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error updating $field", e)
                    }
            }
        }

        Row(
            modifier= Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showDialog by remember { mutableStateOf(false) }
            var editedName by remember { mutableStateOf(name) }

            Text(
                text = "Name: $name",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    editedName=name
                    showDialog=true
                }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Name")
            }
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        Button(onClick = {
                            showDialog = false
                            updateSingleField("name",editedName)
                            name = editedName
                        }) {
                            Text("OK")
                        }
                    },
                    title = { Text(
                        text="Edit Name" ,
                        color = MaterialTheme.colorScheme.primary,
                        style=MaterialTheme.typography.titleLarge,) },
                    text = {
                        Column{
                            Text(
                                text="Enter new Name: ",
                                color = MaterialTheme.colorScheme.primary,
                                style=MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                placeholder = { Text("Name") })
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier= Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showDialog by remember { mutableStateOf(false) }
            var editedsector by remember { mutableStateOf(sector) }

            Text(
                text = "Sector: $sector",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    editedsector=sector
                    showDialog=true
                }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Sector")
            }
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        Button(onClick = {
                            showDialog = false
                            updateSingleField("sector",editedsector)
                            sector=editedsector
                        }) {
                            Text("OK")
                        }
                    },
                    title = { Text(
                        text="Edit sector" ,
                        color = MaterialTheme.colorScheme.primary,
                        style=MaterialTheme.typography.titleLarge,) },
                    text = {
                        Column{
                            Text(
                                text="Enter new Sector: ",
                                color = MaterialTheme.colorScheme.primary,
                                style=MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {

                                OutlinedTextField(
                                    value = editedsector,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sector") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                    },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf("Health", "Education","Environmental","Poverty Alleviation","women empowerment","Disaster Relief","Other").forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                editedsector=option
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier= Modifier.height(16.dp))

        Row(
            modifier= Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showDialog by remember { mutableStateOf(false) }
            var editedloc by remember { mutableStateOf(location) }
            var predictions by remember { mutableStateOf(listOf<String>()) }
            val context = LocalContext.current
            val placesClient = remember { Places.createClient(context) }
            var query by remember { mutableStateOf("") }

            Text(
                text = "location: $location",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    editedloc=location
                    showDialog=true
                }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Name")
            }
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        Button(onClick = {
                            showDialog = false
                            updateSingleField("location",editedloc)
                            location=editedloc

                        }) {
                            Text("OK")
                        }
                    },
                    title = { Text(
                        text="Edit location" ,
                        color = MaterialTheme.colorScheme.primary,
                        style=MaterialTheme.typography.titleLarge,) },
                    text = {
                        Column{
                            Text(
                                text="Enter new location: ",
                                color = MaterialTheme.colorScheme.primary,
                                style=MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LaunchedEffect(query) {
                                if (query.length > 2) {

                                    delay(300)   // debounce (VERY IMPORTANT)

                                    val request = FindAutocompletePredictionsRequest.builder()
                                        .setQuery(query)
                                        .setCountries(listOf("IN"))
                                        .build()

                                    placesClient.findAutocompletePredictions(request)
                                        .addOnSuccessListener { response ->
                                            predictions = response.autocompletePredictions.map {
                                                it.getFullText(null).toString()
                                            }
                                        }

                                } else {
                                    predictions = emptyList()
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth()) {

                                Column {

                                    OutlinedTextField(
                                        value = query,
                                        onValueChange = {
                                            query=it
                                            editedloc=it
                                        },
                                        label = { Text("Location") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    val focusManager=LocalFocusManager.current
                                    DropdownMenu(
                                        expanded = predictions.isNotEmpty(),
                                        onDismissRequest = { predictions = emptyList() }
                                    ) {
                                        predictions.forEach {
                                            DropdownMenuItem(
                                                text = { Text(it) },
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    editedloc=it
                                                    predictions = emptyList()
                                                }
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

        Spacer(modifier= Modifier.height(16.dp))

        Button(
            onClick = {
                auth.signOut()
                navController.navigate("login"){
                    popUpTo(0)
                }
            }
        ) {Text(
            text="Log Out",
            color= MaterialTheme.colorScheme.secondary,
            style= MaterialTheme.typography.bodyLarge
        ) }

    }
}