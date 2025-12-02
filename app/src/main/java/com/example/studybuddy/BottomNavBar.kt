package com.example.studybuddy

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController) {

    // Only bottom bar items
    val items = listOf(
        Routes.Home,
        Routes.Matches,
        Routes.Calendar,
        Routes.Profile
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Hide navbar on some screens
    val hideOnRoutes = listOf(
        Routes.Login.route,
        Routes.ProfileSetup.route,
        Routes.Edit.route,
        Routes.Splash.route
    )
    if (currentRoute in hideOnRoutes) return

    NavigationBar {
        items.forEach { screen ->

            val icon = when (screen) {
                Routes.Home -> Icons.Default.Home
                Routes.Matches -> Icons.Outlined.ChatBubbleOutline
                Routes.Calendar -> Icons.Default.CalendarMonth
                Routes.Profile -> Icons.Default.Person
                else -> Icons.Default.Person
            }

            val label = when (screen) {
                Routes.Home -> "Home"
                Routes.Matches -> "Matches"
                Routes.Calendar -> "Calendar"
                Routes.Profile -> "Profile"
                else -> ""
            }

            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(Routes.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (currentRoute == screen.route)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        label,
                        color = if (currentRoute == screen.route)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )

        }
    }
}