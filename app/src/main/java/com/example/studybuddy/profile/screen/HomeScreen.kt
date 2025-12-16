package com.example.studybuddy.profile.screen

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.HomeViewModel
import com.example.studybuddy.User
import com.example.studybuddy.UserViewModel
import com.example.studybuddy.AvailabilitySlot
import com.example.studybuddy.ui.StudyBuddyTopBar
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchPopup(
    matchedUser: User,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(40.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "It's a Match!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "You and ${matchedUser.name} matched! \nStart a study session together.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(46.dp)
                ) {
                    Text("Awesome!")
                }
            }
        }
    }
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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var lastShakeRefresh by remember { mutableLongStateOf(0L) }
    val refreshMatches: () -> Unit = refresh@{
        val now = System.currentTimeMillis()
        if (now - lastShakeRefresh < 1200 || homeState.isLoading) return@refresh
        lastShakeRefresh = now
        coroutineScope.launch { snackbarHostState.showSnackbar("Refreshing matches...") }
        homeVM.setCandidates(userState.allUsers) // reset deck immediately with current data
        userVM.getAllUsers()
    }

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

    ShakeToRefreshListener { refreshMatches() }

    Scaffold(
        topBar = { StudyBuddyTopBar(title = "studyBUddy") },
        bottomBar = { BottomNavBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                EmptyStateUI(
                    isLoading = homeState.isLoading,
                    onRefresh = { refreshMatches() }
                )
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
                    color = MaterialTheme.colorScheme.surface,
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

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Shake your phone to refresh matches.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { refreshMatches() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Refresh")
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
private fun EmptyStateUI(
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "No More Profiles",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Check back later for new study buddies!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRefresh,
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
            }
            Text(if (isLoading) "Refreshing..." else "Refresh matches")
        }
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

/**
 * ShakeToRefreshListener
 *
 * This implementation was developed with assistance from an AI/LLM
 * to refine the sensor logic, g-force calculation, and lifecycle handling.
 * All logic was reviewed, tested, and integrated by the development team.
 */
@Composable
private fun ShakeToRefreshListener(
    shakeThreshold: Float = 2.7f,
    debounceMs: Long = 1200L,
    onShake: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val lastShake = remember { mutableLongStateOf(0L) }

    DisposableEffect(sensorManager, lifecycleOwner) {
        val manager = sensorManager ?: return@DisposableEffect onDispose { }
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return@DisposableEffect onDispose { }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gX = event.values[0] / SensorManager.GRAVITY_EARTH
                val gY = event.values[1] / SensorManager.GRAVITY_EARTH
                val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
                val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
                val now = System.currentTimeMillis()


                if (gForce > shakeThreshold && now - lastShake.longValue > debounceMs) {
                    lastShake.longValue = now
                    onShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> manager.registerListener(
                    listener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
                )
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> manager.unregisterListener(listener)
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            manager.unregisterListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserCardCompact(
    user: User, elevation: Dp = 6.dp
) {
    val red = Color(0xFFD32F2F)
    val lightRed = Color(0xFFFFEBEE)
    val chipGreyBg = Color(0xFFF2F2F2)
    val chipGreyText = Color(0xFF202124)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
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
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
                )

                Text(
                    text = "${user.major} • ${user.year}",
                    color = MaterialTheme.colorScheme.onPrimary,
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
                Icon(
                    Icons.Filled.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Current Courses",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Text(
                    "No courses added.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (user.studyPreferences.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Study Preferences",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    Icon(
                        Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Availability",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

@Preview(showBackground = true)
@Composable
fun MatchPopupPreview() {
    val fakeUser = User(
        id = "123",
        name = "Preview Student",
        major = "Computer Science",
        year = "Junior",
        courses = listOf("CS 111", "CS 131"),
        studyPreferences = listOf("Night Owl", "Library"),
        availabilitySlots = listOf(
            AvailabilitySlot("Monday Morning"),
            AvailabilitySlot("Wednesday Afternoon")
        ),
        email = "preview@bu.edu"
    )

    MatchPopup(
        matchedUser = fakeUser,
        onDismiss = {}
    )
}
