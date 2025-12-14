package com.example.studybuddy

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Manages Firebase user data, like fetching all users info, updating user profile completion

data class AvailabilitySlot(
    val day: String = "",
    val timeOfDay: String = "",
    val meetTimes: List<String> = emptyList()
) {
    fun label(): String {
        val timeLabel = when {
            meetTimes.isNotEmpty() -> meetTimes.joinToString(" / ")
            timeOfDay.isNotBlank() -> timeOfDay
            else -> ""
        }
        return listOf(day, timeLabel).filter { it.isNotBlank() }.joinToString(" ")
    }
}

// USER DATA CLASS
data class User(
    val studyPreferences: List<String> = emptyList(),
    val id: String = "",
    val name: String = "",
    val major: String = "",
    val year: String = "",
    val courses: List<String> = emptyList(),
    val availabilitySlots: List<AvailabilitySlot> = emptyList(),
    val bio: String = "",
    val photoUrl: String = "",
    val email: String = "",
    val darkMode: Boolean = false,
    val profileSetupComplete: Boolean = false, // flag to track if setup done
    val streakCount: Int = 0,
    val bestStreak: Int = 0,
    val lastStreakDate: String = "" // date of last daily check-in
)

// UI STATE FOR THE USER VM
data class MatchEntry(
    val user: User,
    val isMutual: Boolean = false,
    val liked: Boolean = false
)

data class UserUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val darkMode: Boolean = false,
    val allUsers: List<User> = emptyList(),
    val matches: List<MatchEntry> = emptyList(),
    val deletedMatches: List<MatchEntry> = emptyList()
)


/**
 * Handles login, signup, and password reset using Firebase Authentication.
 */
class AuthViewModel : ViewModel() {
    private val TAG = "AuthVM"
    private val auth = FirebaseAuth.getInstance()
    private fun isBuEmail(email: String) = email.trim().lowercase().endsWith("@bu.edu")

    // --- LOGIN ---
    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        if (!isBuEmail(email)) {
            Log.w(TAG, "Login blocked for non-BU email: $email")
            onResult(false)
            return
        }
        Log.d(TAG, "Attempt login with $email")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.i(TAG, "Login successful for $email")
                } else {
                    Log.e(TAG, "Login FAILED for $email", it.exception)
                }
                onResult(it.isSuccessful)
            }
    }

    // --- SIGN UP ---
    fun signup(email: String, password: String, onResult: (Boolean) -> Unit) {
        if (!isBuEmail(email)) {
            Log.w(TAG, "Signup blocked for non-BU email: $email")
            onResult(false)
            return
        }
        Log.d(TAG, "Attempt signup with $email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.i(TAG, "Signup successful for $email")
                } else {
                    Log.e(TAG, "Signup FAILED for $email", it.exception)
                }
                onResult(it.isSuccessful)
            }
    }

    fun sendVerificationEmail(onResult: (Boolean, String) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "No signed-in user.")
            return
        }
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Verification email sent to ${user.email}")
                } else {
                    onResult(false, task.exception?.message ?: "Failed to send verification email.")
                }
            }
    }

    fun reloadAndCheckVerified(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return onResult(false)
        user.reload()
            .addOnCompleteListener {
                onResult(user.isEmailVerified)
            }
    }

    // --- PASSWORD RESET ---
    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        Log.d(TAG, "Requesting password reset for $email")
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Password reset email sent to $email")
                    onResult(true, "Password reset email sent.")
                } else {
                    Log.e(TAG, "Password reset failed for $email", task.exception)
                    onResult(false, task.exception?.message ?: "Error sending reset email.")
                }
            }
    }


    // --- SESSION HELPERS ---
    fun currentUser() = auth.currentUser
    fun signOut() {
        Log.i(TAG, "User logged out")
        auth.signOut()
    }
}

/**
 * Manages user profiles and Firestore interaction.
 */

class UserViewModel : ViewModel() {
    private val TAG = "UserVM"
    private val db = FirebaseFirestore.getInstance()
    private val matchRepo = MatchRepository()
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState

    private fun setLoading(loading: Boolean) {
        Log.d(TAG, "setLoading($loading)")
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }
    private fun setError(message: String?) {
        Log.w(TAG, "setError: $message")
        _uiState.value = _uiState.value.copy(error = message)
    }

    // LOAD CURRENT USER PROFILE
    fun loadUserProfile(uid: String) {
        Log.d(TAG, "Loading profile for uid=$uid")
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            try {
                val authEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
                val doc = db.collection("users").document(uid).get().await()
                val loadedUser = doc.toObject(User::class.java)

                val patchedUser = if (loadedUser != null && loadedUser.email.isBlank() && authEmail.isNotBlank()) {
                    try {
                        db.collection("users").document(uid)
                            .update("email", authEmail)
                            .await()
                        Log.d(TAG, "Backfilled email for $uid")
                    } catch (updateError: Exception) {
                        Log.w(TAG, "Failed to backfill email for $uid", updateError)
                    }
                    loadedUser.copy(email = authEmail)
                } else loadedUser

                Log.i(TAG, "Profile loaded for $uid: ${patchedUser?.name}")

                _uiState.value = _uiState.value.copy(
                    user = patchedUser,
                    darkMode = patchedUser?.darkMode ?: false,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile for $uid", e)
                setLoading(false)
                setError(e.message)
            }
        }
    }

    // SAVE / UPDATE PROFILE
    fun saveUserProfile(uid: String, updatedUser: User) {
        Log.d(TAG, "Saving profile for $uid")
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            try {
                val authEmail = FirebaseAuth.getInstance().currentUser?.email
                val userToSave = if (updatedUser.email.isBlank() && !authEmail.isNullOrBlank()) {
                    updatedUser.copy(email = authEmail)
                } else updatedUser

                db.collection("users").document(uid)
                    .set(userToSave)
                    .await()

                Log.i(TAG, "Profile saved for $uid")

                _uiState.value = _uiState.value.copy(
                    user = userToSave,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile for $uid", e)
                setLoading(false)
                setError(e.message)
            }
        }
    }

    // DARK MODE TOGGLE
    fun getDarkMode(uid: String, enabled: Boolean) {
        Log.d(TAG, "Updating darkMode($enabled) for $uid")
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .update("darkMode", enabled)
                    .await()

                Log.i(TAG, "Dark mode updated for $uid = $enabled")

                _uiState.value = _uiState.value.copy(darkMode = enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating darkMode for $uid", e)
                setError(e.message)
            }
        }
    }

    // DAILY STREAK CHECK-IN
    fun checkInToday(uid: String) {
        val currentUser = _uiState.value.user ?: return
        viewModelScope.launch {
            try {
                val today = LocalDate.now(ZoneId.systemDefault())
                val lastDateStr = currentUser.lastStreakDate
                val lastDate = lastDateStr.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }

                // Already checked in today
                if (lastDate != null && lastDate.isEqual(today)) {
                    return@launch
                }

                val yesterday = today.minusDays(1)
                val newStreak = when {
                    lastDate == null -> 1
                    lastDate.isEqual(yesterday) -> currentUser.streakCount + 1
                    else -> 1
                }
                val best = maxOf(currentUser.bestStreak, newStreak)

                val updates = mapOf(
                    "streakCount" to newStreak,
                    "bestStreak" to best,
                    "lastStreakDate" to today.toString()
                )

                db.collection("users").document(uid)
                    .set(updates, SetOptions.merge())
                    .await()

                val updatedUser = currentUser.copy(
                    streakCount = newStreak,
                    bestStreak = best,
                    lastStreakDate = today.toString()
                )
                _uiState.value = _uiState.value.copy(user = updatedUser)
                Log.i(TAG, "Check-in recorded for $uid, streak=$newStreak best=$best")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording check-in for $uid", e)
                setError(e.message)
            }
        }
    }

    // LOAD ALL USERS (Used for Home / Matches)
    fun getAllUsers() {
        Log.d(TAG, "Loading all users...")

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        viewModelScope.launch {
            setLoading(true)
            try {
                val snapshot = db.collection("users").get().await()

                // Parse documents into User objects and ensure `id` is set.
                // Keep any `id` that already exists in the document; otherwise fall back to the Firestore doc id.
                val users = snapshot.documents.mapNotNull { doc ->
                    val u = doc.toObject(User::class.java) ?: return@mapNotNull null
                    if (u.id.isBlank()) u.copy(id = doc.id) else u
                }

                _uiState.value = _uiState.value.copy(
                    allUsers = users,
                    isLoading = false
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error loading all users", e)
                setLoading(false)
                setError(e.message)
            }
        }
    }


    // LOAD MATCHES
    fun loadMatches(uid: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                setError(null)
                val deletedIds = _uiState.value.deletedMatches.map { it.user.id }.toSet()

                // 1) Get IDs of matched users (mutual) and liked users (one-sided)
                val matchIds = matchRepo.getMatchIdsForUser(uid)
                val likedIds = matchRepo.getLikedIdsForUser(uid)

                // 2) Union of ids we care about
                val ids = (matchIds + likedIds).distinct()

                if (ids.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        matches = emptyList(),
                        isLoading = false
                    )
                    return@launch
                }

                // 3) Fetch each user's data and build MatchEntry objects
                val entries = mutableListOf<MatchEntry>()
                for (id in ids) {
                    val doc = db.collection("users").document(id).get().await()
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        val populatedUser = if (user.id.isBlank()) user.copy(id = doc.id) else user
                        val isMutual = matchIds.contains(id)
                        val liked = likedIds.contains(id)
                        entries.add(MatchEntry(user = populatedUser, isMutual = isMutual, liked = liked))
                    }
                }

                // 4) Update state
                _uiState.value = _uiState.value.copy(
                    matches = entries.filterNot { deletedIds.contains(it.user.id) },
                    isLoading = false
                )

                Log.d("UserVM", "Loaded matches/liked: ${entries.size}")

            } catch (e: Exception) {
                Log.e("UserVM", "loadMatches ERROR", e)
                setLoading(false)
                setError(e.message)
            }
        }
    }

    // Add a one-sided liked user locally so Matches screen shows it immediately.
    fun addLocalLike(user: User) {
        val populated = user
        val existing = _uiState.value.matches.toMutableList()
        val deletedIds = _uiState.value.deletedMatches.map { it.user.id }
        if (existing.any { it.user.id == populated.id } || deletedIds.contains(populated.id)) return
        existing.add(MatchEntry(user = populated, isMutual = false, liked = true))
        _uiState.value = _uiState.value.copy(matches = existing)
    }

    // Promote an existing local MatchEntry to mutual (reveal email).
    fun promoteLocalToMutual(userId: String) {
        val updated = _uiState.value.matches.map { entry ->
            if (entry.user.id == userId) entry.copy(isMutual = true) else entry
        }
        _uiState.value = _uiState.value.copy(matches = updated)
    }

    fun unmatchUser(userId: String) {
        val current = _uiState.value
        val target = current.matches.firstOrNull { it.user.id == userId } ?: return
        val remainingMatches = current.matches.filterNot { it.user.id == userId }
        val newDeleted = (current.deletedMatches + target).distinctBy { it.user.id }
        _uiState.value = current.copy(matches = remainingMatches, deletedMatches = newDeleted)
    }

    fun undoUnmatch(userId: String) {
        val current = _uiState.value
        val target = current.deletedMatches.firstOrNull { it.user.id == userId } ?: return
        val remainingDeleted = current.deletedMatches.filterNot { it.user.id == userId }
        val updatedMatches = (listOf(target) + current.matches).distinctBy { it.user.id }
        _uiState.value = current.copy(matches = updatedMatches, deletedMatches = remainingDeleted)
    }

    suspend fun isProfileSetupComplete(uid: String): Boolean {
        Log.d(TAG, "Checking profileSetupComplete for $uid")
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val result = doc.getBoolean("profileSetupComplete") ?: false
            Log.i(TAG, "profileSetupComplete = $result for $uid")
            result
        } catch (e: Exception) {
            Log.e(TAG, "isProfileSetupComplete ERROR for $uid", e)
            false
        }
    }

}

/*
 * Shared calendar view model that survives navigation. Owns Google account state,
 * currently selected month/date, and the list of locally scheduled study sessions.
 */
class CalendarViewModel : ViewModel() {
    var signedInEmail: String? by mutableStateOf(null)
        private set
    var signedInAccount: Account? by mutableStateOf(null)
        private set
    var signingIn: Boolean by mutableStateOf(false)
        private set
    var currentMonth: YearMonth by mutableStateOf(YearMonth.now())
        private set
    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set
    val sessions: SnapshotStateList<StudySession> = mutableStateListOf()
    var creationStatus: String? by mutableStateOf(null)
        private set
    var creationResult: CalendarCreationResult? by mutableStateOf(null)
        private set
    var remoteEvents: Map<LocalDate, List<CalendarEvent>> by mutableStateOf(emptyMap())
        private set
    var isFetchingEvents: Boolean by mutableStateOf(false)
        private set
    var eventsError: String? by mutableStateOf(null)
        private set
    private var pendingEvent: PendingEvent? = null
    private var lastRequestAuth: ((Intent?) -> Unit)? = null

    // Toggle the loading spinner while we wait for the Google Identity flow to return.
    fun updateSigningIn(value: Boolean) {
        signingIn = value
    }

    fun onSignedIn(email: String, account: Account) {
        signedInEmail = email
        signedInAccount = account
        signingIn = false
    }

    fun clearAccount() {
        signedInEmail = null
        signedInAccount = null
        clearCalendarData()
    }

    fun showMonth(month: YearMonth) {
        currentMonth = month
        if (selectedDate.month != month.month || selectedDate.year != month.year) {
            selectedDate = month.atDay(1)
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
        currentMonth = YearMonth.from(date)
    }

    fun addSession(session: StudySession) {
        sessions.add(session)
        selectDate(session.date)
    }

    /*
     * Persists the newly-added session to Google Calendar and tracks pending operations
     * so we can recover if the API requests additional scopes.
     */
    fun syncSessionToCalendar(
        context: Context,
        event: PendingEvent,
        requestAuth: (Intent?) -> Unit
    ) {
        lastRequestAuth = requestAuth
        pendingEvent = event
        creationStatus = "Syncing session with Google Calendar..."
        creationResult = null
        viewModelScope.launch {
            performCalendarCreate(
                context = context,
                account = event.account,
                eventTitle = event.title,
                start = event.start,
                end = event.end,
                description = event.description,
                location = event.location,
                attendeeEmail = event.attendeeEmail,
                notifyAttendees = event.attendeeEmail?.isNotBlank() == true,
                onSuccess = { link ->
                    pendingEvent = null
                    creationStatus = "Session synced to Google Calendar"
                    creationResult = CalendarCreationResult(
                        success = true,
                        message = "Calendar event created for your study session.",
                        htmlLink = link.takeIf { it.startsWith("http") }
                    )
                    Toast.makeText(context, "Session synced", Toast.LENGTH_SHORT).show()
                },
                onFailure = { msg ->
                    pendingEvent = null
                    creationStatus = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                },
                onRecover = { intent ->
                    creationStatus = "Authorization required to sync"
                    pendingEvent = event
                    requestAuth(intent)
                }
            )
        }
    }

    /*
     * Called after the user grants additional permissions; replays the pending event creation.
     */
    fun retryPendingEvent(context: Context) {
        val pending = pendingEvent ?: return
        val requestAuth = lastRequestAuth ?: { _: Intent? -> }
        syncSessionToCalendar(context, pending, requestAuth)
    }

    fun clearCreationResult() {
        creationResult = null
    }

    fun clearCalendarData() {
        remoteEvents = emptyMap()
        eventsError = null
        isFetchingEvents = false
        creationStatus = null
        creationResult = null
        pendingEvent = null
        lastRequestAuth = null
        sessions.clear()
    }

    fun fetchUpcomingEvents(
        context: Context,
        daysAhead: Long = 30
    ) {
        val account = signedInAccount ?: return
        viewModelScope.launch {
            isFetchingEvents = true
            eventsError = null
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val timeMin = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val timeMax = now.plusDays(daysAhead).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf("https://www.googleapis.com/auth/calendar.events")
                ).apply { selectedAccount = account }
                val accessToken = withContext(Dispatchers.IO) { credential.token }
                val result = fetchGoogleCalendarEvents(
                    accessToken = accessToken,
                    timeMin = timeMin,
                    timeMax = timeMax
                )
                result.onSuccess { events ->
                    val filtered = events.filter { isStudyBuddyEvent(it) }
                    remoteEvents = filtered.groupBy { it.start.toLocalDate() }
                }.onFailure { t ->
                    eventsError = t.message
                }
            } catch (e: Exception) {
                eventsError = e.message
            } finally {
                isFetchingEvents = false
            }
        }
    }
}

enum class LocationType { IN_PERSON, VIRTUAL }

data class DurationOption(val label: String, val minutes: Int)

data class CalendarCreationResult(
    val success: Boolean,
    val message: String,
    val htmlLink: String? = null
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val start: ZonedDateTime,
    val end: ZonedDateTime?,
    val location: String?,
    val htmlLink: String?,
    val sourceTag: String?,
    val attendees: List<CalendarAttendee> = emptyList()
)

data class CalendarAttendee(
    val email: String,
    val responseStatus: String,
    val displayName: String? = null
)

data class PendingEvent(
    val account: Account,
    val title: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val description: String?,
    val location: String?,
    val attendeeEmail: String? = null
)

data class StudySession(
    val id: Long = System.currentTimeMillis(),
    val partner: String,
    val course: String,
    val date: LocalDate,
    val time: LocalTime,
    val durationLabel: String,
    val durationMinutes: Int,
    val locationType: LocationType,
    val location: String,
    val notes: String
) {
    fun startDateTime(): ZonedDateTime = ZonedDateTime.of(date, time, ZoneId.systemDefault())
    fun endDateTime(): ZonedDateTime = startDateTime().plusMinutes(durationMinutes.toLong())
}

fun LocationType.displayText(): String = when (this) {
    LocationType.IN_PERSON -> "In Person"
    LocationType.VIRTUAL -> "Virtual"
}

fun buildSessionDescription(session: StudySession): String {
    val builder = StringBuilder()
    builder.append("Study session with ").append(session.partner)
    builder.append("\nType: ").append(session.locationType.displayText())
    if (session.location.isNotBlank()) {
        builder.append("\nLocation: ").append(session.location)
    }
    if (session.notes.isNotBlank()) {
        builder.append("\nNotes: ").append(session.notes)
    }
    return builder.toString()
}

private const val CALENDAR_VM_TAG = "CalendarViewModel"
private const val STUDY_BUDDY_EVENT_SOURCE = "studybuddy"

/*
 * Lightweight wrapper around Google Calendar's insert event endpoint. Handles token retrieval,
 * success/failure reporting, and wiring back into the UI callbacks supplied by the view model.
 */
@SuppressLint("NewApi")
private suspend fun performCalendarCreate(
    context: Context,
    account: Account,
    eventTitle: String,
    start: ZonedDateTime,
    end: ZonedDateTime,
    description: String?,
    location: String?,
    attendeeEmail: String?,
    notifyAttendees: Boolean = false,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
    onRecover: (Intent?) -> Unit
) {
    try {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf("https://www.googleapis.com/auth/calendar.events")
        ).apply { selectedAccount = account }
        val accessToken = withContext(Dispatchers.IO) { credential.token }
        val result = createGoogleCalendarEvent(
            accessToken = accessToken,
            summary = eventTitle,
            description = description,
            location = location,
            start = start,
            end = end,
            attendeeEmail = attendeeEmail,
            notifyAttendees = notifyAttendees
        )
        result.onSuccess(onSuccess).onFailure { onFailure("Error: ${it.message}") }
    } catch (e: UserRecoverableAuthException) {
        onRecover(e.intent)
    } catch (e: GoogleAuthException) {
        onFailure("Auth error: ${e.message}")
    } catch (t: Throwable) {
        onFailure("Unexpected error: ${t.message}")
    }
}

/*
 * Talks to the Google Calendar REST API using the supplied OAuth token and event metadata.
 */
@SuppressLint("NewApi")
private suspend fun createGoogleCalendarEvent(
    accessToken: String,
    summary: String,
    description: String? = null,
    location: String? = null,
    start: ZonedDateTime,
    end: ZonedDateTime,
    timeZoneId: String = ZoneId.systemDefault().id,
    attendeeEmail: String? = null,
    notifyAttendees: Boolean = false
): Result<String> = withContext(Dispatchers.IO) {
    try {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val startJson = JSONObject().apply {
            put("dateTime", start.format(formatter))
            put("timeZone", timeZoneId)
        }
        val endJson = JSONObject().apply {
            put("dateTime", end.format(formatter))
            put("timeZone", timeZoneId)
        }
        val body = JSONObject().apply {
            put("summary", summary)
            description?.let { put("description", it) }
            location?.let { put("location", it) }
            put("start", startJson)
            put("end", endJson)
            put("extendedProperties", JSONObject().apply {
                put("private", JSONObject().apply { put("app", STUDY_BUDDY_EVENT_SOURCE) })
            })
            attendeeEmail
                ?.takeIf { it.isNotBlank() }
                ?.let { email ->
                    val attendee = JSONObject()
                    attendee.put("email", email)
                    put("attendees", JSONArray().apply { put(attendee) })
                }
        }

        val urlString = buildString {
            append("https://www.googleapis.com/calendar/v3/calendars/primary/events")
            if (notifyAttendees) append("?sendUpdates=all")
        }
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 20000
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val code = conn.responseCode
        val stream: InputStream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        Log.d(CALENDAR_VM_TAG, "Calendar create response ($code): $response")
        if (code in 200..299) {
            val json = JSONObject(response)
            val htmlLink = json.optString("htmlLink", "")
            Result.success(htmlLink.ifEmpty { "Event created (no link returned)" })
        } else {
            Result.failure(IllegalStateException("HTTP $code: $response"))
        }
    } catch (t: Throwable) {
        Result.failure(t)
    }
}

@SuppressLint("NewApi")
private suspend fun fetchGoogleCalendarEvents(
    accessToken: String,
    timeMin: String,
    timeMax: String,
    maxResults: Int = 100
): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
    try {
        val base = "https://www.googleapis.com/calendar/v3/calendars/primary/events"
        val query = listOf(
            "singleEvents=true",
            "orderBy=startTime",
            "timeMin=${URLEncoder.encode(timeMin, "UTF-8")}",
            "timeMax=${URLEncoder.encode(timeMax, "UTF-8")}",
            "maxResults=$maxResults"
        ).joinToString("&")
        val url = URL("$base?$query")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15000
            readTimeout = 20000
        }

        val code = conn.responseCode
        val stream: InputStream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        Log.d(CALENDAR_VM_TAG, "Calendar list response ($code): $response")
        if (code !in 200..299) {
            return@withContext Result.failure(IllegalStateException("HTTP $code: $response"))
        }

        val json = JSONObject(response)
        val items = json.optJSONArray("items") ?: JSONArray()
        val events = mutableListOf<CalendarEvent>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val start = parseCalendarDate(item.optJSONObject("start"))
            val end = parseCalendarDate(item.optJSONObject("end"))
            if (start != null) {
                val id = item.optString("id", "").ifBlank { "event_$i" }
                val title = item.optString("summary", "Calendar Event")
                val description = item.optString("description", "").takeIf { it.isNotBlank() }
                val location = item.optString("location", "").takeIf { it.isNotBlank() }
                val htmlLink = item.optString("htmlLink", "").takeIf { it.isNotBlank() }
                val attendees = item.optJSONArray("attendees")?.let { arr ->
                    buildList {
                        for (j in 0 until arr.length()) {
                            val attendee = arr.optJSONObject(j) ?: continue
                            val email = attendee.optString("email", "").trim()
                            if (email.isBlank()) continue
                            val status = attendee.optString("responseStatus", "needsAction")
                            val name = attendee.optString("displayName", "").takeIf { it.isNotBlank() }
                            add(CalendarAttendee(email = email, responseStatus = status, displayName = name))
                        }
                    }
                }.orEmpty()
                val sourceTag = item.optJSONObject("extendedProperties")
                    ?.optJSONObject("private")
                    ?.let { priv ->
                        priv.optString("app", "")
                            .ifBlank { priv.optString("source", "") }
                            .takeIf { it.isNotBlank() }
                    }
                events.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        description = description,
                        start = start,
                        end = end,
                        location = location,
                        htmlLink = htmlLink,
                        sourceTag = sourceTag,
                        attendees = attendees
                    )
                )
            }
        }
        Result.success(events)
    } catch (t: Throwable) {
        Result.failure(t)
    }
}

@SuppressLint("NewApi")
private fun parseCalendarDate(obj: JSONObject?): ZonedDateTime? {
    if (obj == null) return null
    val dateTime = obj.optString("dateTime", "")
    val dateOnly = obj.optString("date", "")
    return try {
        when {
            dateTime.isNotBlank() -> ZonedDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            dateOnly.isNotBlank() -> LocalDate.parse(dateOnly).atStartOfDay(ZoneId.systemDefault())
            else -> null
        }
    } catch (t: Throwable) {
        null
    }
}

private fun isStudyBuddyEvent(event: CalendarEvent): Boolean {
    if (event.sourceTag == STUDY_BUDDY_EVENT_SOURCE) return true
    val desc = event.description?.lowercase().orEmpty()
    val title = event.title.lowercase()
    return desc.contains("study session with") ||
            desc.contains("studybuddy") ||
            title.contains("studybuddy")
}
