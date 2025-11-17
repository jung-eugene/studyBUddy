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

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode


    fun loadDarkMode(uid: String) {
        viewModelScope.launch {
            val doc = db.collection("users").document(uid).get().await()
            _darkMode.value = doc.getBoolean("darkMode") ?: false
        }
    }

    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                val user = doc.toObject(User::class.java)
                _userProfile.value = user
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Save updated profile (used by EditProfileScreen)
    fun saveUserProfile(uid: String, updatedUser: User) {
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .set(updatedUser, SetOptions.merge())
                    .await()
                _userProfile.value = updatedUser
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun updateDarkMode(uid: String, dark: Boolean) {
        viewModelScope.launch {
            try {
                db.collection("users").document(uid).update("darkMode", dark).await()
                _darkMode.value = dark  // Update state
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getDarkMode(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                val dark = doc.getBoolean("darkMode") ?: false
                _darkMode.value = dark
            } catch (e: Exception) {
                _darkMode.value = false
            }
        }
    }
    suspend fun getAllUsers(): List<User> {
        val snapshot = db.collection("users").get().await()
        return snapshot.toObjects(User::class.java)
    }

    suspend fun isProfileSetupComplete(uid: String): Boolean {
        val doc = db.collection("users").document(uid).get().await()
        return doc.getBoolean("profileSetupComplete") ?: false
    }

}

/**
 * Stores and updates in-app study sessions.
 * Later will sync with Google Calendar API.
 * Aaron will implement his code viewmodel here
 */
//class CalendarViewModel : ViewModel() {
//    private val _events = MutableStateFlow<List<String>>(emptyList())
//    val events: StateFlow<List<String>> get() = _events
//
//    fun addEvent(title: String, dateTime: String) {
//        _events.value = _events.value + "$title • $dateTime"
//    }
//}
