package com.example.studybuddy.profile.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.HomeViewModel
import com.example.studybuddy.User
import com.example.studybuddy.UserViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.studybuddy.ui.StudyBuddyTopBar
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchPopup(
    matchedUser: User,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("It's a Match!") },
        text = {
            Text("You and ${matchedUser.name} both liked each other.\nStart a study session!")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Awesome!") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    userVM: UserViewModel,
    homeVM: HomeViewModel
) {
    val userState by userVM.uiState.collectAsState()
    val homeState by homeVM.uiState.collectAsState()

    var showMatchPopup by remember { mutableStateOf(false) }
    var matchedUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) { userVM.getAllUsers() }

    LaunchedEffect(userState.allUsers) {
        if (userState.allUsers.isNotEmpty()) {
            homeVM.setCandidates(userState.allUsers)
        }
    }

    val red = Color(0xFFD32F2F)
    val candidate = homeState.candidates.getOrNull(homeState.currentIndex)

    Scaffold(
        topBar = { StudyBuddyTopBar(title = "studyBUddy") },
        bottomBar = { BottomNavBar(navController) }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (homeState.isLoading) {
                CircularProgressIndicator(color = red)
                return@Column
            }

            if (candidate == null) {
                EmptyStateUI()
                return@Column
            }

            CardDeck(
                user = candidate,
                onLike = {
                    userVM.addLocalLike(candidate)
                    homeVM.likeCurrent { isMatch, user ->
                        if (isMatch && user != null) {
                            userVM.promoteLocalToMutual(user.id)
                            matchedUser = user
                            showMatchPopup = true
                        }
                    }
                },
                onSkip = { homeVM.skipCurrent() }
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    tonalElevation = 1.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(2.dp, red),
                    modifier = Modifier.size(70.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { homeVM.skipCurrent() }) {
                            Icon(Icons.Filled.Close,
                                contentDescription = "Skip",
                                tint = red,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = red,
                    tonalElevation = 2.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        IconButton(onClick = {
                            homeVM.likeCurrent { isMatch, user ->
                                if (isMatch && user != null) {
                                    matchedUser = user
                                    showMatchPopup = true
                                }
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Favorite,
                                contentDescription = "Like",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMatchPopup && matchedUser != null) {
        MatchPopup(
            matchedUser = matchedUser!!,
            onDismiss = { showMatchPopup = false }
        )
    }
}

@Composable
private fun EmptyStateUI() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFF0F0F3),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = Color(0xFF777777),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "No More Profiles",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF333333)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Check back later for new study buddies!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF777777)
        )
    }
}

@Composable
fun CardDeck(
    user: User,
    onLike: () -> Unit,
    onSkip: () -> Unit
) {
    SwipeableUserCard(
        user = user,
        onLike = onLike,
        onSkip = onSkip
    )
}

@Composable
private fun SwipeableUserCard(
    user: User,
    onLike: () -> Unit,
    onSkip: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val rotation = (offsetX.value / 60).coerceIn(-10f, 10f)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { 120.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, drag ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + drag.x)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value > threshold -> {
                                    offsetX.animateTo(
                                        targetValue = with(density) { 400.dp.toPx() },
                                        animationSpec = tween(200)
                                    )
                                    onLike()
                                    offsetX.snapTo(0f)
                                }
                                offsetX.value < -threshold -> {
                                    offsetX.animateTo(
                                        targetValue = with(density) { -400.dp.toPx() },
                                        animationSpec = tween(200)
                                    )
                                    onSkip()
                                    offsetX.snapTo(0f)
                                }
                                else -> {
                                    offsetX.animateTo(0f, animationSpec = tween(200))
                                }
                            }
                        }
                    }
                )
            }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = rotation
            }
    ) {
        UserCardCompact(user)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserCardCompact(user: User) {
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(red)
                .padding(top = 18.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = user.name.ifBlank { "Unnamed Student" },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
                )

                Text(
                    text = "${user.major} • ${user.year}",
                    color = Color(0xFFFAFAFA),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Book, contentDescription = null, tint = Color(0xFF6B6B6B))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Current Courses",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (user.courses.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    user.courses.forEach { course ->
                        Chip(
                            text = course,
                            bg = lightRed,
                            fg = red
                        )
                    }
                }
            } else {
                Text("No courses added.", color = Color.Gray)
            }

            if (user.studyPreferences.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF6B6B6B)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Study Preferences", style = MaterialTheme.typography.bodyMedium)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    user.studyPreferences.forEach { pref ->
                        Chip(text = pref, bg = chipGreyBg, fg = chipGreyText)
                    }
                }
            }

            if (user.availabilitySlots.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFF6B6B6B))
                    Spacer(Modifier.width(8.dp))
                    Text("Availability", style = MaterialTheme.typography.bodyMedium)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    user.availabilitySlots.forEach { slot ->
                        Chip(text = slot.label(), bg = chipGreyBg, fg = chipGreyText)
                    }
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
