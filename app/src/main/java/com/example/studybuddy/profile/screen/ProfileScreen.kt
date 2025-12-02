package com.example.studybuddy.profile.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.studybuddy.AuthViewModel
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.studybuddy.ui.StudyBuddyTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    userVM: UserViewModel,
    authVM: AuthViewModel
) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return

    val scope = rememberCoroutineScope()

    val color = MaterialTheme.colorScheme

    val uiState by userVM.uiState.collectAsState()
    val darkMode = uiState.darkMode

    LaunchedEffect(uid) {
        userVM.loadUserProfile(uid)
    }

    Scaffold(
        topBar = {
            StudyBuddyTopBar(
                title = "Profile",
                actions = {
                    IconButton(onClick = { navController.navigate("editProfile") }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        containerColor = color.background
    ) { pad ->

        val user = uiState.user
        if (user == null) {
            Box(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = color.primary)
            }
            return@Scaffold
        }

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(pad)
                .background(color.background)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Avatar
            if (user.photoUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(user.photoUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(color.secondary),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Name and Major
            Text(
                text = user.name.ifBlank { "Unnamed User" },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color.onBackground
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "${user.major} • ${user.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = color.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // BIO with icon
            if (user.bio.isNotBlank()) {
                SectionCard(title = "About Me", icon = Icons.Default.Person) {
                    Text(user.bio,
                        color = color.onBackground,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // COURSES with icon
            SectionCard(title = "Courses", icon = Icons.Default.Book) {
                if (user.courses.isEmpty()) {
                    Text("No courses added.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color.onSurfaceVariant)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(user.courses) { course ->
                            AssistChip(
                                onClick = {},
                                shape = RoundedCornerShape(50),
                                label = { Text(course,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                ) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = color.secondary,
                                    labelColor = color.primary
                                )
                            )
                        }
                    }
                }
            }

            // AVAILABILITY with icon
            if (user.availability.isNotBlank()) {
                SectionCard(title = "Availability", icon = Icons.Default.AccessTime) {
                    Text(user.availability,
                        color = color.onBackground,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }

            // STUDY PREFERENCES with icon
            SectionCard(title = "Study Preferences", icon = Icons.Default.CalendarToday) {
                if (user.studyPreferences.isEmpty()) {
                    Text("No preferences selected.",
                        color = color.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(user.studyPreferences) { pref ->
                            AssistChip(
                                onClick = {},
                                shape = RoundedCornerShape(50),
                                label = { Text(pref,
                                    style = MaterialTheme.typography.bodyMedium) },
                                    colors = AssistChipDefaults.assistChipColors(
                                    containerColor = color.secondary,
                                    labelColor = color.primary
                                )
                            )
                        }
                    }
                }
            }

            // SETTINGS with dark mode icon
            SectionCard(title = "Settings", icon = Icons.Default.DarkMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = color.onBackground
                    )
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { enabled ->
                            Log.d("ProfileScreen", "Dark mode toggled: $enabled")

                            scope.launch {
                                // Update Firestore darkMode field
                                userVM.getDarkMode(uid, enabled)

                                // Reload user profile so MainActivity recomposes theme
                                userVM.loadUserProfile(uid)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = color.primary,
                            checkedTrackColor = color.secondary,
                            uncheckedThumbColor = color.primary,
                            uncheckedTrackColor = color.secondary
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // LOGOUT BUTTON
            Button(
                onClick = {
                    authVM.signOut()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color.primary)
            ) {
                Text("Log Out", color = color.onPrimary, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            // FOOTER
            Text("studyBUddy", color = color.primary, fontWeight = FontWeight.Bold)
            Text(
                "Find your perfect study partner!",
                color = color.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFE0E0E0),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            // Title row
            Row(verticalAlignment = Alignment.CenterVertically) {

                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color(0xFF717182),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    title,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
