package com.example.studybuddy.profile.screen

import android.accounts.Account
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.studybuddy.BottomNavBar
import com.example.studybuddy.BuildConfig
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "CalendarScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavHostController) {
    var signedInEmail by remember { mutableStateOf<String?>(null) }
    var signedInAccount by remember { mutableStateOf<Account?>(null) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            GoogleAccountSection(
                signedInEmail = signedInEmail,
                onSignedIn = { email, account ->
                    signedInEmail = email
                    signedInAccount = account
                },
                onSignOut = {
                    signedInEmail = null
                    signedInAccount = null
                }
            )
            CalendarEventCreator(
                appSignedInEmail = signedInEmail,
                signedInAccount = signedInAccount
            )
        }
    }
}

@Composable
private fun GoogleAccountSection(
    signedInEmail: String?,
    onSignedIn: (String, Account) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var signingIn by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val acct = task.getResult(ApiException::class.java)
            val email = acct?.email?.trim()
            if (!email.isNullOrBlank()) {
                val account = acct.account ?: Account(email, "com.google")
                onSignedIn(email, account)
                Toast.makeText(context, "Signed in as $email", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to read account email", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Sign-in failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Google sign-in failed", e)
        } finally {
            signingIn = false
        }
    }

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
                text = "Google Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (signedInEmail == null) {
                Text("Not signed in")
                Button(
                    enabled = !signingIn,
                    onClick = {
                        signingIn = true
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events"))
                            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        signInLauncher.launch(client.signInIntent)
                    }
                ) {
                    Text(if (signingIn) "Signing in…" else "Sign in with Google")
                }
            } else {
                Text("Signed in as $signedInEmail")
                Button(
                    enabled = !signingIn,
                    onClick = {
                        signingIn = false
                        val client = GoogleSignIn.getClient(
                            context,
                            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build()
                        )
                        client.signOut()
                        onSignOut()
                        Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Sign out")
                }
            }
        }
    }
}

@android.annotation.SuppressLint("NewApi")
@Composable
private fun CalendarEventCreator(
    appSignedInEmail: String?,
    signedInAccount: Account?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("Study Session") }
    var eventDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember {
        mutableStateOf(LocalTime.now().plusMinutes(30).withSecond(0).withNano(0))
    }
    var endTime by remember { mutableStateOf(startTime.plusHours(1)) }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var creating by remember { mutableStateOf(false) }
    var lastStatus by remember { mutableStateOf<String?>(null) }

    var pendingAccount by remember { mutableStateOf<Account?>(null) }
    var pendingTitle by remember { mutableStateOf<String?>(null) }
    var pendingStart by remember { mutableStateOf<ZonedDateTime?>(null) }
    var pendingEnd by remember { mutableStateOf<ZonedDateTime?>(null) }
    var pendingDescription by remember { mutableStateOf<String?>(null) }
    var pendingLocation by remember { mutableStateOf<String?>(null) }

    lateinit var startCreation: (
        Account,
        String,
        ZonedDateTime,
        ZonedDateTime,
        String,
        String
    ) -> Unit

    val recoverAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val account = pendingAccount
        val titlePending = pendingTitle
        val startPending = pendingStart
        val endPending = pendingEnd
        val descPending = pendingDescription
        val locPending = pendingLocation

        pendingAccount = null
        pendingTitle = null
        pendingStart = null
        pendingEnd = null
        pendingDescription = null
        pendingLocation = null

        if (
            account != null &&
            titlePending != null &&
            startPending != null &&
            endPending != null &&
            descPending != null &&
            locPending != null
        ) {
            startCreation(
                account,
                titlePending,
                startPending,
                endPending,
                descPending,
                locPending
            )
        }
    }

    startCreation = { account, eventTitle, startDateTime, endDateTime, desc, loc ->
        creating = true
        lastStatus = null
        scope.launch {
            performCalendarCreate(
                context = context,
                account = account,
                eventTitle = eventTitle,
                start = startDateTime,
                end = endDateTime,
                description = desc.ifBlank { null },
                location = loc.ifBlank { null },
                onSuccess = { link ->
                    creating = false
                    lastStatus = "Created: $link"
                    Toast.makeText(context, "Event created", Toast.LENGTH_SHORT).show()
                },
                onFailure = { msg ->
                    creating = false
                    lastStatus = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                },
                onRecover = { intent ->
                    creating = false
                    pendingAccount = account
                    pendingTitle = eventTitle
                    pendingStart = startDateTime
                    pendingEnd = endDateTime
                    pendingDescription = desc
                    pendingLocation = loc
                    intent?.let { recoverAuthLauncher.launch(it) } ?: run {
                        lastStatus = "Authorization requires user action"
                        Log.e(TAG, "UserRecoverableAuthException intent was null")
                    }
                }
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            Text(appSignedInEmail?.let { "Signed in as $it" } ?: "Not signed in")

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = { title = it },
                label = { Text("Event title") },
                singleLine = true
            )
            Button(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        eventDate = LocalDate.of(year, month + 1, day)
                    },
                    eventDate.year,
                    eventDate.monthValue - 1,
                    eventDate.dayOfMonth
                ).show()
            }) {
                Text("Date: ${eventDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}")
            }
            Button(onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val picked = LocalTime.of(hour, minute)
                        startTime = picked
                        if (endTime <= picked) {
                            endTime = picked.plusHours(1)
                        }
                    },
                    startTime.hour,
                    startTime.minute,
                    true
                ).show()
            }) {
                Text("Start: ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
            }
            Button(onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        endTime = LocalTime.of(hour, minute)
                    },
                    endTime.hour,
                    endTime.minute,
                    true
                ).show()
            }) {
                Text("End: ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
            }
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") }
            )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (optional)") },
                singleLine = true
            )
            Button(
                enabled = !creating && signedInAccount != null && appSignedInEmail != null,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
                onClick = {
                    val account = signedInAccount
                    if (account == null) {
                        lastStatus = "Sign in first"
                        return@Button
                    }
                    val eventTitle = title.ifBlank { "Study Session (test)" }
                    val startDateTime = ZonedDateTime.of(eventDate, startTime, ZoneId.systemDefault())
                    val adjustedEndTime = if (endTime <= startTime) {
                        ZonedDateTime.of(eventDate.plusDays(1), endTime, ZoneId.systemDefault())
                    } else {
                        ZonedDateTime.of(eventDate, endTime, ZoneId.systemDefault())
                    }
                    startCreation(
                        account,
                        eventTitle,
                        startDateTime,
                        adjustedEndTime,
                        description,
                        location
                    )
                }
            ) {
                Text(if (creating) "Creating…" else "Create calendar event")
            }
            lastStatus?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@android.annotation.SuppressLint("NewApi")
private suspend fun performCalendarCreate(
    context: android.content.Context,
    account: Account,
    eventTitle: String,
    start: ZonedDateTime,
    end: ZonedDateTime,
    description: String?,
    location: String?,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
    onRecover: (android.content.Intent?) -> Unit
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

@android.annotation.SuppressLint("NewApi")
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
        Log.d(TAG, "Calendar create response ($code): $response")
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
