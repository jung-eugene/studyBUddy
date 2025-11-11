package com.example.studybuddy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.accounts.Account
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studybuddy.ui.theme.StudyBUddyTheme
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

private const val TAG = "StudyBuddy"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyBUddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(Modifier.padding(innerPadding)) {
                        // states for Google Sign-In
                        var signedInEmail by remember { mutableStateOf<String?>(null) }
                        var signedInAccount by remember { mutableStateOf<Account?>(null) }

                        AppSignInSection( // call AppSignInSection composable
                            signedInEmail = signedInEmail,
                            onSignIn = { email, account ->
                                signedInEmail = email
                                signedInAccount = account
                            },
                            onSignOut = {
                                signedInEmail = null
                                signedInAccount = null
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        CalendarTestScreen( // call calendar make event composable
                            appSignedInEmail = signedInEmail,
                            signedInAccount = signedInAccount
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppSignInSection(
    signedInEmail: String?,
    onSignIn: (String, Account) -> Unit,
    onSignOut: () -> Unit
) {
    //get Context, object the provides access to global information -> launch new activity, show toasts, etc.
    val context = LocalContext.current
    var signingIn by remember { mutableStateOf(false) } // state to keep track of signin progress

    //providence Intent to launch another activity
    //rememberLauncherForActivityResult's contract<- what job it will do
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult() // contract self explanatory
    ) { result -> // function that executes when sign in closes
        try {
            //get the task
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            //get the account from the task
            val acct = task.getResult(ApiException::class.java)
            //get email
            val acctEmail = acct?.email?.trim()
            //check valid email
            if (!acctEmail.isNullOrBlank()) {
                val account = acct.account ?: Account(acctEmail, "com.google")
                onSignIn(acctEmail, account) //pass information up
                Toast.makeText(context, "Signed in as $acctEmail", Toast.LENGTH_SHORT).show()
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

    Column(modifier = Modifier.padding(16.dp)) {
        if (signedInEmail == null) {
            Text("Not signed in")
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !signingIn,
                onClick = {
                    signingIn = true
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events"))
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    //needs context to access google play services (for email), app package name, etc
                    signInLauncher.launch(client.signInIntent)
                }
            ) {
                Text(if (signingIn) "Signing in…" else "Sign in with Google")
            }
        } else { //signed in is not null == user is signed in
            Text("Signed in as $signedInEmail")
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                signingIn = false
                val client = GoogleSignIn.getClient(
                    context,
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
                )
                client.signOut() //logout
                onSignOut() //update states
            }) {
                Text("Sign Out")
            }
        }
    }
}

@android.annotation.SuppressLint("NewApi")
@Composable
fun CalendarTestScreen(appSignedInEmail: String?, signedInAccount: Account?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    //fields for creating an event
    var title by remember { mutableStateOf("Study Session") }
    var eventDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember {
        mutableStateOf(LocalTime.now().plusMinutes(30).withSecond(0).withNano(0))
    }
    var endTime by remember { mutableStateOf(startTime.plusHours(1)) }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // status for ui
    var creating by remember { mutableStateOf(false) }
    var lastStatus by remember { mutableStateOf<String?>(null) }

    // error handling
    var pendingAccount by remember { mutableStateOf<Account?>(null) }
    var pendingTitle by remember { mutableStateOf<String?>(null) }
    var pendingStart by remember { mutableStateOf<ZonedDateTime?>(null) }
    var pendingEnd by remember { mutableStateOf<ZonedDateTime?>(null) }
    var pendingDescription by remember { mutableStateOf<String?>(null) }
    var pendingLocation by remember { mutableStateOf<String?>(null) }

    // hold logic, 'lateinit' = give value before using it
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
        //retrieve from pending if needed
        val account = pendingAccount
        val titlePending = pendingTitle
        val startPending = pendingStart
        val endPending = pendingEnd
        val descPending = pendingDescription
        val locPending = pendingLocation

        //clear it
        pendingAccount = null
        pendingTitle = null
        pendingStart = null
        pendingEnd = null
        pendingDescription = null
        pendingLocation = null

        if ( //check all fields are valid
            account != null &&
            titlePending != null &&
            startPending != null &&
            endPending != null &&
            descPending != null &&
            locPending != null
        ) { //retry
            startCreation(account, titlePending, startPending, endPending, descPending, locPending)
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
                    //retry
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

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Google Calendar: Create event for signed-in email")
        Text(appSignedInEmail?.let { "Signed in as $it" } ?: "Not signed in")
        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Event title") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    eventDate = LocalDate.of(year, month + 1, day) // +1 to offset 0 indexing
                },
                eventDate.year,
                eventDate.monthValue - 1,
                eventDate.dayOfMonth
            ).show()
        }) {
            Text("Date: ${eventDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}")
        }
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(8.dp))
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") }
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location (optional)") },
            singleLine = true
        )
        Button(
            enabled = !creating && signedInAccount != null && appSignedInEmail != null,
            onClick = {
                val account = signedInAccount
                if (account == null) {
                    lastStatus = "Sign in first"
                    return@Button
                }
                val eventTitle = title.ifBlank { "Study Session (test)" }
                val startDateTime = ZonedDateTime.of(eventDate, startTime, ZoneId.systemDefault())
                val endDateTime = if (endTime <= startTime) {
                    ZonedDateTime.of(eventDate.plusDays(1), endTime, ZoneId.systemDefault())
                } else {
                    ZonedDateTime.of(eventDate, endTime, ZoneId.systemDefault())
                }
                startCreation(account, eventTitle, startDateTime, endDateTime, description, location)
            }
        ) {
            Text(if (creating) "Creating…" else "Create Test Event")
        }
        lastStatus?.let { Text(it) }
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
        result.onSuccess { onSuccess(it) }
            .onFailure { onFailure("Error: ${it.message}") }
    } catch (e: UserRecoverableAuthException) {
        onRecover(e.intent)
    } catch (e: GoogleAuthException) {
        onFailure("Auth error: ${e.message}")
    } catch (t: Throwable) {
        onFailure("Unexpected error: ${t.message}")
    }
}

@android.annotation.SuppressLint("NewApi")
suspend fun createGoogleCalendarEvent(
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

