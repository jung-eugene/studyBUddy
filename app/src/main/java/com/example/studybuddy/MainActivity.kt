package com.example.studybuddy

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import com.example.studybuddy.ui.theme.StudyBUddyTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.credentials.exceptions.*

private const val TAG = "SignIn"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyBUddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(Modifier.padding(innerPadding)) {
                        SignInWithGoogleButton(
                            activity = this@MainActivity,
                            onIdToken = { token ->
                                Log.d(TAG, "Got ID token (len=${token.length}) — send to backend")
                            },
                            onError = { e ->
                                Log.e(TAG, "Sign-in error: ${e.message}", e)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SignInWithGoogleButton(
    activity: Activity,
    onIdToken: (String) -> Unit,
    onError: (Throwable) -> Unit
) {
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(activity) }
    val signedInLabel = remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    val googleIdOption = remember {
        GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
    }
    val request = remember {
        GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    }

    Column {
        Button(onClick = {
            scope.launch {
                try {
                    val result = credentialManager.getCredential(activity, request)
                    when (val cred = result.credential) {
                        is androidx.credentials.CustomCredential -> {
                            if (cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val g = GoogleIdTokenCredential.createFrom(cred.data)
                                val idToken = g.idToken
                                val name = g.displayName ?: g.givenName
                                val email = g.id
                                signedInLabel.value = "$name ($email)"
                                onIdToken(idToken) // send to backend for verification
                            }
                        }
                        else -> { /* ignore other types in this flow */ }
                    }
                } catch (e: GetCredentialException) {
                    onError(e)
                }
            }
        }) { Text("Sign in with Google") }
        signedInLabel.value?.let {
            label -> Text("Signed in as $label")
        }
    }
}



@Preview(showBackground = true)
@Composable
fun PreviewSignIn() {
    val context = LocalContext.current
    StudyBUddyTheme {
        SignInWithGoogleButton(activity = context as Activity, onIdToken = {}, onError = {})
    }
}
