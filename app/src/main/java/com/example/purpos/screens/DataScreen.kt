package com.example.purpos.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DatasetLinked
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import org.json.JSONObject
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.window.Dialog
import com.google.firebase.firestore.FirebaseFirestore

//Photo upload api service
interface ApiService {
    @Multipart
    @POST("api/extract")
    suspend fun extractData(
        @Part image: MultipartBody.Part,
        @Part reference_file: MultipartBody.Part
    ): retrofit2.Response<String>
}

private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .writeTimeout(120, TimeUnit.SECONDS)
    .build()



fun userStoragePath(): String {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    return "users/$uid/csvs"
}

suspend fun getAllCsvFilesFromFirebase(): List<String> {
    val storageRef = FirebaseStorage.getInstance().reference
        .child(userStoragePath())
    return try {
        storageRef.listAll().await()
            .items
            .map { it.name.removeSuffix(".csv") }
            .sorted()
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun downloadCsv(name: String): String {
    val ref = FirebaseStorage.getInstance().reference
        .child("${userStoragePath()}/$name.csv")
    val bytes = ref.getBytes(10 * 1024 * 1024).await()
    return String(bytes)
}

suspend fun uploadCsv(name: String, content: String) {
    val ref = FirebaseStorage.getInstance().reference
        .child("${userStoragePath()}/$name.csv")
    ref.putBytes(content.toByteArray()).await()
}

suspend fun renameCsvOnFirebase(oldName: String, newName: String): Boolean {
    return try {
        val content = downloadCsv(oldName)
        uploadCsv(newName, content)
        FirebaseStorage.getInstance().reference
            .child("${userStoragePath()}/$oldName.csv")
            .delete().await()
        true
    } catch (e: Exception) { false }
}

suspend fun deleteCsvFromFirebase(name: String) {
    FirebaseStorage.getInstance().reference
        .child("${userStoragePath()}/$name.csv")
        .delete().await()
}

private val api: ApiService by lazy {
    Retrofit.Builder()
        .baseUrl("https://ngo-backend-1at7.onrender.com/")
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

fun uriToPart(context: Context, uri: Uri, partName: String, fileName: String): MultipartBody.Part {
    val file = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    val body = file.asRequestBody("image/*".toMediaType())
    return MultipartBody.Part.createFormData(partName, file.name, body)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var csvFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCsv by remember { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var fullCsvContent by remember { mutableStateOf("") }
    var loadingCsv by remember { mutableStateOf(false) }

    var showManageDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showComplete by remember {mutableStateOf(false)}
    var showEntry by remember {mutableStateOf(false)}
    var renameText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImage = uri
    }

    LaunchedEffect(Unit) {
        csvFiles = getAllCsvFilesFromFirebase()
    }

    LaunchedEffect(selectedImage, selectedCsv) {
        val imageUri = selectedImage
        val csv = selectedCsv
        if (imageUri != null && csv != null) {
            loading = true
            scope.launch(Dispatchers.IO) {
                try {
                    val imagePart = uriToPart(context, imageUri, "image", "upload.jpg")

                    // Download CSV from Firebase into a temp file for the API
                    val csvContent = downloadCsv(csv)
                    val tempCsvFile = File(context.cacheDir, "$csv.csv") // cacheDir, not filesDir
                    tempCsvFile.writeText(csvContent)

                    val csvBody = tempCsvFile.asRequestBody("text/csv".toMediaType())
                    val refPart = MultipartBody.Part.createFormData("reference_file", tempCsvFile.name, csvBody)

                    val response = api.extractData(imagePart, refPart)

                    message = if (response.isSuccessful) {
                        val result = response.body().orEmpty()
                        val obj = JSONObject(result)
                        val success = obj.get("success").toString().toBoolean()
                        if (success) {
                            val newCsv = obj.getString("csv")
                            val oldCsv = csvContent // reuse what we already downloaded
                            val mergedCsv = if (oldCsv.isBlank()) {
                                newCsv
                            } else {
                                oldCsv.trim() + "\n" +
                                        newCsv.trim().lines().drop(1).joinToString("\n")
                            }
                            uploadCsv(csv, mergedCsv)
                            csvFiles = getAllCsvFilesFromFirebase()
                            "CSV merged successfully."
                        } else {
                            obj.optString("reason", "Extraction failed.")
                        }
                    } else {
                        "Upload failed: ${response.code()}"
                    }
                } catch (e: Exception) {
                    message = "Error: ${e.message}"
                } finally {
                    loading = false
                    selectedImage = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text="Your Dataframes",
                    color= MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("analytics") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            restoreState = true
                        }
                    }) {
                        Icon(Icons.Default.Analytics, contentDescription = "Analytics")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val route = navController.currentBackStackEntryAsState().value?.destination?.route
                NavigationBarItem(selected = route == "home", onClick = { navController.navigate("home") }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = route == "publish", onClick = { navController.navigate("publish") }, icon = { Icon(Icons.Default.Publish, null) }, label = { Text("Publish") })
                NavigationBarItem(selected = route == "My Data", onClick = { }, icon = { Icon(Icons.Default.DatasetLinked, null) }, label = { Text("My Data") })
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(csvFiles) { fileName ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fileName, modifier = Modifier.weight(1f).clickable {
                                selectedCsv = fileName
                                showManageDialog = true
                            })
                            IconButton(onClick = {
                                selectedCsv = fileName
                                renameText = fileName
                                showRenameDialog = true
                            }) { Icon(Icons.Default.Edit, null) }
                        }
                    }
                }
            }

            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }

    if (showManageDialog && selectedCsv != null) {
        AlertDialog(
            onDismissRequest = { showManageDialog = false },
            title = { Text(selectedCsv!!,style= MaterialTheme.typography.bodyMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {showManageDialog = false
                        message = "Select an image to upload"
                        launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text="Upload data through image",
                            color= MaterialTheme.colorScheme.secondary,
                            style= MaterialTheme.typography.bodyMedium)
                    }

                    Button(onClick = {showEntry= true},
                        modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text="Add a single entry",
                            color= MaterialTheme.colorScheme.secondary,
                            style= MaterialTheme.typography.bodyMedium)
                    }

                    OutlinedButton(onClick = {
                        showManageDialog = false
                        showDeleteDialog = true
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text="Delete CSV",
                            color= MaterialTheme.colorScheme.error,
                            style= MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick={showComplete=true}){
                    Text("View full CSV file", color= MaterialTheme.colorScheme.primary,style= MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.Underline)
                }
            }
        )
    }

    if (showRenameDialog && selectedCsv != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(
                text="Rename CSV",
                style= MaterialTheme.typography.bodyMedium) },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("File name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        renameCsvOnFirebase(selectedCsv!!, renameText.trim())
                        csvFiles = getAllCsvFilesFromFirebase()
                        showRenameDialog = false
                    }
                }) { Text(
                    text="Save",
                    color=MaterialTheme.colorScheme.secondary,
                    style=MaterialTheme.typography.bodyMedium)}
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel",style= MaterialTheme.typography.bodyMedium) } }
        )
    }

    if (showDeleteDialog && selectedCsv != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(
                text="Delete ${selectedCsv}? ",
                color= MaterialTheme.colorScheme.error,
                style= MaterialTheme.typography.titleMedium) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        deleteCsvFromFirebase(selectedCsv!!)
                        csvFiles = getAllCsvFilesFromFirebase()
                        showDeleteDialog = false
                    }
                }) { Text(
                    text="Delete",
                    color= MaterialTheme.colorScheme.error,
                    style= MaterialTheme.typography.bodyMedium) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(
                text="Cancel",
                color= MaterialTheme.colorScheme.secondary,
                style= MaterialTheme.typography.bodyMedium) } }
        )
    }

    if (showComplete && selectedCsv != null){

        var selectedRowIndex by remember { mutableStateOf(-1) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        LaunchedEffect(selectedCsv) {
            loadingCsv = true
            try {
                fullCsvContent = downloadCsv(selectedCsv!!)
            } catch (e: Exception) {
                fullCsvContent = ""
            }
            loadingCsv = false
        }

        Dialog(
            onDismissRequest = { showComplete = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                shape = MaterialTheme.shapes.large
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    Text(
                        text = selectedCsv!!,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(12.dp))

                    if (loadingCsv) {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                    } else {

                        val lines = fullCsvContent.trim().lines()
                        val headers =
                            if (lines.isNotEmpty()) lines[0].split(",") else emptyList()

                        val rows =
                            if (lines.size > 1) lines.drop(1) else emptyList()

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {

                            itemsIndexed(rows) { index, row ->

                                val values = row.split(",")

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable {
                                            selectedRowIndex = index
                                            showDeleteDialog = true
                                        },
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {

                                    Column(
                                        modifier = Modifier.padding(14.dp)
                                    ) {

                                        headers.forEachIndexed { i, header ->

                                            val value =
                                                values.getOrNull(i)?.trim() ?: ""

                                            Text(
                                                text = "$header: $value",
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            Spacer(Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showComplete = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }

        if (showDeleteDialog && selectedRowIndex != -1) {

            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },

                title = {
                    Text("Delete Entry?")
                },

                text = {
                    Text("Do you want to delete this row from your Database?")
                },

                confirmButton = {
                    Button(
                        onClick = {

                            val lines = fullCsvContent.trim().lines().toMutableList()

                            if (lines.size > selectedRowIndex + 1) {
                                lines.removeAt(selectedRowIndex + 1)
                            }

                            val updatedCsv = lines.joinToString("\n")

                            val ref = FirebaseStorage.getInstance().reference
                                .child("${userStoragePath()}/${selectedCsv!!}.csv")

                            ref.putBytes(updatedCsv.toByteArray())
                                .addOnSuccessListener {
                                    fullCsvContent = updatedCsv
                                }
                                .addOnFailureListener {
                                    println(it.message)
                                }

                            showDeleteDialog = false
                        }
                    ) {
                        Text("Delete",color= MaterialTheme.colorScheme.tertiary)
                    }
                },

                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    if (showEntry && selectedCsv != null) {

        var csvContent by remember { mutableStateOf("") }
        var loadingEntry by remember { mutableStateOf(true) }

        LaunchedEffect(selectedCsv) {
            loadingEntry = true
            csvContent = downloadCsv(selectedCsv!!)
            loadingEntry = false
        }

        if (loadingEntry) {
            AlertDialog(
                onDismissRequest = { showEntry = false },
                title = { Text("Loading...") },
                text = { CircularProgressIndicator() },
                confirmButton = {}
            )
        } else {

            val lines = csvContent.trim().lines()
            val headers = if (lines.isNotEmpty()) lines.first().split(",") else emptyList()
            var message by remember { mutableStateOf("") }
            val fieldValues = remember(csvContent) {
                mutableStateMapOf<String, String>().apply {
                    headers.forEach { put(it.trim(), "") }
                }
            }

            AlertDialog(
                onDismissRequest = { showEntry = false },

                title = {
                    Text(
                        text = "Add Entry to $selectedCsv",
                        style = MaterialTheme.typography.titleMedium
                    )
                },

                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        headers.forEach { header ->

                            OutlinedTextField(
                                value = fieldValues[header.trim()] ?: "",
                                onValueChange = {
                                    fieldValues[header.trim()] = it
                                },
                                label = { Text(header.trim()) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(message,color=MaterialTheme.colorScheme.error,style= MaterialTheme.typography.labelSmall)
                    }
                },

                confirmButton = {
                    TextButton(
                        onClick = {
                            if (headers.any { fieldValues[it.trim()].isNullOrBlank() }) {
                                message = "All fields cannot be empty"
                                return@TextButton
                            }
                            message = ""
                            scope.launch {

                                val newRow = headers.joinToString(",") {
                                    fieldValues[it.trim()] ?: ""
                                }

                                val updatedCsv =
                                    csvContent.trimEnd() + "\n" + newRow

                                uploadCsv(selectedCsv!!, updatedCsv)

                                csvFiles = getAllCsvFilesFromFirebase()

                                showEntry = false
                                message = "Entry added successfully."
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },

                dismissButton = {
                    TextButton(onClick = { showEntry = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }


    if (message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { message = "" },
            title = { Text("Heads Up!", color= MaterialTheme.colorScheme.primary, style= MaterialTheme.typography.titleMedium) },
            text = { Text(message, style= MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = { message = "" }
                ) {
                    Text("OK",style= MaterialTheme.typography.bodyLarge)
                }
            }
        )
    }
}
