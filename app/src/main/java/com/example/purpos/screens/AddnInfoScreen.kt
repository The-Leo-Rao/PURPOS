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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddnUI(
    name: String,
    locality: String,
    city: String,
    state: String,
    Sector: String,
    loading: Boolean,

    onFinnishclick:()-> Unit,
    onNameChange:(String)->Unit,
    onSectorChange: (String) -> Unit,
    onLocalityChange: (String) -> Unit,
    onCityChange: (String)-> Unit,
    onStateChange: (String)-> Unit
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
                label = { Text("Sector", color= MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf("Agriculture", "Animal Welfare", "Art and Culture", "Child Welfare", "Disaster Relief", "Education", "Environmental", "Health", "Legal Aid", "Poverty Alleviation", "Social Welfare", "Women Empowerment").forEach { option ->
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

        OutlinedTextField(
            value = locality,
            onValueChange = onLocalityChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Locality", color = MaterialTheme.colorScheme.primary) }
        )

        Spacer(modifier=Modifier.height(16.dp))

        var citypredictions by remember { mutableStateOf(listOf<String>()) }
        val context = LocalContext.current
        val placesClient = remember { Places.createClient(context) }
        var cityquery by remember { mutableStateOf("") }
        LaunchedEffect(cityquery) {
            if (cityquery.length > 2) {

                kotlinx.coroutines.delay(300)

                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(cityquery)
                    .setCountries(listOf("IN"))
                    .setTypesFilter(listOf("locality"))
                    .build()

                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        citypredictions = response.autocompletePredictions.map {
                            it.getPrimaryText(null).toString()
                        }
                    }

            } else {
                citypredictions = emptyList()
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {

            Column {

                OutlinedTextField(
                    value = cityquery,
                    onValueChange = {
                        cityquery=it
                        onCityChange(it)
                    },
                    label = { Text(
                            text="City",
                            color= MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth()
                )
                val focusManager=LocalFocusManager.current
                DropdownMenu(
                    expanded = citypredictions.isNotEmpty(),
                    onDismissRequest = { citypredictions = emptyList() }
                ) {
                    citypredictions.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                cityquery=it
                                focusManager.clearFocus()
                                onCityChange(it)
                                citypredictions = emptyList()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier=Modifier.height(16.dp))
        var statequery by remember { mutableStateOf("") }
        var statepredictions by remember { mutableStateOf(listOf<String>()) }

        LaunchedEffect(statequery) {
            if (statequery.length > 2) {

                kotlinx.coroutines.delay(300)

                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(statequery)
                    .setCountries(listOf("IN"))
                    .setTypesFilter(listOf("administrative_area_level_1"))
                    .build()

                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        statepredictions = response.autocompletePredictions.map {
                            it.getPrimaryText(null).toString()
                        }.distinct()
                    }
            } else {
                statepredictions = emptyList()
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {

            Column {

                OutlinedTextField(
                    value = statequery,
                    onValueChange = {
                        statequery=it
                        onStateChange(it)
                    },
                    label = { Text(
                            text="State",
                            color= MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth()
                )
                val focusManager=LocalFocusManager.current
                DropdownMenu(
                    expanded = statepredictions.isNotEmpty(),
                    onDismissRequest = { statepredictions = emptyList() }
                ) {
                    statepredictions.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                statequery=it
                                focusManager.clearFocus()
                                onStateChange(it)
                                statepredictions = emptyList()
                            }
                        )
                    }
                }
            }
        }
        val focusManager=LocalFocusManager.current
        Spacer(modifier=Modifier.height(16.dp))
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.tertiary
            ),
            onClick ={focusManager.clearFocus()
                onFinnishclick()},
        ) {Text(
            text="Continue",
            color= MaterialTheme.colorScheme.secondary,
            style= MaterialTheme.typography.bodyMedium
        )}
    }
}
@Composable
fun AddnInfoScreen(navController: NavController) {
    var loading by remember { mutableStateOf(false) }
    var name by remember {mutableStateOf("")}
    var sector by remember{mutableStateOf("")}
    var locality by remember{mutableStateOf("")}
    var city by remember{mutableStateOf("")}
    var state by remember{mutableStateOf("")}
    var showVerifyDialog by remember { mutableStateOf(false) }
    AddnUI(
        name=name,
        Sector=sector,
        locality=locality,
        city=city,
        state=state,
        loading=loading,

        onNameChange = {name=it},
        onSectorChange = {sector=it},
        onLocalityChange = { locality = it },
        onCityChange = {city=it},
        onStateChange = {state=it},
        onFinnishclick = {
            loading = true
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            val user = auth.currentUser

            user?.reload()?.addOnCompleteListener {

                val refreshedUser = FirebaseAuth.getInstance().currentUser

                if (refreshedUser != null && refreshedUser.isEmailVerified) {

                    val uid = refreshedUser.uid

                    val data = hashMapOf(
                        "name" to name,
                        "locality" to locality,
                        "city" to city,
                        "state" to state,
                        "sector" to sector
                    )

                    db.collection("users")
                        .document(uid)
                        .set(data)
                        .addOnSuccessListener {
                            loading = false
                            navController.navigate("home")
                        }
                        .addOnFailureListener {
                            loading = false
                        }

                } else {
                    loading = false
                    showVerifyDialog = true
                }
            }

        }
    )
    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text("Verify Email",style= MaterialTheme.typography.titleMedium) },
            text = { Text("Please verify using the link sent to your email. (check Spam as well)") },

            confirmButton = {
                Row {

                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().currentUser
                                ?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    showVerifyDialog = false
                                }
                        }
                    ) {
                        Text(
                            "Resend",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().currentUser
                                ?.reload()
                                ?.addOnSuccessListener {
                                    if (FirebaseAuth.getInstance().currentUser?.isEmailVerified == true) {
                                        showVerifyDialog = false
                                    }
                                }
                        }
                    ) {
                        Text(
                            "I Verified",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },

            dismissButton = {
                OutlinedButton(onClick = {

                    FirebaseAuth.getInstance().currentUser
                        ?.delete()
                        ?.addOnCompleteListener {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("signup")
                        }

                }) {
                    Text("Cancel Signup",style= MaterialTheme.typography.labelSmall)
                }
            }
        )
    }
}