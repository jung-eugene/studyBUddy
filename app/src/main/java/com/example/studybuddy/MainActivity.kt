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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            // Create ViewModels once at the Activity level
            val userVM: UserViewModel = viewModel()
            val authVM: AuthViewModel = viewModel()

            // Observe UI state from UserViewModel
            val uiState by userVM.uiState.collectAsState()
            val darkMode = uiState.darkMode

            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid

            // Load user profile when user is logged in
            LaunchedEffect(uid) {
                if (uid != null) {
                    userVM.loadUserProfile(uid)
                }
            }

            val navController = rememberNavController()

            StudybuddyTheme(darkTheme = darkMode) {
                StudyBuddyNavGraph(
                    navController = navController,
                    userVM = userVM,
                    authVM = authVM
                )
            }
        }
    }
}