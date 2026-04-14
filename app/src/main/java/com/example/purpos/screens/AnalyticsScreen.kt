package com.example.purpos.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
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
import androidx.compose.ui.unit.dp
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

interface ApiService {
    @Multipart
    @POST("api/extract")
    suspend fun extractData(
        @Part image: MultipartBody.Part,
        @Part reference_file: MultipartBody.Part
    ): retrofit2.Response<String>
}

private val api: ApiService by lazy {
    Retrofit.Builder()
        .baseUrl("https://ngo-backend-1at7.onrender.com/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

fun getAllCsvFiles(context: Context): List<String> =
    context.filesDir.listFiles()
        ?.filter { it.isFile && it.extension == "csv" }
        ?.map { it.nameWithoutExtension }
        ?.sorted()
        ?: emptyList()

fun renameCsv(context: Context, oldName: String, newName: String): Boolean {
    val oldFile = File(context.filesDir, "$oldName.csv")
    val newFile = File(context.filesDir, "$newName.csv")
    if (!oldFile.exists() || newFile.exists()) return false
    return oldFile.renameTo(newFile)
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
fun AnalyticsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var csvFiles by remember { mutableStateOf(getAllCsvFiles(context)) }
    var selectedCsv by remember { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }

    var showManageDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImage = uri
    }

    LaunchedEffect(selectedImage, selectedCsv) {
        val imageUri = selectedImage
        val csv = selectedCsv
        if (imageUri != null && csv != null) {
            loading = true
            scope.launch(Dispatchers.IO) {
                try {
                    val imagePart = uriToPart(context, imageUri, "image", "upload.jpg")
                    val csvFile = File(context.filesDir, "$csv.csv")
                    val csvBody = csvFile.asRequestBody("text/csv".toMediaType())
                    val refPart = MultipartBody.Part.createFormData("reference_file", csvFile.name, csvBody)
                    val response = api.extractData(imagePart, refPart)

                    message = if (response.isSuccessful) {
                        val result = response.body().orEmpty()
                        val obj = JSONObject(result)
                        val success =
                            obj.get("success").toString().toBoolean()
                        if (success) {
                            val newCsv = obj.getString("csv")
                            val targetFile =
                                File(context.filesDir, "$csv.csv")
                            val oldCsv =
                                if (targetFile.exists())
                                    targetFile.readText()
                                else ""
                            val mergedCsv =
                                if (oldCsv.isBlank()) {
                                    newCsv
                                } else {
                                    oldCsv.trim() + "\n" +
                                            newCsv.trim()
                                                .lines()
                                                .drop(1)
                                                .joinToString("\n")
                                }
                            targetFile.writeText(mergedCsv)
                            csvFiles = getAllCsvFiles(context)
                            "CSV merged successfully."
                        } else {
                            obj.optString(
                                "reason",
                                "Extraction failed."
                            )
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
                        text="Saved CSV Files",
                        color= MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButton(onClick = { csvFiles = getAllCsvFiles(context) }) {
                        Icon(Icons.Default.Sync, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val route = navController.currentBackStackEntryAsState().value?.destination?.route
                NavigationBarItem(selected = route == "home", onClick = { navController.navigate("home") }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = route == "publish", onClick = { navController.navigate("publish") }, icon = { Icon(Icons.Default.Publish, null) }, label = { Text("Publish") })
                NavigationBarItem(selected = route == "analytics", onClick = { }, icon = { Icon(Icons.Default.Analytics, null) }, label = { Text("Analytics") })
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
            title = { Text(selectedCsv!!) },
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
            confirmButton = {}
        )
    }

    if (showRenameDialog && selectedCsv != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(
                text="Rename CSV",
                color= MaterialTheme.colorScheme.primary) },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("File name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    renameCsv(context, selectedCsv!!, renameText.trim())
                    csvFiles = getAllCsvFiles(context)
                    showRenameDialog = false
                }) { Text(
                    text="Save",
                    color=MaterialTheme.colorScheme.secondary,
                    style=MaterialTheme.typography.bodyMedium)}
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
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
                    context.deleteFile("${selectedCsv}.csv")
                    csvFiles = getAllCsvFiles(context)
                    showDeleteDialog = false
                    showManageDialog = false
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

    if (message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { message = "" },
            title = { Text("Notice") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = { message = "" }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
