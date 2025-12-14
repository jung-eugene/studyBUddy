package com.example.studybuddy.profile.screen

import android.accounts.Account
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.window.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.CalendarViewModel
import com.example.studybuddy.MatchEntry
import com.example.studybuddy.DurationOption
import com.example.studybuddy.LocationType
import com.example.studybuddy.StudySession
import com.example.studybuddy.User
import com.example.studybuddy.UserViewModel
import com.example.studybuddy.buildSessionDescription
import com.example.studybuddy.ui.StudyBuddyTopBar
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.YearMonth
import java.util.Locale

private const val GOOGLE_WEB_CLIENT_ID =
    "275610785003-tlgs2ht9s9ks022r1lgmr3k0eebpmuou.apps.googleusercontent.com"

private val durationOptions = listOf(
    DurationOption("30 minutes", 30),
    DurationOption("45 minutes", 45),
    DurationOption("1 hour", 60),
    DurationOption("90 minutes", 90),
    DurationOption("2 hours", 120),
    DurationOption("3 hours", 180)
)

private val daysOfWeek = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    navController: NavHostController,
    userVM: UserViewModel,
    calendarVM: CalendarViewModel
) {
    val uiState by userVM.uiState.collectAsState()
    val currentUser = uiState.user
    var selectedUserForPopup by remember { mutableStateOf<User?>(null) }
    var selectedIsMutual by remember { mutableStateOf(false) }
    var scheduleTarget by remember { mutableStateOf<User?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    val signedInAccount = calendarVM.signedInAccount
    val signingIn = calendarVM.signingIn

    val recoverAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        calendarVM.retryPendingEvent(context)
    }

    fun launchGoogleSignIn(onSuccess: () -> Unit) {
        calendarVM.updateSigningIn(true)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        coroutineScope.launch {
            try {
                val resultCredential = credentialManager.getCredential(context, request).credential
                val googleCredential = (resultCredential as? CustomCredential)
                    ?.takeIf { it.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL }
                    ?.let { GoogleIdTokenCredential.createFrom(it.data) }

                val email = googleCredential?.id.orEmpty()
                if (email.isNotBlank()) {
                    val account = Account(email, "com.google")
                    calendarVM.onSignedIn(email, account)
                    val displayName = googleCredential?.displayName?.takeUnless { it.isNullOrBlank() } ?: email
                    Toast.makeText(context, "Signed in as $displayName", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "Failed to read Google account email", Toast.LENGTH_LONG).show()
                }
            } catch (e: GetCredentialException) {
                Toast.makeText(context, e.errorMessage ?: "Sign-in failed", Toast.LENGTH_LONG).show()
            } catch (t: Throwable) {
                Toast.makeText(context, "Sign-in failed: ${t.message}", Toast.LENGTH_LONG).show()
            } finally {
                calendarVM.updateSigningIn(false)
            }
        }
    }

    LaunchedEffect(currentUser?.id) {
        val uid = currentUser?.id ?: FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {
            userVM.loadMatches(uid)
        }
    }

    LaunchedEffect(signedInAccount, scheduleTarget) {
        if (signedInAccount != null && scheduleTarget != null) {
            showScheduleDialog = true
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
                        if (uiState.matches.isNotEmpty()) {
                            item {
                                Text(
                                    "Current Matches",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
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
                                    onClick = {
                                        selectedUserForPopup = entry.user
                                        selectedIsMutual = entry.isMutual
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        if (uiState.deletedMatches.isNotEmpty()) {
                            item {
                                Text(
                                    "Past Matches",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
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

    if (selectedUserForPopup != null) {
        Dialog(onDismissRequest = { selectedUserForPopup = null }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        UserCardCompact(
                            user = selectedUserForPopup!!,
                            elevation = 0.dp   // disable shadow for popup
                        )
                        Button(
                            onClick = {
                                if (!selectedIsMutual) return@Button
                                selectedUserForPopup?.let { target ->
                                    scheduleTarget = target
                                    if (calendarVM.signedInAccount == null) {
                                        launchGoogleSignIn { showScheduleDialog = true }
                                    } else {
                                        showScheduleDialog = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                            ,
                            enabled = selectedIsMutual && !signingIn
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    signingIn -> "Connecting..."
                                    selectedIsMutual -> "Send Calendar Invite"
                                    else -> "Mutual match required"
                                }
                            )
                        }
                        if (!selectedIsMutual) {
                            Text(
                                text = "Scheduling unlocks after a mutual match.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

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

    if (showScheduleDialog && scheduleTarget != null) {
        ScheduleInviteDialog(
            user = scheduleTarget!!,
            onDismiss = {
                showScheduleDialog = false
                scheduleTarget = null
            },
            onCreate = { session, attendeeEmail ->
                calendarVM.addSession(session)
                val account = calendarVM.signedInAccount
                if (account != null) {
                    calendarVM.syncSessionToCalendar(
                        context = context,
                        event = com.example.studybuddy.PendingEvent(
                            account = account,
                            title = session.course,
                            start = session.startDateTime(),
                            end = session.endDateTime(),
                            description = buildSessionDescription(session),
                            location = session.location.takeIf { it.isNotBlank() },
                            attendeeEmail = attendeeEmail
                        ),
                        requestAuth = { intent ->
                            intent?.let { recoverAuthLauncher.launch(it) }
                        }
                    )
                } else {
                    Toast.makeText(context, "Google sign-in required", Toast.LENGTH_LONG).show()
                }
                if (attendeeEmail.isNullOrBlank()) {
                    Toast.makeText(context, "No email stored for this user", Toast.LENGTH_SHORT).show()
                }
                showScheduleDialog = false
                scheduleTarget = null
                selectedUserForPopup = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleInviteDialog(
    user: User,
    onDismiss: () -> Unit,
    onCreate: (StudySession, String?) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val guestEmail = remember(user.email) { user.email.trim() }
    val scrollState = rememberScrollState()
    var course by remember { mutableStateOf("") }
    var dialogMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var sessionTime by remember { mutableStateOf(LocalTime.now()) }
    var durationExpanded by remember { mutableStateOf(false) }
    var durationOption by remember { mutableStateOf(durationOptions[2]) }
    var locationType by remember { mutableStateOf(LocationType.IN_PERSON) }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties()) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
        ) {
        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Invite ${user.name.ifBlank { "Study Buddy" }}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = guestEmail.ifBlank { "No email on file" },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Guest") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    isError = guestEmail.isBlank()
                )

                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Course") },
                    placeholder = { Text("e.g. CS112 Exam Review") }
                )

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Date", fontWeight = FontWeight.SemiBold)
                        MonthCalendar(
                            month = dialogMonth,
                            selectedDate = selectedDate,
                            onMonthChanged = { dialogMonth = it },
                            onDateSelected = { selectedDate = it },
                            minDate = today
                        )
                    }
                }

                val timeTapSource = remember { MutableInteractionSource() }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = timeFormatter.format(sessionTime),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time") },
                        trailingIcon = { Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null) }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = timeTapSource,
                                indication = null
                            ) {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> sessionTime = LocalTime.of(hour, minute) },
                                    sessionTime.hour,
                                    sessionTime.minute,
                                    false
                                ).show()
                            }
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    val durationTapSource = remember { MutableInteractionSource() }
                    OutlinedTextField(
                        value = durationOption.label,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Duration") },
                        trailingIcon = { Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = null) }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = durationTapSource,
                                indication = null
                            ) { durationExpanded = true }
                    )
                    DropdownMenu(
                        expanded = durationExpanded,
                        onDismissRequest = { durationExpanded = false }
                    ) {
                        durationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    durationOption = option
                                    durationExpanded = false
                                }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Location Type", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = locationType == LocationType.IN_PERSON,
                            onClick = { locationType = LocationType.IN_PERSON },
                            label = { Text("In Person") }
                        )
                        FilterChip(
                            selected = locationType == LocationType.VIRTUAL,
                            onClick = { locationType = LocationType.VIRTUAL },
                            label = { Text("Virtual") }
                        )
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location") },
                    placeholder = { Text("e.g., Mugar Library, 3rd Floor") }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("What will you study?") }
                )

                Button(
                    onClick = {
                        val session = StudySession(
                            partner = user.name.ifBlank { "Study Buddy" },
                            course = course.ifBlank { "Study Session" },
                            date = selectedDate,
                            time = sessionTime,
                            durationLabel = durationOption.label,
                            durationMinutes = durationOption.minutes,
                            locationType = locationType,
                            location = location,
                            notes = notes
                        )
                        val attendeeEmail = guestEmail.takeIf { it.isNotBlank() }
                        onCreate(session, attendeeEmail)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = course.isNotBlank()
                ) {
                    Text("Send Invite")
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    modifier: Modifier = Modifier,
    month: YearMonth,
    selectedDate: LocalDate,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    minDate: LocalDate? = null
) {
    val today by rememberUpdatedState(newValue = LocalDate.now())

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChanged(month.minusMonths(1)) }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = month.year.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onMonthChanged(month.plusMonths(1)) }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val calendarDays = remember(month) { buildCalendarDays(month) }
        calendarDays.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { date ->
                    val disabled = date == null || (minDate != null && date.isBefore(minDate))
                    DayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        isDisabled = disabled,
                        onSelect = onDateSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    isDisabled: Boolean,
    onSelect: (LocalDate) -> Unit
) {
    val background = when {
        date == null -> Color.Transparent
        isDisabled -> Color.Transparent
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val textColor = when {
        date == null -> Color.Transparent
        isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(enabled = !isDisabled && date != null) { date?.let(onSelect) }
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        date?.let {
            Text(
                text = it.dayOfMonth.toString(),
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private fun buildCalendarDays(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val padding = daysOfWeek.indexOf(firstDay.dayOfWeek)
    return buildList {
        repeat(padding) { add(null) }
        for (day in 1..daysInMonth) {
            add(month.atDay(day))
        }
        while (size % 7 != 0) {
            add(null)
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
                    "${user.major} - ${user.year}",
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                        text = "${user.major} - ${user.year}",
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
