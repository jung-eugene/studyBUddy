package com.example.studybuddy.profile.screen

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
            elevation = CardDefaults.cardElevation(6.dp)
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // App title
                Text(
                    "studyBUddy",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )

                Text(
                    "Swipe, match, and study smarter!",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = if (!isLogin) "Create Account" else "Welcome Back!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(Modifier.height(16.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("BU Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (showPassword) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                // LOGIN / SIGNUP button
                Button(
                    onClick = {
                        // Validation
                        if (email.isBlank() || password.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Please fill in all fields.") }
                            return@Button
                        }
                        if (!email.endsWith("@bu.edu")) {
                            scope.launch { snackbarHostState.showSnackbar("Use your BU email (@bu.edu).") }
                            return@Button
                        }
                        if (password.length < 6) {
                            scope.launch { snackbarHostState.showSnackbar("Password must be 6+ characters.") }
                            return@Button
                        }

                        if (isLogin) {
                            // -----------------------------------------
                            // LOGIN FLOW
                            // -----------------------------------------
                            authVM.login(email, password) { success ->
                                if (success) {
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                                    if (uid != null) {
                                        scope.launch {
                                            // Check if user completed profile setup
                                            val setupComplete = userVM.isProfileSetupComplete(uid)

                                            // Load user profile into unified uiState
                                            userVM.loadUserProfile(uid)

                                            // Navigate depending on setup
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
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Login failed. Check credentials.")
                                    }
                                }
                            }

                        } else {
                            // -----------------------------------------
                            // SIGN UP
                            // -----------------------------------------
                            authVM.signup(email, password) { success ->
                                scope.launch {
                                    if (success) {
                                        navController.navigate(Routes.ProfileSetup.route) {
                                            popUpTo(Routes.Login.route) { inclusive = true }
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Signup failed.")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(
                        if (isLogin) "Log In" else "Sign Up",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Forgot password
                TextButton(onClick = {
                    if (email.isNotBlank()) {
                        authVM.resetPassword(email) { _, message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Enter your email to reset password.")
                        }
                    }
                }) {
                    Text("Forgot password?", color = Color(0xFFD32F2F))
                }

                Spacer(Modifier.height(8.dp))

                // Toggle login/signup mode
                TextButton(onClick = { isLogin = !isLogin }) {
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