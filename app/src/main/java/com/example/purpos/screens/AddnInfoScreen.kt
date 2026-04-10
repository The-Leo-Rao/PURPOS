package com.example.purpos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddnUI(
    name: String,
    location: String,
    Sector: String,
    loading: Boolean,

    onFinnishclick:()-> Unit,
    onNameChange:(String)->Unit,
    onSectorChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    ){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enter Addition Information",
            color= MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            style=MaterialTheme.typography.titleLarge)

        Spacer(modifier=Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name", color = MaterialTheme.colorScheme.primary) }
        )

        Spacer(modifier=Modifier.height(16.dp))

        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {

            OutlinedTextField(
                value = Sector,
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
                            onSectorChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier=Modifier.height(16.dp))

        var predictions by remember { mutableStateOf(listOf<String>()) }
        val context = LocalContext.current
        val placesClient = remember { Places.createClient(context) }
        var query by remember { mutableStateOf("") }
        LaunchedEffect(query) {
            if (query.length > 2) {

                kotlinx.coroutines.delay(300)   // debounce (VERY IMPORTANT)

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
                        onLocationChange(it)
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
                                onLocationChange(it)
                                predictions = emptyList()
                            }
                        )
                    }
                }
            }
        }
        val focusManager=LocalFocusManager.current
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.tertiary
            ),
            onClick ={focusManager.clearFocus()
                onFinnishclick()},
            enabled = !loading,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Continue")
            }
        }
    }
}
@Composable
fun AddnInfoScreen(navController: NavController) {
    var loading by remember { mutableStateOf(false) }
    var name by remember {mutableStateOf("")}
    var sector by remember{mutableStateOf("")}
    var location by remember{mutableStateOf("")}
    AddnUI(
        name=name,
        Sector=sector,
        location=location,
        loading=loading,

        onNameChange = {name=it},
        onSectorChange = {sector=it},
        onLocationChange = { location = it },
        onFinnishclick = {

            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()

            val uid = auth.currentUser?.uid

            if (uid != null) {

                val data = hashMapOf(
                    "name" to name,
                    "location" to location,
                    "sector" to sector
                )

                db.collection("users")
                    .document(uid)
                    .set(data)
                    .addOnSuccessListener {
                        loading=false
                        navController.navigate("home")
                    }
                    .addOnFailureListener {
                        loading=false
                        println("Error saving data: ${it.message}")
                    }
            }
        }
    )
}