package com.example.studybuddy.profile.screen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.MatchEntry
import com.example.studybuddy.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.studybuddy.ui.StudyBuddyTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    navController: NavHostController,
    userVM: UserViewModel
) {
    val uiState by userVM.uiState.collectAsState()
    val currentUser = uiState.user

    LaunchedEffect(currentUser?.id) {
        val uidFromState = currentUser?.id
        if (!uidFromState.isNullOrBlank()) {
            userVM.loadMatches(uidFromState)
        } else {
            val authUid = FirebaseAuth.getInstance().currentUser?.uid
            if (!authUid.isNullOrBlank()) {
                userVM.loadMatches(authUid)
            }
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
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (uiState.matches.isEmpty()) {
                EmptyMatchesUI()
                return@Column
            }

            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.matches) { entry ->
                    MatchCard(entry)
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
fun MatchCard(entry: MatchEntry) {
    val user = entry.user
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
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

            Column {
                Text(
                    user.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${user.major} • ${user.year}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (entry.isMutual) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = user.email.ifBlank { "Email not set" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32)
                    )
                } else if (entry.liked) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "You liked this user — waiting for a match",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
