package com.example.studybuddy.profile.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.User
import com.example.studybuddy.UserViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.CalendarMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    vm: UserViewModel = UserViewModel()
) {
    var users by remember { mutableStateOf(listOf<User>()) }

    LaunchedEffect(Unit) { users = vm.getAllUsers() }

    val red = Color(0xFFD32F2F)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("studyBUddy") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = red
                )
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (users.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("No profiles yet — check again later.", color = Color.Gray)
            } else {
                val user = users.first()
                UserCardCompact(user)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip — red X with subtle red ring, advances to next card
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        tonalElevation = 1.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(2.dp, red),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { users = users.drop(1) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Skip", tint = red)
                            }
                        }
                    }

                    // Like — red circle w/ white outline heart
                    Surface(
                        shape = CircleShape,
                        color = red,
                        tonalElevation = 2.dp,
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(8.dp, CircleShape, clip = false)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { users = users.drop(1) }) {
                                Icon(
                                    Icons.Outlined.Favorite,
                                    contentDescription = "Like",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserCardCompact(user: User) {
    val red = Color(0xFFD32F2F)
    val lightRed = Color(0xFFFFEBEE)
    val chipGreyBg = Color(0xFFF2F2F2)
    val chipGreyText = Color(0xFF202124)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        // Header (solid red, centered avatar + name; smaller avatar)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(red)
                .padding(top = 18.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // simple circular icon "avatar"
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = user.name.ifBlank { "Unnamed Student" },
                color = Color.White,
                // keep normal weight, no bold
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
            )

            // Major • Year (same row)
            val yearText = user.year.ifBlank { "Year N/A" }
            val majorText = user.major.ifBlank { "Major N/A" }
            Text(
                text = "$majorText • $yearText",
                color = Color(0xFFFAFAFA),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Current Courses
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Book, contentDescription = null, tint = Color(0xFF6B6B6B))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Current Courses",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal)
                )
            }
            Spacer(Modifier.height(4.dp))
            if (user.courses.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp) // tighter rows
                ) {
                    user.courses.forEach { course ->
                        Chip(
                            text = course.trim(),
                            bg = lightRed,
                            fg = red
                        )
                    }
                }
            } else {
                Text("No courses added.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }

            // Study Preferences (grey chips, black text)
            if (!user.studyPreferences.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF6B6B6B)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Study Preferences",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal)
                    )
                }
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    user.studyPreferences!!.forEach { pref ->
                        Chip(text = pref, bg = chipGreyBg, fg = chipGreyText)
                    }
                }
            }

            // Availability (individual grey chips)
            if (user.availability.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFF6B6B6B))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Available",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal)
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Split availability string into tokens by comma and chip them
                val slots = user.availability.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slots.forEach { s -> Chip(text = s, bg = chipGreyBg, fg = chipGreyText) }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, bg.copy(alpha = 0.6f))
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
