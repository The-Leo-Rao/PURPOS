package com.example.purpos.screens

import android.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DatasetLinked
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

data class UserPost(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val city: String = "",
    val state: String="",
    val time: Long = 0L
)
data class SuccessCase(
    val name: String = "",
    val email: String = "",
    val phone: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(navController: NavController) {
    val secondApp = FirebaseApp.getInstance("VOLUNTEER_APP")
    val volunteerDb = FirebaseFirestore.getInstance(secondApp)
    val currentNgoId = FirebaseAuth.getInstance().currentUser?.uid
    var volunteers by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var cases by remember { mutableStateOf(listOf<SuccessCase>()) }
    var showDialog by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    var statedata by remember { mutableStateOf("")}
    var citydata by remember { mutableStateOf("") }

    LaunchedEffect(user?.uid) {
        user?.let {
            db.collection("users")
                .document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    citydata = document.getString("city") ?: ""
                    statedata = document.getString("state") ?: ""
                }
        }
    }

    LaunchedEffect(Unit) {
        volunteerDb.collection("addressedproblems")
            .whereEqualTo("ngo_id", currentNgoId)
            .get()
            .addOnSuccessListener { result ->
                volunteers = result.documents
                loading = false
            }
            .addOnFailureListener {
                loading = false
            }
    }

    val posts = remember { mutableStateListOf<UserPost>() }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var showSecondDialog by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<UserPost?>(null) }
    var message by remember { mutableStateOf("") }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add a Post",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "AddPost"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("home")
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentRoute == "publish",
                    onClick = {
                        navController.navigate("publish") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Publish, contentDescription = "Publish") },
                    label = { Text("Publish") }
                )
                NavigationBarItem(
                    selected = currentRoute == "My Data",
                    onClick = {
                        navController.navigate("My Data") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.DatasetLinked, contentDescription = "My Data") },
                    label = { Text("My Data") }
                )
            }
        }
    ) { padding ->


        LaunchedEffect(user?.uid) {
            user?.let { u ->
                db.collection("posts")
                    .whereEqualTo("NGOid", u.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        posts.clear()
                        for (doc in snapshot.documents) {
                            posts.add(
                                UserPost(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    body = doc.getString("body") ?: "",
                                    state=doc.getString("state")?: "",
                                    city = doc.getString("city") ?: "",
                                    time = doc.getLong("time") ?: 0L
                                )
                            )
                        }
                    }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.CenterHorizontally),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your Posts",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn {
                items(posts) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                selectedPost = post
                                showSecondDialog = true
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = post.title,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = post.body,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Create Post", style= MaterialTheme.typography.bodyMedium) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Post Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text("Post Body") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(message,color= MaterialTheme.colorScheme.error,style= MaterialTheme.typography.labelSmall)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (title.isBlank() || body.isBlank()) {
                                message = "Title and Body cannot be empty"
                                return@Button
                            }
                            message = ""
                            val cleanTitle = title.trim()
                            val cleanBody = body.trim()
                            val uid = user?.uid ?: return@Button
                            val newTime = System.currentTimeMillis()

                            // Build the Firestore document
                            val postData = hashMapOf(
                                "NGOid" to uid,
                                "title" to cleanTitle,
                                "body" to cleanBody,
                                "state" to statedata,
                                "city" to citydata,
                                "time" to newTime
                            )

                            db.collection("posts")
                                .add(postData)
                                .addOnSuccessListener { docRef ->
                                    posts.add(
                                        UserPost(
                                            id = docRef.id,
                                            title = title,
                                            body = body,
                                            state=statedata,
                                            city = citydata,
                                            time = newTime
                                        )
                                    )
                                }

                            title = ""
                            body = ""
                            showDialog = false
                        }
                    ) {
                        Text(
                            text = "Post",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            )
        }

        if (showSecondDialog && selectedPost != null) {
            val scrollState = rememberScrollState()
            AlertDialog(
                onDismissRequest = { showSecondDialog = false },
                title = {
                    Text(
                        text = "Delete/End your post",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                text = {
                    when {
                        loading -> {
                            CircularProgressIndicator()
                        }

                        volunteers.isEmpty() -> {
                            Text("No volunteers matched yet.\nDon't Worry! We're at it", textAlign = TextAlign.Left)
                        }

                        else -> {
                            val matchedVolunteers = volunteers.filter { doc ->
                                doc.getLong("post_id") == selectedPost?.time
                            }
                            LazyColumn {
                                items(matchedVolunteers) { doc ->

                                    val name = doc.getString("name") ?: ""
                                    val city = doc.getString("city") ?: ""
                                    val sector = doc.getString("sector") ?: ""
                                    val phone = doc.getString("phone") ?: ""

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(name, fontWeight = FontWeight.Bold)
                                                Text(city)
                                                Text(sector)
                                                Text(phone)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
//                    Column(
//                        modifier = Modifier
//                            .heightIn(max = 300.dp)
//                            .verticalScroll(scrollState)
//                    ) {
//                        Text("List of Volunteers")
//                        Card(modifier = Modifier.fillMaxWidth()) {
//                            Text("Hi",textAlign = TextAlign.Start)
//                        }
//                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedPost?.let { post ->
                                db.collection("posts").document(post.id).delete()
                                posts.remove(post)
                            }
                            showSecondDialog = false
                        }
                    ) {
                        Text(
                            text = "Completed",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            selectedPost?.let { post ->
                                db.collection("posts").document(post.id).delete()
                                posts.remove(post)
                            }
                            showSecondDialog = false
                        }
                    ) {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            )
        }
    }
}