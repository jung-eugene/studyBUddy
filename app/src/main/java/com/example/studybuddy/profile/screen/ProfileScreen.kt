package com.example.studybuddy.profile.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector

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
            TopAppBar(
                title = {
                    Text(
                        "My Profile",
                        fontWeight = FontWeight.Bold,
                        color = color.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = color.primary,
                    titleContentColor = color.onPrimary
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("editProfile") }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = color.onPrimary
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        containerColor = color.background
    ) { pad ->

        if (uiState.user == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = color.primary)
            }
        }

        val user = uiState.user!!
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
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(color.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = color.primary,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Name and Major
            Text(
                text = user.name.ifBlank { "Unnamed User" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color.onBackground
            )
            Text(
                "${user.major} • ${user.year}",
                color = color.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // COURSES with icon
            SectionCard(title = "Courses", icon = Icons.Default.School) {
                if (user.courses.isEmpty()) {
                    Text("No courses added.", color = color.onSurfaceVariant)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(user.courses) { course ->
                            AssistChip(
                                onClick = {},
                                label = { Text(course, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                SectionCard(title = "Availability", icon = Icons.Default.Timer) {
                    Text(user.availability, color = color.onBackground)
                }
            }

            // STUDY PREFERENCES with icon
            SectionCard(title = "Study Preferences", icon = Icons.Default.CalendarToday) {
                if (user.studyPreferences.isEmpty()) {
                    Text("No preferences selected.", color = color.onSurfaceVariant)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(user.studyPreferences) { pref ->
                            AssistChip(
                                onClick = {},
                                label = { Text(pref) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = color.secondary,
                                    labelColor = color.primary
                                )
                            )
                        }
                    }
                }
            }

            // BIO without icon
            if (user.bio.isNotBlank()) {
                SectionCard(title = "About Me") {
                    Text(user.bio, color = color.onBackground)
                }
            }

            // SETTINGS with dark mode icon
            SectionCard(title = "Settings", icon = Icons.Default.DarkMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode", modifier = Modifier.weight(1f), color = color.onBackground)
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                userVM.getDarkMode(uid, enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = color.primary,
                            checkedTrackColor = color.secondary
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // LOGOUT BUTTON
            Button(
                onClick = {
                    authVM.signOut()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color.primary)
            ) {
                Text("Log Out", color = color.onPrimary, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

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
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    title,
                    color = color.primary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
        .}