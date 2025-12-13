package com.example.studybuddy.profile.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.studybuddy.AuthViewModel
import com.example.studybuddy.Routes
import com.example.studybuddy.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authVM: AuthViewModel,
    userVM: UserViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser

        // 1. No user -> stay on login screen
        if (user == null) return@LaunchedEffect

        // 2. If user came back AND they finished setup in the past -> skip verify
        val uid = user.uid
        val setupComplete = userVM.isProfileSetupComplete(uid)

        if (setupComplete) {
            // User already completed onboarding → go straight home
            navController.navigate(Routes.Home.route) {
                popUpTo(Routes.Login.route) { inclusive = true }
            }
            return@LaunchedEffect
        }

        // 3. If user did NOT complete setup, but is verified → go to ProfileSetup
        if (user.isEmailVerified) {
            navController.navigate(Routes.ProfileSetup.route) {
                popUpTo(Routes.Login.route) { inclusive = true }
            }
            return@LaunchedEffect
        }

        // 4. If user is NOT verified AND signup just happened → send to VerifyEmail
        navController.navigate(Routes.VerifyEmail.route) {
            popUpTo(Routes.Login.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD32F2F)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {

            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --------- Title ---------
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    "studyBUddy",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )

                Spacer(Modifier.height(32.dp))

                // --------- Welcome header ---------
                Text(
                    text = if (!isLogin) "Create Account" else "Welcome Back",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(Modifier.height(18.dp))

                // --------- Email ---------
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("BU Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // --------- Password ---------
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
                )

                // -------- Forgot Password --------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val normalizedEmail = email.trim()
                            when {
                                normalizedEmail.isBlank() -> scope.launch {
                                    snackbarHostState.showSnackbar("Enter your BU email to reset password.")
                                }
                                !normalizedEmail.lowercase().endsWith("@bu.edu") -> scope.launch {
                                    snackbarHostState.showSnackbar("Use your BU email (@bu.edu).")
                                }
                                else -> authVM.resetPassword(normalizedEmail) { _, message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Forgot Password?", color = Color(0xFFD32F2F), fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --------- LOGIN / SIGNUP BUTTON ---------
                Button(
                    onClick = {
                        val normalizedEmail = email.trim()
                        // VALIDATION LOGIC
                        if (normalizedEmail.isBlank() || password.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Please fill in all fields.") }
                            return@Button
                        }
                        if (!normalizedEmail.lowercase().endsWith("@bu.edu")) {
                            scope.launch { snackbarHostState.showSnackbar("Use your BU email (@bu.edu).") }
                            return@Button
                        }
                        if (password.length < 6) {
                            scope.launch { snackbarHostState.showSnackbar("Password must be 6+ characters.") }
                            return@Button
                        }

                        if (isLogin) {
                            authVM.login(normalizedEmail, password) { success ->
                                if (success) {
                                    val user = FirebaseAuth.getInstance().currentUser

                                    // Reload user to get up-to-date verification status
                                    user?.reload()?.addOnCompleteListener {
                                        val refreshed = FirebaseAuth.getInstance().currentUser

                                        if (refreshed != null && !refreshed.isEmailVerified) {
                                            // New user or unverified user → go to verify screen
                                            navController.navigate(Routes.VerifyEmail.route) {
                                                popUpTo(Routes.Login.route) { inclusive = true }
                                            }
                                            return@addOnCompleteListener
                                        }

                                        // Verified → proceed normally
                                        val uid = refreshed?.uid
                                        if (uid != null) {
                                            scope.launch {
                                                userVM.loadUserProfile(uid)
                                                val setupComplete = userVM.isProfileSetupComplete(uid)

                                                if (setupComplete) {
                                                    navController.navigate(Routes.Home.route) {
                                                        popUpTo(Routes.Login.route) { inclusive = true }
                                                    }
                                                } else {
                                                    navController.navigate(Routes.ProfileSetup.route) {
                                                        popUpTo(Routes.Login.route) { inclusive = true }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Login failed. Check credentials.")
                                    }
                                }
                            }
                        } else {
                            authVM.signup(normalizedEmail, password) { success ->
                                scope.launch {
                                    if (success) {
                                        authVM.sendVerificationEmail { sent, msg ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                                if (!sent) return@launch
                                                navController.navigate(Routes.VerifyEmail.route) {
                                                    popUpTo(Routes.Login.route) { inclusive = true }
                                                }
                                            }
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Signup failed. Use your BU email (@bu.edu).")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(
                        if (isLogin) "Log In" else "Sign Up",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // --------- SIGN UP PROMPT ---------
                TextButton(
                    onClick = { isLogin = !isLogin },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (isLogin) "Don’t have an account? Sign up"
                        else "Already have an account? Log In",
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }

        // Snackbar for errors
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}