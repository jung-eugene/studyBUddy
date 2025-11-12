package com.example.studybuddy

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
//import com.example.studybuddy.profile.screen.CalendarScreen
import com.example.studybuddy.profile.screen.HomeScreen
import com.example.studybuddy.profile.screen.LoginScreen
//import com.example.studybuddy.profile.screen.MatchesScreen
import com.example.studybuddy.profile.screen.ProfileScreen
import com.example.studybuddy.profile.screen.ProfileSetupScreen


// Defines which screens exist and how to get between them
@Composable
fun StudyBuddyNavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Routes.Login.route) {
        composable(Routes.Login.route) { LoginScreen(navController) }
        composable(Routes.ProfileSetup.route) { ProfileSetupScreen(navController) }
        composable(Routes.Home.route) { HomeScreen(navController) }
//        composable(Routes.Matches.route) { MatchesScreen(navController) }
//        composable(Routes.Calendar.route) { CalendarScreen(navController) }
        composable(Routes.Profile.route) { ProfileScreen(navController) }
    }
}

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Home : Routes("home")
//    object Matches : Routes("matches")
//    object Calendar : Routes("calendar")
    object Profile : Routes("profile")
    object ProfileSetup : Routes("profile_setup")
}