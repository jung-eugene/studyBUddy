package com.example.studybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.studybuddy.ui.theme.StudybuddyTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            // --- Create ALL ViewModels once (app-level) ---
            val userVM: UserViewModel = viewModel()
            val authVM: AuthViewModel = viewModel()
            val setupVM: ProfileSetupViewModel = viewModel()
            val calendarVM: CalendarViewModel = viewModel()
            val homeVM: HomeViewModel = viewModel()

            // --- Watch darkMode state ---
            val uiState by userVM.uiState.collectAsState()
            val darkMode = uiState.darkMode

            // --- Load dark mode on startup ---
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid

            LaunchedEffect(uid) {
                uid?.let { id ->
                    // load user profile so uiState.darkMode gets updated
                    userVM.loadUserProfile(id)
                }
            }

            val navController = rememberNavController()

            StudybuddyTheme(darkTheme = darkMode) {
                StudyBuddyNavGraph(
                    navController = navController,
                    userVM = userVM,
                    authVM = authVM,
                    setupVM = setupVM,
                    calendarVM = calendarVM,
                    homeVM = homeVM
                )
            }
        }
    }
}
