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

/**
 * Main entry point of studyBUddy.
 * Handles global theming by loading dark mode preference from Firestore.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userVM: UserViewModel = viewModel()
            val darkMode by userVM.darkMode.collectAsState()

            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid

            LaunchedEffect(uid) {
                uid?.let { userVM.getDarkMode(it) }  // fetch user setting on launch
            }

            val navController = rememberNavController()

            StudybuddyTheme(darkTheme = darkMode) {
                StudyBuddyNavGraph(navController = navController)
            }
        }
    }
}
