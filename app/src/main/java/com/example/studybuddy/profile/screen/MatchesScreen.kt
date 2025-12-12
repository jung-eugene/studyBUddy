package com.example.studybuddy.profile.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.MatchEntry
import com.example.studybuddy.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.studybuddy.ui.StudyBuddyTopBar
import com.example.studybuddy.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    navController: NavHostController,
    userVM: UserViewModel
) {
    val uiState by userVM.uiState.collectAsState()
    val currentUser = uiState.user
    var selectedUserForPopup by remember { mutableStateOf<User?>(null) }

    // Load matches for this user
    LaunchedEffect(currentUser?.id) {
        val uid = currentUser?.id ?: FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {
            userVM.loadMatches(uid)
        }
    }

    Scaffold(
        topBar = { StudyBuddyTopBar(title = "Matches") },
        bottomBar = { BottomNavBar(navController) }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.matches.isEmpty() && uiState.deletedMatches.isEmpty() -> {
                    EmptyMatchesUI()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // ============================
                        // ACTIVE MATCHES SECTION
                        // ============================
                        if (uiState.matches.isNotEmpty()) {
                            item {
                                Text(
                                    "Current Matches",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            items(
                                items = uiState.matches,
                                key = { it.user.id }
                            ) { entry ->
                                MatchCard(
                                    entry = entry,
                                    onUnmatch = { userVM.unmatchUser(entry.user.id) },
                                    onClick = { selectedUserForPopup = entry.user }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        // ============================
                        // DELETED MATCHES SECTION
                        // ============================
                        if (uiState.deletedMatches.isNotEmpty()) {
                            item {
                                Text(
                                    "Past Matches",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            items(
                                items = uiState.deletedMatches,
                                key = { it.user.id }
                            ) { entry ->
                                DeletedMatchCard(
                                    entry = entry,
                                    onRestore = { userVM.undoUnmatch(entry.user.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    // Popup showing user profile when clicked
    if (selectedUserForPopup != null) {
        Dialog(onDismissRequest = { selectedUserForPopup = null }) {

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.LightGray,
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                Box(modifier = Modifier.fillMaxWidth()) {

                    // Main content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        UserCardCompact(selectedUserForPopup!!)
                    }

                    // Close button
                    IconButton(
                        onClick = { selectedUserForPopup = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyMatchesUI() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Grey circle with chat icon
            Surface(
                shape = CircleShape,
                color = Color(0xFFF0F0F3),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color(0xFF777777),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Title
            Text(
                "No Matches Yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF333333)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Start swiping to find your study buddies!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF777777)
            )
        }
    }
}

@Composable
fun MatchCard(
    entry: MatchEntry,
    onUnmatch: () -> Unit,
    onClick: () -> Unit
) {
    val user = entry.user
    val mutual = entry.isMutual
    val liked = entry.liked
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFE0E0E0),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${user.major} • ${user.year}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mutual) {
                        AssistChip(
                            onClick = {},
                            shape = RoundedCornerShape(50),
                            label = { Text("Mutual match") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    } else if (liked) {
                        AssistChip(
                            onClick = {},
                            shape = RoundedCornerShape(50),
                            label = { Text("Waiting for match") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.HourglassTop,
                                    contentDescription = null
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }


                if (mutual) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = user.email.ifBlank { "Email not set" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            OutlinedButton(onClick = onUnmatch) {
                Text("Unmatch")
            }
        }
    }
}

@Composable
private fun DeletedMatchCard(
    entry: MatchEntry,
    onRestore: () -> Unit
) {
    val user = entry.user
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.PersonOff,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${user.major} • ${user.year}",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = onRestore) {
                    Text("Restore")
                }
            }
        }
    }
}
