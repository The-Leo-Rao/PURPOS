package com.example.purpos.navigation
import androidx.navigation.compose.*
import com.example.purpos.screens.HomeScreen
import com.example.purpos.screens.LoginScreen
import com.example.purpos.screens.SignUpScreen
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.Composable
import androidx.navigation.NavHost
import com.example.purpos.screens.ForgotPasswordScreen
import com.example.purpos.screens.AddnInfoScreen
import com.example.purpos.screens.DataScreen
import com.example.purpos.screens.PublishScreen
import com.example.purpos.screens.ProfileScreen
import com.example.purpos.screens.AnalyticsScreen

@Composable
fun AppNavigation() {
    val navController=rememberNavController()
    val auth= FirebaseAuth.getInstance()
    val StartDest=if(auth.currentUser!=null)"home" else "login"
    NavHost(
        navController=navController,
        startDestination = StartDest
    ){
        composable("login"){LoginScreen(navController)}
        composable("signup"){SignUpScreen(navController)}
        composable("home"){HomeScreen(navController)}
        composable("forgotp"){ ForgotPasswordScreen(navController) }
        composable("addn"){ AddnInfoScreen(navController) }
        composable("My Data"){DataScreen(navController)}
        composable("publish"){PublishScreen(navController)}
        composable("profile"){ProfileScreen(navController)}
        composable("analytics"){AnalyticsScreen(navController)}
    }
}