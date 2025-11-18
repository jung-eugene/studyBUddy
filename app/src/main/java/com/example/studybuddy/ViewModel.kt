package com.example.studybuddy

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch

// Manages Firebase user data, like fetching all users info, updating user profile completion

// --------------------------
// USER DATA CLASS
// --------------------------
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

// --------------------------
// UI STATE FOR THE USER VM
// --------------------------
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
    private val auth = FirebaseAuth.getInstance()

    // --- LOGIN ---
    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    // --- SIGN UP ---
    fun signup(email: String, password: String, onResult: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    // --- PASSWORD RESET ---
    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful)
                    onResult(true, "Password reset email sent to $email.")
                else
                    onResult(false, task.exception?.message ?: "Error sending reset email.")
            }
    }


    // --- SESSION HELPERS ---
    fun currentUser() = auth.currentUser
    fun signOut() = auth.signOut()
}

/**
 * Manages user profiles and Firestore interaction.
 */

class UserViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState

    private fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }

    private fun setError(message: String?) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    // ---------------------------------------------------------
    // LOAD CURRENT USER PROFILE
    // ---------------------------------------------------------
    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)

            try {
                val doc = db.collection("users").document(uid).get().await()
                val user = doc.toObject(User::class.java)

                _uiState.value = _uiState.value.copy(
                    user = user,
                    darkMode = user?.darkMode ?: false,
                    isLoading = false
                )

            } catch (e: Exception) {
                setLoading(false)
                setError(e.message)
            }
        }
    }

    // ---------------------------------------------------------
    // SAVE / UPDATE PROFILE
    // ---------------------------------------------------------
    fun saveUserProfile(uid: String, updatedUser: User) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)

            try {
                db.collection("users").document(uid)
                    .set(updatedUser, SetOptions.merge())
                    .await()

                _uiState.value = _uiState.value.copy(
                    user = updatedUser,
                    isLoading = false
                )

            } catch (e: Exception) {
                setLoading(false)
                setError(e.message)
            }
        }
    }

    // ---------------------------------------------------------
    // DARK MODE TOGGLE
    // ---------------------------------------------------------
    fun getDarkMode(uid: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .update("darkMode", enabled)
                    .await()

                _uiState.value = _uiState.value.copy(darkMode = enabled)

            } catch (e: Exception) {
                setError(e.message)
            }
        }
    }

    // ---------------------------------------------------------
    // LOAD ALL USERS (Used for Home / Matches)
    // ---------------------------------------------------------
    fun getAllUsers() {
        viewModelScope.launch {
            setLoading(true)

            try {
                val snapshot = db.collection("users").get().await()
                val users = snapshot.toObjects(User::class.java)

                _uiState.value = _uiState.value.copy(
                    allUsers = users,
                    isLoading = false
                )

            } catch (e: Exception) {
                setLoading(false)
                setError(e.message)
            }
        }
    }


    suspend fun isProfileSetupComplete(uid: String): Boolean {
        val doc = db.collection("users").document(uid).get().await()
        return doc.getBoolean("profileSetupComplete") ?: false
    }

}