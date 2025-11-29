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
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
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

// USER DATA CLASS
data class User(
    val studyPreferences: List<String> = emptyList(),
    val id: String = "",
    val name: String = "",
    val major: String = "",
    val year: String = "",
    val courses: List<String> = emptyList(),
    val availability: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val darkMode: Boolean = false,
    val profileSetupComplete: Boolean = false // flag to track if setup done
)

// UI STATE FOR THE USER VM
data class UserUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val darkMode: Boolean = false,
    val allUsers: List<User> = emptyList()
)


/**
 * Handles login, signup, and password reset using Firebase Authentication.
 */
class AuthViewModel : ViewModel() {
    private val TAG = "AuthVM"
    private val auth = FirebaseAuth.getInstance()

    // --- LOGIN ---
    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
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
                val doc = db.collection("users").document(uid).get().await()
                val user = doc.toObject(User::class.java)
                Log.i(TAG, "Profile loaded for $uid: ${user?.name}")

                _uiState.value = _uiState.value.copy(
                    user = user,
                    darkMode = user?.darkMode ?: false,
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
                db.collection("users").document(uid)
                    .set(updatedUser, SetOptions.merge())
                    .await()

                Log.i(TAG, "Profile saved for $uid")

                _uiState.value = _uiState.value.copy(
                    user = updatedUser,
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

    // LOAD ALL USERS (Used for Home / Matches)
    fun getAllUsers() {
        Log.d(TAG, "Loading all users...")

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        viewModelScope.launch {
            setLoading(true)
            try {
                val snapshot = db.collection("users").get().await()
                val users = snapshot.toObjects(User::class.java)

                // FILTER OUT CURRENT USER
                val filtered = if (currentUid != null) {
                    users.filter { it.id != currentUid }
                } else {
                    users
                }

                Log.i(TAG, "Loaded ${filtered.size}/${users.size} users after filtering self")

                _uiState.value = _uiState.value.copy(
                    allUsers = filtered,
                    isLoading = false
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error loading all users", e)
                setLoading(false)
                setError(e.message)
            }
        }
    }


    suspend fun isProfileSetupComplete(uid: String): Boolean {
        Log.d(TAG, "Checking profileSetupComplete for $uid")
        val doc = db.collection("users").document(uid).get().await()
        val result = doc.getBoolean("profileSetupComplete") ?: false
        Log.i(TAG, "profileSetupComplete = $result for $uid")
        return result
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
        viewModelScope.launch {
            performCalendarCreate(
                context = context,
                account = event.account,
                eventTitle = event.title,
                start = event.start,
                end = event.end,
                description = event.description,
                location = event.location,
                onSuccess = {
                    pendingEvent = null
                    creationStatus = "Session synced to Google Calendar"
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
}

enum class LocationType { IN_PERSON, VIRTUAL }

data class DurationOption(val label: String, val minutes: Int)

data class PendingEvent(
    val account: Account,
    val title: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val description: String?,
    val location: String?
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
            end = end
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
    timeZoneId: String = ZoneId.systemDefault().id
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
        }

        val url = URL("https://www.googleapis.com/calendar/v3/calendars/primary/events")
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
