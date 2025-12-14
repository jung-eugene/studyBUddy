package com.example.studybuddy

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavBar(navController: NavHostController) {

    val items = listOf(
        Routes.Home,
        Routes.Matches,
        Routes.Calendar,
        Routes.Profile
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Screens that should NOT show the bottom bar
    val hideOnRoutes = listOf(
        Routes.Login.route,
        Routes.VerifyEmail.route,
        Routes.ProfileSetup.route,
        Routes.Edit.route,
        Routes.Splash.route
    )
    if (currentRoute in hideOnRoutes) return

    val color = MaterialTheme.colorScheme

    Column {
        // Top divider line (very subtle)
        HorizontalDivider(
            color = Color.Gray,
            thickness = 1.dp
        )

        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp, // Remove tint/shadows
        ) {

            items.forEach { screen ->

                val icon = when (screen) {
                    Routes.Home -> Icons.Default.Home
                    Routes.Matches -> Icons.Outlined.ChatBubbleOutline
                    Routes.Calendar -> Icons.Default.CalendarToday
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

                val selected = currentRoute == screen.route

                NavigationBarItem(
                    selected = selected,
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
                            tint = if (selected) color.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            label,
                            color = if (selected) color.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}