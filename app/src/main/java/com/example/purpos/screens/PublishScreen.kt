package com.example.purpos.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class UserPost(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val location: String = "",
    val time: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(navController: NavController) {
    var showDialog by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var locationdata by remember { mutableStateOf("") }

    LaunchedEffect(user?.uid) {
        user?.let {
            db.collection("users")
                .document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    locationdata = document.getString("location") ?: ""
                }
        }
    }

    val posts = remember { mutableStateListOf<UserPost>() }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var showSecondDialog by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<UserPost?>(null) }
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
                    selected = currentRoute == "analytics",
                    onClick = {
                        navController.navigate("Analytics") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
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
                                    location = doc.getString("location") ?: "",   // add this
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
                title = { Text("Create Post") },
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
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val uid = user?.uid ?: return@Button
                            val newTime = System.currentTimeMillis()

                            // Build the Firestore document
                            val postData = hashMapOf(
                                "NGOid" to uid,
                                "title" to title,
                                "body" to body,
                                "location" to locationdata,
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
                                            location = locationdata,
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
            AlertDialog(
                onDismissRequest = { showSecondDialog = false },
                title = {
                    Text(
                        text = "Delete/End your post",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                text = {},
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