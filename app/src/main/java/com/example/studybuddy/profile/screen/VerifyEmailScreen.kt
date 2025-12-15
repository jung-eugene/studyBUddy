package com.example.studybuddy.profile.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.studybuddy.AuthViewModel
import com.example.studybuddy.Routes
import com.example.studybuddy.UserViewModel
import com.example.studybuddy.ui.StudyBuddyTopBar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreen(
    navController: NavHostController,
    authVM: AuthViewModel,
    userVM: UserViewModel
)
{
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var initialEmailSent by remember { mutableStateOf(false) }
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email.orEmpty()

    LaunchedEffect(user) {
        // If user somehow already verified, route forward.
        if (user?.isEmailVerified == true) {
            navigatePostVerify(navController, userVM)
        }
    }

    LaunchedEffect(user?.uid) {
        // Auto-send verification when the screen opens for a newly signed-up user.
        if (!initialEmailSent && user != null && !user.isEmailVerified) {
            initialEmailSent = true
            authVM.sendVerificationEmail { _, msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        }
    }

    Scaffold(
        topBar = { StudyBuddyTopBar(title = "studyBUddy", showBack = false) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.MarkEmailUnread,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Verify your BU email",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "We sent a verification link to $email.\nPlease open the email and tap the link to continue.",
                style = MaterialTheme.typography.bodyMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Didn’t receive the email?",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "• Check your spam folder\n" +
                                "• Make sure you signed up with your @bu.edu address\n" +
                                "• You can resend the verification link below",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = {
                        authVM.sendVerificationEmail { ok, msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    }) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Resend verification email")
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    checking = true
                    authVM.reloadAndCheckVerified { verified ->
                        checking = false
                        if (verified) {
                            // User is verified → sign out and go to login
                            authVM.signOut()
                            navController.navigate(Routes.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Email not verified yet. Please click the link in your inbox."
                                )
                            }
                        }
                    }
                },
                enabled = !checking
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp)
                    )
                } else {
                    Text("Continue")
                }
            }
        }
    }
}

private suspend fun navigatePostVerify(navController: NavHostController, userVM: UserViewModel) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid == null) {
        navController.navigate(Routes.Login.route) { popUpTo(0) { inclusive = true } }
        return
    }
    userVM.loadUserProfile(uid)
    val setupComplete = runCatching { userVM.isProfileSetupComplete(uid) }.getOrDefault(false)
    if (setupComplete) {
        navController.navigate(Routes.Home.route) {
            popUpTo(Routes.VerifyEmail.route) { inclusive = true }
        }
    } else {
        navController.navigate(Routes.ProfileSetup.route) {
            popUpTo(Routes.VerifyEmail.route) { inclusive = true }
        }
    }
}
