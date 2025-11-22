package com.example.studybuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ------------------------------
// Holds temporary profile-setup user input
// ------------------------------
data class ProfileSetupState(
    val name: String = "",
    val major: String = "",
    val year: String = "",
    val courses: List<String> = emptyList(),
    val availability: List<String> = emptyList(),
    val preferences: List<String> = emptyList(),
    val bio: String = ""
)

// ------------------------------
// ViewModel handling 4-step onboarding flow
// ------------------------------
class ProfileSetupViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupState())
    val state: StateFlow<ProfileSetupState> = _state

    // Notifies UI when save is complete
    private val _profileSaved = MutableStateFlow(false)
    val profileSaved: StateFlow<Boolean> = _profileSaved

    // --------------------------
    // Update helper functions
    // --------------------------
    fun updateName(v: String) = _state.update { it.copy(name = v) }
    fun updateMajor(v: String) = _state.update { it.copy(major = v) }
    fun updateYear(v: String) = _state.update { it.copy(year = v) }
    fun updateBio(v: String) = _state.update { it.copy(bio = v) }

    fun addCourse(course: String) =
        _state.update { it.copy(courses = it.courses + course) }

    fun removeCourse(course: String) =
        _state.update { it.copy(courses = it.courses - course) }

    fun toggleAvailability(slot: String) = _state.update {
        val updated = it.availability.toMutableList()
        if (slot in updated) updated.remove(slot) else updated.add(slot)
        it.copy(availability = updated)
    }

    fun togglePref(pref: String) = _state.update {
        val updated = it.preferences.toMutableList()
        if (pref in updated) updated.remove(pref) else updated.add(pref)
        it.copy(preferences = updated)
    }

    // --------------------------
    // Create Firestore User document
    // --------------------------
    fun completeProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val profile = _state.value

        val user = User(
            id = uid,
            name = profile.name,
            major = profile.major,
            year = profile.year,
            courses = profile.courses,
            availability = profile.availability.joinToString(", "),
            studyPreferences = profile.preferences,
            bio = profile.bio,
            photoUrl = "",        // required so Firestore never stores null
            darkMode = false,     // required default since your VM loads this
            profileSetupComplete = true
        )

        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(user)
                    .await()
                _profileSaved.value = true  // triggers navigation in UI
            } catch (e: Exception) {
                e.printStackTrace()
                // OPTIONAL — Add an error flow if needed
            }
        }
    }
}