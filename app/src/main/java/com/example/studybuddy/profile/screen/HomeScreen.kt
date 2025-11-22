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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.User
import com.example.studybuddy.UserViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder

/**
 * Home screen displaying swipeable list of potential study partners.
 * Swipe right to like, left to skip.
 */
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp), // lifts above bottom nav
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Grey circle with heart icon
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
            } else {
                val user = users.first()

                CardDeck(
                    users = users,
                    onLike = { users = users.drop(1) },
                    onSkip = { users = users.drop(1) }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button, advances to next card
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        tonalElevation = 1.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(2.dp, red),
                        modifier = Modifier.size(70.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { users = users.drop(1) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Skip",
                                    tint = red,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    // Like button, advances to next card
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
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
    val swipeThreshold = with(density) { 120.dp.toPx() } // how far to swipe to trigger action

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value > swipeThreshold -> {
                                    // Swiped right --> like
                                    offsetX.animateTo(
                                        targetValue = with(density) { 400.dp.toPx() },
                                        animationSpec = tween(200)
                                    )
                                    onLike()
                                    offsetX.snapTo(0f)
                                }
                                offsetX.value < -swipeThreshold -> {
                                    // Swiped left --> skip
                                    offsetX.animateTo(
                                        targetValue = with(density) { -400.dp.toPx() },
                                        animationSpec = tween(200)
                                    )
                                    onSkip()
                                    offsetX.snapTo(0f)
                                }
                                else -> {
                                    // Not far enough --> snap back
                                    offsetX.animateTo(0f, animationSpec = tween(200))
                                }
                            }
                        }
                    }
                )
            }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = rotation // adds angled swipe
            }
    ) {
        UserCardCompact(user)
    }
}

@Composable
fun CardDeck(
    users: List<User>,
    onLike: () -> Unit,
    onSkip: () -> Unit
) {
    val topUser = users.firstOrNull() ?: return

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val rotation = (offsetX.value / 60).coerceIn(-10f, 10f)

    val density = LocalDensity.current
    val swipeThreshold = with(density) { 120.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        // Slightly visible next card under the top card
        if (users.size > 1) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 20.dp)
                    .graphicsLayer {
                        scaleX = 0.96f
                        scaleY = 0.96f
                        alpha = 0.7f
                    }
            ) {
                UserCardCompact(users[1])
            }
        }

        // Top swipeable card
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > swipeThreshold -> {
                                        offsetX.animateTo(with(density) { 500.dp.toPx() })
                                        onLike()
                                        offsetX.snapTo(0f)
                                    }
                                    offsetX.value < -swipeThreshold -> {
                                        offsetX.animateTo(with(density) { -500.dp.toPx() })
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
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .graphicsLayer { rotationZ = rotation }
        ) {
            UserCardCompact(topUser)
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
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(red)
                .padding(top = 18.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "avatar" icon - replace with profile pic in the future
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

                // Major and year of user
                val yearText = user.year.ifBlank { "Year N/A" }
                val majorText = user.major.ifBlank { "Major N/A" }
                Text(
                    text = "$majorText • $yearText",
                    color = Color(0xFFFAFAFA),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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

            // Study Preferences
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    user.studyPreferences!!.forEach { pref ->
                        Chip(text = pref, bg = chipGreyBg, fg = chipGreyText)
                    }
                }
            }

            // Availability
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
