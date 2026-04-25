package com.example.purpos.screens


import android.R
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import com.example.purpos.ApiService2
import java.io.File
import androidx.core.content.FileProvider


private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .writeTimeout(120, TimeUnit.SECONDS)
    .build()

private val api2: ApiService2 by lazy {
    Retrofit.Builder()
        .baseUrl("https://ngo-backend-1at7.onrender.com/")
        .client(client)
        .addConverterFactory(
            ScalarsConverterFactory.create()
        )
        .build()
        .create(ApiService2::class.java)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController){

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var fileList by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var apiloading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (uid != null) {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("users")
                .child(uid)
                .child("csvs")

            storageRef.listAll()
                .addOnSuccessListener { result ->
                    fileList = result.items
                        .filter { it.name.endsWith(".csv") }
                        .map { it.name }

                    loading = false
                }
                .addOnFailureListener {
                    loading = false
                }
        } else {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("My Data") {
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
            )
        }
    ) { padding ->

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            fileList.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No CSV files found")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Text("Your file analytics ",color= MaterialTheme.colorScheme.primary, style= MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(2.dp))
                        Text("Powered by AI",color= MaterialTheme.colorScheme.primary, style= MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(fileList) { fileName ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    apiloading=true
                                    val fileRef = FirebaseStorage.getInstance().reference
                                        .child("users")
                                        .child(uid!!)
                                        .child("csvs")
                                        .child(fileName)

                                    fileRef.getBytes(10 * 1024 * 1024)
                                        .addOnSuccessListener { bytes ->

                                            scope.launch(Dispatchers.IO) {

                                                try {

                                                    val requestBody = bytes.toRequestBody(
                                                        "text/csv".toMediaTypeOrNull()
                                                    )

                                                    val part =
                                                        MultipartBody.Part.createFormData(
                                                            "file",
                                                            fileName,
                                                            requestBody
                                                        )

                                                    val response =
                                                        api2.submitData(part)

                                                    if (response.isSuccessful) {

                                                        val url =
                                                            response.body().orEmpty()

                                                        withContext(Dispatchers.Main) {
                                                            apiloading = false

                                                            val html = response.body().orEmpty()

                                                            val file = File(context.cacheDir, "dashboard.html")
                                                            file.writeText(html)

                                                            val uri = FileProvider.getUriForFile(
                                                                context,
                                                                "${context.packageName}.provider",
                                                                file
                                                            )

                                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                setDataAndType(uri, "text/html")
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }

                                                            context.startActivity(intent)
                                                        }
                                                    }

                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(fileName.removeSuffix(".csv"))
                            }
                        }
                    }
                }
            }
        }

        if (apiloading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Card {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Generating data analytics...\n(Ensure your data has minimal discrepancies for faster analysis)")
                    }
                }
            }
        }
    }
}