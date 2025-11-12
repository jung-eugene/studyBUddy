package com.example.studybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.studybuddy.ui.theme.StudybuddyTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Main entry point of studyBUddy.
 * Handles global theming by loading dark mode preference from Firestore.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val userVM: UserViewModel = viewModel()
            val auth = FirebaseAuth.getInstance()
            val scope = rememberCoroutineScope()

            // Reactive theme state
            var darkMode by remember { mutableStateOf(false) }

            // Fetch dark mode preference when user logs in
            LaunchedEffect(auth.currentUser) {
                auth.currentUser?.uid?.let { uid ->
                    scope.launch {
                        darkMode = userVM.getDarkMode(uid)
                    }
                }
            }

            // Apply Material theme
            StudybuddyTheme(darkTheme = darkMode) {
                StudyBuddyNavGraph(navController)
            }
        }
    }
}
