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
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

fun savePostToJson(
    context: Context,
    title: String,
    body: String,
    location: String,
    NGOid: String?
) {
    val fileName = "posts.json"

    val file = context.getFileStreamPath(fileName)

    val jsonArray = if (file.exists()) {
        val text = file.readText()
        JSONArray(text)
    } else {
        JSONArray()
    }

    val newPost = JSONObject().apply {
        put("NGOid",NGOid)
        put("title", title)
        put("body", body)
        put("location", location)
        put("time", System.currentTimeMillis())
    }

    jsonArray.put(newPost)

    context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
        output.write(jsonArray.toString(4).toByteArray())
    }
}

fun loadPostsFromJson(context: Context): List<UserPost> {
    val fileName = "posts.json"
    val file = context.getFileStreamPath(fileName)

    if (!file.exists()) return emptyList()

    val text = file.readText()

    val jsonArray = JSONArray(text)

    val loadedPosts = mutableListOf<UserPost>()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)

        loadedPosts.add(
            UserPost(
                title = obj.getString("title"),
                body = obj.getString("body"),
                time = obj.getLong("time")
            )
        )
    }

    return loadedPosts
}

fun deletePostFromJson(context: Context, selectedTime: Long) {
    val file = context.getFileStreamPath("posts.json")
    if (!file.exists()) return

    val jsonArray = JSONArray(file.readText())
    val newArray = JSONArray()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)

        if (obj.getLong("time") != selectedTime) {
            newArray.put(obj)
        }
    }

    context.openFileOutput("posts.json", Context.MODE_PRIVATE).use {
        it.write(newArray.toString(4).toByteArray())
    }
}


data class UserPost(
    val title: String,
    val body: String,
    val time: Long = 0L
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(navController: NavController) {
    var showDialog by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    var locationdata by remember { mutableStateOf("") }
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    user?.let {
        db.collection("users")
            .document(it.uid)
            .get()
            .addOnSuccessListener { document ->
                locationdata = document.getString("location") ?: ""
            }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text="Add a Post",
                        color= MaterialTheme.colorScheme.primary,
                        style= MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    val focusManager= LocalFocusManager.current
                    IconButton(onClick = {showDialog = true}) {
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
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home"){
                        popUpTo("home")
                        launchSingleTop=true
                        restoreState=true
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
                    selected = currentRoute == "analytics",
                    onClick = { navController.navigate("Analytics"){
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop=true
                        restoreState=true
                    } },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var title by remember { mutableStateOf("") }
            var body by remember { mutableStateOf("") }
            var showSecondDialog by remember { mutableStateOf(false) }
            var timeContext by remember { mutableStateOf(0L) }
            val context = LocalContext.current

            val posts = remember {
                mutableStateListOf<UserPost>().apply {
                    addAll(loadPostsFromJson(context))
                }
            }

            Spacer(modifier= Modifier.height(20.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.85f).align(Alignment.CenterHorizontally),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier= Modifier.height(10.dp))
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
                            .clickable{timeContext = post.time
                                showSecondDialog=true}
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = post.title,
                                color=MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text=post.body,
                                color= MaterialTheme.colorScheme.secondary,
                                style= MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (showDialog) {

                AlertDialog(
                    onDismissRequest = { showDialog = false },

                    title = {
                        Text("Create Post")
                    },

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
                        val context = LocalContext.current
                        val uid = auth.currentUser?.uid
                        Button(
                            onClick = {

                                posts.add(
                                    UserPost(
                                        title = title,
                                        body = body,
                                        time = System.currentTimeMillis()
                                    )
                                )

                                savePostToJson(context,title,body,locationdata,uid)

                                title = ""
                                body = ""
                                showDialog = false
                            }
                        ) {
                            Text(
                                text="Post",
                                color= MaterialTheme.colorScheme.secondary,
                                style= MaterialTheme.typography.titleMedium)
                        }
                    },

                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDialog = false
                            }
                        ) {
                            Text(
                                text="Cancel",
                                color= MaterialTheme.colorScheme.primary,
                                style= MaterialTheme.typography.bodyLarge)
                        }
                    }
                )
            }

            if(showSecondDialog){
                AlertDialog(
                    onDismissRequest ={showSecondDialog=false},
                    title={Text(
                        text="Delete/End you post",
                        color= MaterialTheme.colorScheme.primary,
                        style= MaterialTheme.typography.bodyLarge
                    )
                          },
                    text={},
                    confirmButton = {
                        TextButton(
                            onClick = {
                                posts.removeAll { post ->
                                    post.time == timeContext
                                }
                                deletePostFromJson(context, timeContext)
                                showSecondDialog=false
                            }
                        ) {
                            Text(
                                text = "Completed",
                                color = MaterialTheme.colorScheme.primary,
                                style= MaterialTheme.typography.bodyLarge
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                posts.removeAll { post ->
                                    post.time == timeContext
                                }
                                deletePostFromJson(context, timeContext)
                                showSecondDialog=false
                            }
                        ) {
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.error,
                                style= MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                )
            }
        }


    }
}