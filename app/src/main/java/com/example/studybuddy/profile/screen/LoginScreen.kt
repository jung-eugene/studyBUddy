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
import kotlinx.coroutines.launch
import com.example.studybuddy.AuthViewModel
import com.example.studybuddy.Routes
import com.example.studybuddy.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking


//the first page of the app (loginpage)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    vm: AuthViewModel = AuthViewModel(),
    userVM: UserViewModel = UserViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPassword by remember { mutableStateOf(false) }


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
                    text = if (!isLogin)  "Create Account" else "Welcome Back!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("BU Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (showPassword) "Hide password" else "Show password"

                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    }

                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please fill in all fields.")
                            }
                        } else if (password.length < 6) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Password must be at least 6 characters.")
                            }
                        } else if (!email.endsWith("@bu.edu")) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please use your BU email address (@bu.edu).")
                            }
                        } else {
                            if (isLogin) {
                                vm.login(email, password) { success ->
                                    scope.launch {
                                        if (success) {
                                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                                            if (uid != null) {
                                                val setupComplete = runBlocking { userVM.isProfileSetupComplete(uid) }
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
                                        } else {
                                            snackbarHostState.showSnackbar("Login failed. Check credentials.")
                                        }
                                    }
                                }
                            } else {
                                vm.signup(email, password) { success ->
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
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(if (isLogin) "Log In" else "Sign Up", color = Color.White)
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = {
                    if (email.isNotBlank()) {
                        vm.resetPassword(email) { success, message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
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

                TextButton(onClick = { isLogin = !isLogin }) {
                    Text(
                        if (isLogin) "Don’t have an account? Sign up"
                        else "Already have an account? Log In",
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}