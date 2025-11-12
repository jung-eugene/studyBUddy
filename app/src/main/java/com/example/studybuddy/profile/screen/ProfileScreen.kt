package com.example.studybuddy.profile.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.studybuddy.AuthViewModel
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// the profile screen page (include the currently user informations) + logout + light/dark theme
// dark/light theme need to be fixed because it can only be changed when you restart the page once you toggle it.
//not sure if its due to it not being deployed to an actual device
// also need to inclde an edit button for user to be able to edit there accoutn in future
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    userVM: UserViewModel = UserViewModel(),
    authVM: AuthViewModel = AuthViewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

    var darkMode by remember { mutableStateOf(false) }
    val userProfile by userVM.userProfile.collectAsState()

    LaunchedEffect(uid) {
        userVM.loadUserProfile(uid)
        darkMode = userVM.getDarkMode(uid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        userProfile?.name ?: "Loading...",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${userProfile?.major ?: ""} • ${userProfile?.year ?: ""}",
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clickable { navController.navigate("profileStep1") }
                            .padding(8.dp)
                    ) {
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Courses", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        userProfile?.courses?.forEach { course ->
                            AssistChip(onClick = {}, label = { Text(course) })
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Study Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    if (userProfile?.studyPreferences.isNullOrEmpty()) {
                        Text("No study preferences set.", color = Color.Gray)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            userProfile?.studyPreferences?.forEach { pref ->
                                AssistChip(onClick = {}, label = { Text(pref) })
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark Mode", modifier = Modifier.weight(1f))
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { enabled ->
                                darkMode = enabled
                                scope.launch {
                                    userVM.updateDarkMode(uid, enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD32F2F),
                                checkedTrackColor = Color(0xFFFFCDD2),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    authVM.signOut()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("Log Out", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}