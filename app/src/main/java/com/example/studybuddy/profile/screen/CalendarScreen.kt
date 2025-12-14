package com.example.studybuddy.profile.screen

import android.accounts.Account
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.CalendarViewModel
import com.example.studybuddy.DurationOption
import com.example.studybuddy.LocationType
import com.example.studybuddy.PendingEvent
import com.example.studybuddy.StudySession
import com.example.studybuddy.CalendarEvent
import com.example.studybuddy.CalendarAttendee
import com.example.studybuddy.buildSessionDescription
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.example.studybuddy.displayText
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.example.studybuddy.ui.StudyBuddyTopBar

private const val TAG = "CalendarScreen"
// OAuth web client ID from the current Firebase/Google Cloud project (matches google-services.json)
private const val GOOGLE_WEB_CLIENT_ID = "275610785003-tlgs2ht9s9ks022r1lgmr3k0eebpmuou.apps.googleusercontent.com"

// Enums for variable Days of Week, include sun -> sat since calendar format
private val daysOfWeek = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
)

// options for the duration portion, map it to minutes to use in actual event creation
private val durationOptions = listOf(
    DurationOption("30 minutes", 30),
    DurationOption("45 minutes", 45),
    DurationOption("1 hour", 60),
    DurationOption("90 minutes", 90),
    DurationOption("2 hours", 120),
    DurationOption("3 hours", 180)
)

/*
 * Screen composition that wires view-model state into Compose UI
 * and surfaces the entire calendar experience for the user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    calendarViewModel: CalendarViewModel
) {
    /*
    * Compose-level scoped references used throughout the screen for context + coroutine helpers.
    */
    val context = LocalContext.current
    val credentialManager: CredentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

    // Snapshot the current state so recompositions show the freshest data.
    val signedInEmail: String? = calendarViewModel.signedInEmail
    val signedInAccount: Account? = calendarViewModel.signedInAccount
    val signingIn: Boolean = calendarViewModel.signingIn
    val currentMonth: YearMonth = calendarViewModel.currentMonth
    val selectedDate: LocalDate = calendarViewModel.selectedDate
    val remoteEvents = calendarViewModel.remoteEvents
    val isFetchingEvents = calendarViewModel.isFetchingEvents
    val eventsError = calendarViewModel.eventsError
    val currentUserEmail = calendarViewModel.signedInEmail

    /*
    * Entrypoint for the Google Identity flow. Requests a Google ID token so we can
    * tie new study sessions to the correct Google account in the view model.
    */
    fun launchSignIn() {
        calendarViewModel.updateSigningIn(true)
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

                if (googleCredential != null) {
                    val email = googleCredential.id
                    if (email.isNotBlank()) {
                        val account = Account(email, "com.google")
                        calendarViewModel.onSignedIn(email, account)
                        calendarViewModel.fetchUpcomingEvents(context)
                        val displayName = googleCredential.displayName?.takeUnless { it.isBlank() } ?: email
                        Toast.makeText(context, "Signed in as $displayName", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to read account email", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Unsupported credential", Toast.LENGTH_LONG).show()
                }
            } catch (e: GetCredentialException) {
                Toast.makeText(context, e.errorMessage ?: "Sign-in failed", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Credential Manager sign-in failed", e)
            } catch (t: Throwable) {
                Toast.makeText(context, "Sign-in failed: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Unexpected sign-in error", t)
            } finally {
                calendarViewModel.updateSigningIn(false)
            }
        }
    }

    fun launchSignOut() {
        // Best effort clear of the token + UI feedback - data is guarded via view model.
        calendarViewModel.clearAccount()
        Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
        coroutineScope.launch {
            runCatching {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { Log.w(TAG, "Failed to clear credential state", it) }
        }
    }

    LaunchedEffect(signedInAccount) {
        if (signedInAccount != null) {
            calendarViewModel.fetchUpcomingEvents(context)
        }
    }

    /*
    * actual ui for the user in calendar screen
    */
    Scaffold(
        topBar = { StudyBuddyTopBar(title = "Calendar") },
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

    GoogleAccountStatusCard(
        email = signedInEmail,
        signingIn = signingIn,
        isFetchingEvents = isFetchingEvents,
        onSignIn = ::launchSignIn,
        onSignOut = ::launchSignOut,
        onRefresh = {
            calendarViewModel.fetchUpcomingEvents(context)
        }
    )

            CalendarCard(
                month = currentMonth,
                selectedDate = selectedDate,
                onMonthChanged = { month -> calendarViewModel.showMonth(month) },
                onDateSelected = { date -> calendarViewModel.selectDate(date) }
            )

            SessionListSection(
                selectedDate = selectedDate,
                remoteEvents = remoteEvents[selectedDate].orEmpty(),
                isFetchingExternal = isFetchingEvents,
                eventsError = eventsError,
                currentUserEmail = currentUserEmail
            )

        }
    }
}
@Composable
private fun GoogleAccountStatusCard(
    email: String?,
    signingIn: Boolean,
    isFetchingEvents: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRefresh: () -> Unit
) {
    /*
    * card-like UI that shows the email of the logged in user or directs user to login using the button.
    * Updates states accordingly by passing state through onClick behavior.
    */
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Google Calendar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (email == null) {
                Text("Connect your Google account to sync study sessions.")
                Button(
                    onClick = onSignIn,
                    enabled = !signingIn
                ) {
                    Text(if (signingIn) "Signing in..." else "Sign in with Google")
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Signed in as $email", modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isFetchingEvents && !signingIn
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh calendar",
                            tint = if (isFetchingEvents) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (isFetchingEvents) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                Button(onClick = onSignOut) {
                    Text("Sign out")
                }
            }
        }
    }
}

@Composable
private fun SignInPromptDialog(
    signingIn: Boolean,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit
) {
    /*
    * Modal dialog that shows the sign in prompt when the user tries to schedule a session without signing in a google account.
    */
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text( // title
                    text = "Sign in required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("Please sign in with Google to schedule and sync study sessions.") //body
                Row( //buttons for sign in or exit
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Not now")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSignIn, enabled = !signingIn) {
                        Text(if (signingIn) "Opening..." else "Sign in")
                    }
                }
            }
        }
    }
}
@Composable
private fun CalendarCard(
    month: YearMonth,
    selectedDate: LocalDate,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    /*
    * Container encompassing the calendar, handling onClick behavior for the calendar.
    */
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Study Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            MonthCalendar(
                month = month,
                selectedDate = selectedDate,
                onMonthChanged = onMonthChanged,
                onDateSelected = onDateSelected
            )
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
    minDate: LocalDate? = null,
) {
    /*
    * Custom Calendar implementation using local date, days of week enum, and the buildCalendarDays function.
    */
    val today by rememberUpdatedState(newValue = LocalDate.now())

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChanged(month.minusMonths(1)) }) { // months are offset by 1
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
    /*
    * create days of the month for the calendar, mapping to the local date type for ease of use.
    */
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
private fun SessionListSection(
    selectedDate: LocalDate,
    remoteEvents: List<CalendarEvent>,
    isFetchingExternal: Boolean,
    eventsError: String?,
    currentUserEmail: String?
) {
    val headerFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy") }
    Text(
        text = "Sessions on ${selectedDate.format(headerFormatter)}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    if (remoteEvents.isEmpty() && !isFetchingExternal) {
        Text(
            text = "No sessions scheduled for this day.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        if (remoteEvents.isNotEmpty() || isFetchingExternal) {
            Text(
                text = "Google Calendar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
        if (isFetchingExternal && remoteEvents.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text("Fetching events...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        remoteEvents.forEach { event ->
            RemoteEventCard(event = event, currentUserEmail = currentUserEmail)
            Spacer(modifier = Modifier.height(12.dp))
        }
        eventsError?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SessionCard(session: StudySession) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val locationLine = listOfNotNull(
        session.locationType.displayText(),
        session.location.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.course.take(3).uppercase(Locale.getDefault()),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = session.course,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "with ${session.partner}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SessionDetailRow(
                icon = Icons.Default.CalendarMonth,
                text = dateFormatter.format(session.date)
            )
            SessionDetailRow(
                icon = Icons.Default.AccessTime,
                text = "${timeFormatter.format(session.time)} • ${session.durationLabel}"
            )
            SessionDetailRow(
                icon = Icons.Default.LocationOn,
                text = locationLine
            )
            if (session.notes.isNotBlank()) {
                SessionDetailRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    text = session.notes
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    text = "Synced to Google Calendar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RemoteEventCard(event: CalendarEvent, currentUserEmail: String?) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val timeText = buildString {
        append(timeFormatter.format(event.start.toLocalTime()))
        event.end?.let { end ->
            append("  ").append(timeFormatter.format(end.toLocalTime()))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = event.title.ifBlank { "Calendar Event" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Google Calendar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SessionDetailRow(
                icon = Icons.Default.CalendarMonth,
                text = dateFormatter.format(event.start.toLocalDate())
            )
            SessionDetailRow(
                icon = Icons.Default.AccessTime,
                text = timeText
            )
            SessionDetailRow(
                icon = Icons.Default.LocationOn,
                text = event.location ?: "No location"
            )
            if (event.attendees.isNotEmpty()) {
                Text(
                    text = "Guests",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AttendeesList(attendees = event.attendees, currentUserEmail = currentUserEmail)
            }
        }
    }
}

@Composable
private fun SessionDetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AttendeesList(attendees: List<CalendarAttendee>, currentUserEmail: String?) {
    val statusLabel: (String) -> String = { status ->
        when (status.lowercase()) {
            "accepted" -> "Accepted"
            "tentative" -> "Tentative"
            "declined" -> "Declined"
            else -> "Waiting"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        attendees.forEach { attendee ->
            val isYou = currentUserEmail?.equals(attendee.email, ignoreCase = true) == true
            val primaryLabel = attendee.displayName?.takeIf { it.isNotBlank() }
                ?: if (isYou) "You" else attendee.email
            val secondary = attendee.displayName?.takeIf { it.isNotBlank() && !isYou }?.let { attendee.email }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                secondary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = statusLabel(attendee.responseStatus),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (attendee.responseStatus.lowercase()) {
                        "accepted" -> MaterialTheme.colorScheme.primary
                        "declined" -> MaterialTheme.colorScheme.error
                        "tentative" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSessionDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSessionCreated: (StudySession) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var partner by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var dialogMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(if (initialDate.isBefore(today)) today else initialDate) }
    var sessionTime by remember { mutableStateOf(LocalTime.of(19, 0)) }
    var durationExpanded by remember { mutableStateOf(false) }
    var durationOption by remember { mutableStateOf(durationOptions[2]) }
    var locationType by remember { mutableStateOf(LocationType.IN_PERSON) }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "New Study Session",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = partner,
                    onValueChange = { partner = it },
                    label = { Text("Study Partner") },
                    placeholder = { Text("Select a partner") }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Course") },
                    placeholder = { Text("Select a course") }
                )

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Date", fontWeight = FontWeight.SemiBold)
                        MonthCalendar(
                            month = dialogMonth,
                            selectedDate = selectedDate,
                            onMonthChanged = { dialogMonth = it },
                            onDateSelected = { selectedDate = it },
                            minDate = today
                        )
                    }
                }

                /*
                * Full-width Box + overlay clickable layer lets taps anywhere on the field
                * trigger the native picker instead of focusing the read-only text field.
                */
                val timeTapSource = remember { MutableInteractionSource() }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = timeFormatter.format(sessionTime),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time") },
                        trailingIcon = { Icon(imageVector = Icons.Default.AccessTime, contentDescription = null) }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                        trailingIcon = { Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null) }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                    Text(text = "Location Type", fontWeight = FontWeight.SemiBold)
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
                    modifier = Modifier.fillMaxWidth(),
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("e.g., Mugar Library, 3rd Floor") }
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("What will you study?") }
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = course.isNotBlank(),
                    onClick = {
                        onSessionCreated(
                            StudySession(
                                partner = partner.ifBlank { "Study Buddy" },
                                course = course.ifBlank { "Study Session" },
                                date = selectedDate,
                                time = sessionTime,
                                durationLabel = durationOption.label,
                                durationMinutes = durationOption.minutes,
                                locationType = locationType,
                                location = location,
                                notes = notes
                            )
                        )
                    }
                ) {
                    Text("Create Session")
                }
            }
        }
    }
}
