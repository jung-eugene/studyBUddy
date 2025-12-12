package com.example.studybuddy

import android.util.Log
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
    val availabilitySlots: List<AvailabilitySlot> = emptyList(),
    val preferences: List<String> = emptyList(),
    val bio: String = ""
)

// ------------------------------
// ViewModel handling 4-step onboarding flow
// ------------------------------
class ProfileSetupViewModel : ViewModel() {
    private val TAG = "SetupVM"
    private val _state = MutableStateFlow(ProfileSetupState())
    val state: StateFlow<ProfileSetupState> = _state

    // Notifies UI when save is complete
    private val _profileSaved = MutableStateFlow(false)
    val profileSaved: StateFlow<Boolean> = _profileSaved

    // --------------------------
    // Update helper functions
    // --------------------------
    fun updateName(v: String) {
        Log.d(TAG, "updateName: $v")
        _state.update { it.copy(name = v) }
    }
    fun updateMajor(v: String) {
        Log.d(TAG, "updateMajor: $v")
        _state.update { it.copy(major = v) }
    }

    fun updateYear(v: String) {
        Log.d(TAG, "updateYear: $v")
        _state.update { it.copy(year = v) }
    }

    fun updateBio(v: String) {
        Log.d(TAG, "updateBio: $v")
        _state.update { it.copy(bio = v) }
    }

    fun addCourse(course: String) {
        Log.d(TAG, "addCourse: $course")
        _state.update { it.copy(courses = it.courses + course) }
    }

    fun removeCourse(course: String) =
        _state.update { it.copy(courses = it.courses - course) }

    fun toggleAvailability(slot: AvailabilitySlot) {
        Log.d(TAG, "toggleAvailability: $slot")
        _state.update {
            val updated = it.availabilitySlots.toMutableList()
            if (slot in updated) updated.remove(slot) else updated.add(slot)
            it.copy(availabilitySlots = updated)
        }
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
        Log.i(TAG, "Saving complete profile for $uid")
        val profile = _state.value

        val user = User(
            id = uid,
            name = profile.name,
            major = profile.major,
            year = profile.year,
            courses = profile.courses,
            availabilitySlots = profile.availabilitySlots,
            studyPreferences = profile.preferences,
            bio = profile.bio,
            photoUrl = "",
            darkMode = false,
            profileSetupComplete = true
        )

        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(user)
                    .await()
                Log.i(TAG, "Profile setup saved successfully for $uid")
                _profileSaved.value = true  // triggers navigation in UI
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile setup for $uid", e)
            }
        }
    }
}