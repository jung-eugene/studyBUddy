package com.example.studybuddy.profile.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.User
import com.example.studybuddy.UserViewModel

//placeholders for home button the defult page -- will be later edit by Eugene


/**
 * Home screen displaying swipeable list of potential study partners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    vm: UserViewModel = UserViewModel()
) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf(listOf<User>()) }

    // Fetch user list once
    LaunchedEffect(Unit) {
        users = vm.getAllUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "studyBUddy",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (users.isEmpty()) {
                Text(
                    text = "No profiles available yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                val user = users.first() // display user for now

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = user.name.ifBlank { "Unnamed Student" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1A1A1A)
                        )
                        Text("${user.major} • ${user.year}", color = Color.Gray)

                        if (user.bio.isNotBlank()) {
                            Text(
                                text = "\"${user.bio}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF444444)
                            )
                        }

                        if (user.courses.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                user.courses.take(3).forEach { course ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(course.trim()) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Color(0xFFFFEBEE),
                                            labelColor = Color(0xFFD32F2F)
                                        )
                                    )
                                }
                            }
                        }

                        if (user.availability.isNotBlank()) {
                            Text(
                                text = "Availability: ${user.availability}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Skip / Like buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { /* Skip logic */ },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Skip",
                            tint = Color(0xFFB0B0B0),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                // Example placeholder
                                users = users.drop(1)
                            }
                        },
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }
        }
    }
}