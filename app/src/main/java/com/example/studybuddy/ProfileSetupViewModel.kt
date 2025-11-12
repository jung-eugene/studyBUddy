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

//Handles temporary user input during the 4-step (now unified) profile setup flow, then saves to Firestore
data class ProfileSetupState(
    val name: String = "",
    val major: String = "",
    val year: String = "",
    val courses: List<String> = emptyList(),
    val availability: List<String> = emptyList(),
    val preferences: List<String> = emptyList(),
    val bio: String = ""
)

class ProfileSetupViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupState())
    val state: StateFlow<ProfileSetupState> = _state

    //track when save completes
    private val _profileSaved = MutableStateFlow(false)
    val profileSaved: StateFlow<Boolean> = _profileSaved

    // --- Update helpers ----
    fun updateName(v: String) = _state.update { it.copy(name = v) }
    fun updateMajor(v: String) = _state.update { it.copy(major = v) }
    fun updateYear(v: String) = _state.update { it.copy(year = v) }
    fun addCourse(c: String) = _state.update { it.copy(courses = it.courses + c) }
    fun toggleAvailability(day: String) = _state.update {
        val list = it.availability.toMutableList()
        if (day in list) list.remove(day) else list.add(day)
        it.copy(availability = list)
    }
    fun togglePref(pref: String) = _state.update {
        val list = it.preferences.toMutableList()
        if (pref in list) list.remove(pref) else list.add(pref)
        it.copy(preferences = list)
    }
    fun updateBio(v: String) = _state.update { it.copy(bio = v) }

    // --- Save to Firestore ---
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
            profileSetupComplete = true
        )

        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(user)
                    .await()           // suspends until Firestore finishes
                _profileSaved.value = true  // trigger UI navigation
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
