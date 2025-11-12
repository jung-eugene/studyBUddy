package com.example.studybuddy

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

// Displays the icons/buttons that tell the NavController where to go
@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        Routes.Home,
//        Routes.Matches,
//        Routes.Calendar,
        Routes.Profile
    )

    val current by navController.currentBackStackEntryAsState()
    val route = current?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            val icon = when (screen) {
                Routes.Home -> Icons.Default.Home
//                Routes.Matches -> Icons.Default.Favorite
//                Routes.Calendar -> Icons.Default.CalendarMonth
                Routes.Profile -> Icons.Default.Person
                else -> Icons.Default.Person
            }

            NavigationBarItem(
                selected = route == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = screen.route) },
                label = {
                    Text(
                        screen.route.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}
