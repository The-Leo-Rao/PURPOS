package com.example.purpos.screens

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import android.net.Uri
import android.provider.MediaStore
import android.widget.Space
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.material.icons.filled.Delete
import java.io.ByteArrayOutputStream

fun uploadProfileImage(
    uri: Uri,
    onSuccess: (String) -> Unit,
    onLoading: (Boolean) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val uid = user.uid

    val storageRef = FirebaseStorage.getInstance()
        .reference
        .child("profile_images/$uid.jpg")

    onLoading(true)

    storageRef.putFile(uri)
        .continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
            storageRef.downloadUrl
        }
        .addOnSuccessListener { downloadUri ->

            val url = downloadUri.toString()

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(mapOf("imageUrl" to url), SetOptions.merge())

            onSuccess(url)
            onLoading(false)
        }
        .addOnFailureListener {
            onLoading(false)
        }
}

fun compressImage(context: Context, uri: Uri): ByteArray {
    val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    val resized = Bitmap.createScaledBitmap(bitmap, 800, 800, true)
    val stream = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    return stream.toByteArray()
}
fun uploadGalleryImage(context: Context, uri: Uri, onComplete: (String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val uid = user.uid
    val fileName = "gallery_${System.currentTimeMillis()}.jpg"
    val storageRef = FirebaseStorage.getInstance()
        .reference
        .child("gallery_images/$uid/$fileName")
    val compressed = compressImage(context, uri)
    storageRef.putBytes(compressed)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val url = downloadUri.toString()
                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection("users").document(uid)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val current = snapshot.get("galleryUrls") as? List<String> ?: emptyList()
                    transaction.update(docRef, "galleryUrls", current + url)
                }.addOnSuccessListener {
                    onComplete(url)
                }
            }
        }
}


fun deleteGalleryImage(url: String, onComplete: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val uid = user.uid
    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)

    storageRef.delete().addOnSuccessListener {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val current = snapshot.get("galleryUrls") as? List<String> ?: emptyList()
            transaction.update(docRef, "galleryUrls", current - url)
        }.addOnSuccessListener {
            onComplete()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {

    Scaffold(

        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = false
                                }
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    var showDialog by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showDialog = true },
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
                                    Text(
                                        text="Close",
                                        color= MaterialTheme.colorScheme.secondary,
                                        style= MaterialTheme.typography.bodyMedium)
                                }
                            },
                            title = {
                                Text(
                                    text = "Data Security & Privacy",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            text = {
                                val scrollState = rememberScrollState()
                                var bod =
                                    "PURPOS securely stores all user data using our trusted cloud infrastructure, designed with standard security practices to help keep your information protected and reliable. All communication between the app and our servers is handled through secure APIs with encrypted connections, reducing the risk of unauthorized access or data leaks during transmission.\n\n We take data privacy and security seriously, and our systems are built to ensure that your information is stored safely, accessed only when necessary, and managed responsibly. Regular security measures and modern backend technologies are used to maintain the integrity and confidentiality of your data.\n\n With us, you can rest assured that we will manage your operations seamlessly, helping you achieve your PURPOS."
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
            )
        }){padding ->

        val auth = FirebaseAuth.getInstance()

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var name by remember { mutableStateOf("") }
            var locality by remember { mutableStateOf("") }
            var city by remember { mutableStateOf("") }
            var state by remember { mutableStateOf("") }
            var sector by remember { mutableStateOf("") }
            var purposeStatement by remember { mutableStateOf("") }
            val user = FirebaseAuth.getInstance().currentUser
            val db = FirebaseFirestore.getInstance()
            var imageUrl by remember { mutableStateOf("") }
            var imageUri by remember { mutableStateOf<Uri?>(null) }
            var isUploading by remember { mutableStateOf(false) }
            val imagePickerLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->

                    uri?.let {
                        imageUri = it   // instant preview

                        uploadProfileImage(
                            uri = it,
                            onSuccess = { url ->
                                imageUrl = url
                                imageUri = null
                            },
                            onLoading = {
                                isUploading = it
                            }
                        )
                    }
                }
            var galleryUrls by remember { mutableStateOf(listOf<String>()) }
            var showGalleryDialog by remember { mutableStateOf(false) }
            var imageToDelete by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            val galleryPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    uploadGalleryImage(context, it) { newUrl ->
                        galleryUrls = galleryUrls + newUrl
                        isUploading = false
                    }
                }
            }

            LaunchedEffect(Unit) {
                user?.uid?.let { uid ->
                    db.collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null) {
                                Log.d("Firestore", "Data: ${document.data}")

                                name = document.getString("name") ?: ""
                                locality = document.getString("locality") ?: ""
                                city=document.getString("city")?:""
                                state=document.getString("state")?:""
                                sector = document.getString("sector") ?: ""
                                imageUrl = document.getString("imageUrl") ?: ""
                                purposeStatement = document.getString("purpose") ?: ""
                                galleryUrls = (document.get("galleryUrls") as? List<String>) ?: emptyList()
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

            Spacer(modifier = Modifier.height(20.dp))

            // Profile photo
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        imagePickerLauncher.launch("image/*")
                    }
            ) {

                when {
                    imageUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                        )
                    }

                    imageUrl.isNotEmpty() -> {
                        Image(
                            painter = rememberAsyncImagePainter(imageUrl),
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                        )
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp),
                        strokeWidth = 3.dp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change Photo",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            //Name
            Card(Modifier.fillMaxWidth(0.85f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 17.dp, end = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showDialog by remember { mutableStateOf(false) }
                    var editedName by remember { mutableStateOf(name) }

                    Text(
                        text = "Name: $name",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                        modifier=Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            editedName = name
                            showDialog = true
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
                                    updateSingleField("name", editedName)
                                    name = editedName
                                }) {
                                    Text(
                                        text = "OK",
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Edit Name",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "Enter new Name: ",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge,
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            //Sector
            Card(Modifier.fillMaxWidth(0.85f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 17.dp, end = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showDialog by remember { mutableStateOf(false) }
                    var editedsector by remember { mutableStateOf(sector) }

                    Text(
                        text = "Sector: $sector",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                        modifier=Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            editedsector = sector
                            showDialog = true
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
                                    updateSingleField("sector", editedsector)
                                    sector = editedsector
                                }) {
                                    Text(
                                        text = "OK",
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Edit sector",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "Enter new Sector: ",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge,
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
                                            listOf(
                                                "Health",
                                                "Education",
                                                "Environmental",
                                                "Poverty Alleviation",
                                                "women empowerment",
                                                "Disaster Relief",
                                                "Other"
                                            ).forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        editedsector = option
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
            }

            Spacer(modifier= Modifier.height(16.dp))

            //LocationCard
            Card(Modifier.fillMaxWidth(0.85f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 17.dp, end = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showDialog by remember { mutableStateOf(false)}
                    var editedloc by remember { mutableStateOf(locality) }
                    var editcity by remember { mutableStateOf(city) }
                    var editstate by remember { mutableStateOf(state) }
                    var citypredictions by remember { mutableStateOf(listOf<String>()) }
                    var statepredictions by remember { mutableStateOf(listOf<String>()) }
                    val context = LocalContext.current
                    val placesClient = remember { Places.createClient(context) }
                    var cityquery by remember { mutableStateOf("") }
                    var statequery by remember {mutableStateOf("")}

                    Text(
                        text = "location: $locality"+", "+"$city"+", "+"$state",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Left,
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    IconButton(
                        onClick = {
                            editedloc = locality
                            editcity=city
                            editstate=state
                            cityquery = city
                            statequery = state
                            showDialog = true
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
                                    updateSingleField("locality", editedloc)
                                    updateSingleField("city",editcity)
                                    updateSingleField("state",editstate)
                                    locality = editedloc
                                    city=editcity
                                    state=editstate
                                }) {
                                    Text(
                                        text = "OK",
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Edit location",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            },
                            text = {
                                Column {

                                    Text(
                                        text = "Enter new locality: ",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Spacer(modifier=Modifier.height(16.dp))
                                    TextField(
                                        value = editedloc,
                                        onValueChange = { editedloc = it },
                                        placeholder = { Text("locality") })


                                    Text(
                                        text = "Enter new city: ",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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
                                                    cityquery = it
                                                    editcity = it
                                                },
                                                label = { Text("City") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            val focusManager = LocalFocusManager.current
                                            DropdownMenu(
                                                expanded = citypredictions.isNotEmpty(),
                                                onDismissRequest = { citypredictions = emptyList() }
                                            ) {
                                                citypredictions.forEach {
                                                    DropdownMenuItem(
                                                        text = { Text(it) },
                                                        onClick = {
                                                            focusManager.clearFocus()
                                                            cityquery = it
                                                            editcity = it
                                                            citypredictions = emptyList()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }


                                    Text(
                                        text = "Enter new State: ",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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
                                                    statequery = it
                                                    editstate = it
                                                },
                                                label = { Text("State") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            val focusManager = LocalFocusManager.current
                                            DropdownMenu(
                                                expanded = statepredictions.isNotEmpty(),
                                                onDismissRequest = { statepredictions = emptyList() }
                                            ) {
                                                statepredictions.forEach {
                                                    DropdownMenuItem(
                                                        text = { Text(it) },
                                                        onClick = {
                                                            focusManager.clearFocus()
                                                            statequery = it
                                                            editstate = it
                                                            statepredictions = emptyList()
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
            }

            Spacer(modifier= Modifier.height(16.dp))

            //Purpose statement
            Card(Modifier
                    .fillMaxWidth(0.85f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 17.dp, end = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,

                ) {
                    var showDialog by remember { mutableStateOf(false) }
                    var purposeSt by remember { mutableStateOf(purposeStatement) }

                    Text(
                        text = "Purpose statement: $purposeStatement",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            purposeSt = purposeStatement
                            showDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit purpose statement")
                    }
                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            confirmButton = {
                                Button(onClick = {
                                    showDialog = false
                                    updateSingleField("purpose", purposeSt)
                                    purposeStatement = purposeSt
                                }) {
                                    Text(
                                        text = "OK",
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Edit purpose statement",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "Enter new purpose statement: ",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextField(
                                        value = purposeSt,
                                        onValueChange = { purposeSt = it },
                                        placeholder = { Text("purpose Statement") })
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gallery Card
            Card(Modifier
                .fillMaxWidth(0.85f)
                .clickable { showGalleryDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gallery (${galleryUrls.size})",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Gallery",
                    )
                }
            }

            if (showGalleryDialog) {
                AlertDialog(
                    onDismissRequest = { showGalleryDialog = false },
                    confirmButton = {
                        Button(onClick = { showGalleryDialog = false }) {
                            Text(
                                text = "Done",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Gallery",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Button(
                                onClick = { galleryPickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "+ Add Image",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (galleryUrls.isEmpty()) {
                                Text(
                                    text = "No images yet. Add some! (Give it a second to load)",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                galleryUrls.forEach { url ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(url),
                                            contentDescription = "Gallery Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = { imageToDelete = url }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Image",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            if (imageToDelete != null) {
                AlertDialog(
                    onDismissRequest = { imageToDelete = null },
                    confirmButton = {
                        Button(onClick = {
                            val urlToRemove = imageToDelete!!
                            deleteGalleryImage(urlToRemove) {
                                galleryUrls = galleryUrls - urlToRemove
                            }
                            imageToDelete = null
                        }) {
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    dismissButton = {
                        Button(onClick = { imageToDelete = null }) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Delete Image?",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    text = {
                        Text(
                            text = "This will permanently delete the image from your gallery.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }

            Spacer(modifier= Modifier.height(24.dp))

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
}