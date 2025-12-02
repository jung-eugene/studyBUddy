package com.example.studybuddy

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.studybuddy.profile.screen.*
@Composable
fun StudyBuddyNavGraph(
    navController: NavHostController,
    userVM: UserViewModel,
    authVM: AuthViewModel,
    setupVM: ProfileSetupViewModel,
    calendarVM: CalendarViewModel,
    homeVM: HomeViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route //Routes.Login.route
    ) {
        // SPLASH
        composable(Routes.Splash.route) {
            SplashScreen(navController = navController)
        }

        // LOGIN
        composable(Routes.Login.route) {
            LoginScreen(
                navController = navController,
                authVM = authVM,
                userVM = userVM
            )
        }

        // PROFILE SETUP
        composable(Routes.ProfileSetup.route) {
            ProfileSetupScreen(
                navController = navController,
                viewModel = setupVM
            )
        }

        // HOME
        composable(Routes.Home.route) {
            HomeScreen(
                navController = navController,
                userVM = userVM,
                homeVM = homeVM
            )
        }

        // MATCHES
        composable(Routes.Matches.route) {
            MatchesScreen(
                navController = navController,
                userVM = userVM
            )
        }

        // CALENDAR
        composable(Routes.Calendar.route) {
            CalendarScreen(
                navController = navController,
                calendarViewModel = calendarVM
            )
        }

        // PROFILE
        composable(Routes.Profile.route) {
            ProfileScreen(
                navController = navController,
                userVM = userVM,
                authVM = authVM
            )
        }

        // EDIT PROFILE
        composable(Routes.Edit.route) {
            EditProfileScreen(
                navController = navController,
                userVM = userVM
            )
        }
    }
}


sealed class Routes(val route: String) {
    object Edit : Routes("editProfile")
    object Login : Routes("login")
    object Home : Routes("home")
    object Matches : Routes("matches")
    object Calendar : Routes("calendar")
    object Profile : Routes("profile")
    object ProfileSetup : Routes("profile_setup")
    object Splash : Routes("splash")
}
