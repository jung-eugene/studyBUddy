package com.example.studybuddy

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG_HOME = "HomeViewModel"
private const val WEIGHT_MAJOR = 6
private const val WEIGHT_COURSE = 4   // per shared course
private const val WEIGHT_YEAR = 3
private const val WEIGHT_AVAILABILITY = 2

data class HomeUiState(
    val candidates: List<User> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val matchRepo: MatchRepository = MatchRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var currentUser: User? = null

    private fun currentUid(): String? = auth.currentUser?.uid

    // ---------------------------------------------------------
    // RECEIVE USERS + FILTER BY SHARED COURSES
    // ---------------------------------------------------------
    fun setCandidates(allUsers: List<User>) {
        Log.d(TAG_HOME, "setCandidates() received ${allUsers.size} users")

        // Do async work (fetch likes/matches) and then update state
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val uid = currentUid()

            // Save current user for filtering
            currentUser = allUsers.find { it.id == uid }

            // Fetch ids to exclude: already liked + already matched + self
            val likedIds = uid?.let { matchRepo.getLikedIdsForUser(it) } ?: emptyList()
            val matchedIds = uid?.let { matchRepo.getMatchIdsForUser(it) } ?: emptyList()
            val excluded = (likedIds + matchedIds + listOfNotNull(uid)).toSet()

            if (uid == null || currentUser == null) {
                Log.w(TAG_HOME, "currentUser not found (currentUid=$uid). Using fallback filtering.")
            }

            val base = allUsers.filter { it.id !in excluded }

            val filtered = if (uid != null && currentUser != null) {
                // Prefer users with same/overlapping major OR at least one shared class.
                val strongMatches = base.filter { user ->
                    hasMajorMatch(user, currentUser!!) ||
                            sharedCourseCount(user, currentUser!!) > 0
                }
                val pool = strongMatches.ifEmpty { base }
                pool.sortedByDescending { candidateScore(it, currentUser!!) }
            } else {
                base
            }

            Log.i(TAG_HOME, "Filtered to ${filtered.size} users (after excluding liked/matched/self)")

            _uiState.value = HomeUiState(
                candidates = filtered,
                currentIndex = 0,
                isLoading = false,
                error = null
            )
        }
    }

    // ---------------------------------------------------------
    // GET CURRENT CANDIDATE
    // ---------------------------------------------------------
    fun getCurrentCandidate(): User? {
        val state = _uiState.value
        return state.candidates.getOrNull(state.currentIndex)
    }

    // ---------------------------------------------------------
    // SKIP CURRENT USER
    // ---------------------------------------------------------
    fun skipCurrent() {
        Log.d(TAG_HOME, "Skipping user...")
        moveToNext()
    }

    // ---------------------------------------------------------
    // LIKE + CALLBACK FOR MATCH POPUP
    // ---------------------------------------------------------
    fun likeCurrent(onMatch: (Boolean, User?) -> Unit) {
        val target = getCurrentCandidate() ?: return
        val myUid = currentUid()
        if (myUid == null) {
            Log.w(TAG_HOME, "Cannot like ${target.id}: no signed-in user")
            return
        }

        Log.d(TAG_HOME, "Liking ${target.id}...")

        viewModelScope.launch {
            try {
                val isMutual = matchRepo.sendLike(myUid, target.id)

                // Optimistically remove the target from local candidates so it won't reappear
                val state = _uiState.value
                val updated = state.candidates.filter { it.id != target.id }
                // Keep currentIndex stable (the next candidate will now occupy this index). If list is empty, index becomes 0.
                val newIndex = if (updated.isEmpty()) 0 else state.currentIndex.coerceAtMost(updated.size - 1)
                _uiState.value = state.copy(candidates = updated, currentIndex = newIndex)

                if (isMutual) {
                    Log.i(TAG_HOME, "MUTUAL MATCH with ${target.name}!")
                    onMatch(true, target)  // return matched user to popup
                } else {
                    onMatch(false, null)
                }

            } catch (e: Exception) {
                Log.e(TAG_HOME, "Error liking ${target.id}", e)
                onMatch(false, null)
            }
        }
    }

    // ---------------------------------------------------------
    // ADVANCE TO NEXT CANDIDATE SAFELY
    // ---------------------------------------------------------
    private fun moveToNext() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1

        if (nextIndex >= state.candidates.size) {
            Log.d(TAG_HOME, "No more candidates!")
            _uiState.value = state.copy(
                currentIndex = state.candidates.size  // stays safe
            )
            return
        }

        Log.d(TAG_HOME, "Moving to card index $nextIndex")

        _uiState.value = state.copy(
            currentIndex = nextIndex
        )
    }

    private fun candidateScore(user: User, current: User): Int {
        var score = 0
        val majorMatch = hasMajorMatch(user, current)
        val sharedCourses = sharedCourseCount(user, current)
        if (majorMatch) score += WEIGHT_MAJOR
        score += sharedCourses * WEIGHT_COURSE
        if (user.year.equals(current.year, ignoreCase = true)) score += WEIGHT_YEAR
        score += availabilityOverlap(user.availability, current.availability) * WEIGHT_AVAILABILITY
        return score
    }

    private fun hasMajorMatch(user: User, current: User): Boolean {
        val majorA = user.major.normalizeForMatch()
        val majorB = current.major.normalizeForMatch()
        if (majorA.isEmpty() || majorB.isEmpty()) return false
        return majorA.contains(majorB) || majorB.contains(majorA)
    }

    private fun sharedCourseCount(user: User, current: User): Int {
        val theirs = user.courses.mapNotNull { it.normalizeForMatch().takeIf { it.isNotEmpty() } }
        val mine = current.courses.mapNotNull { it.normalizeForMatch().takeIf { it.isNotEmpty() } }
        if (theirs.isEmpty() || mine.isEmpty()) return 0

        var count = 0
        for (course in theirs) {
            if (mine.any { course.contains(it) || it.contains(course) }) {
                count++
            }
        }
        return count
    }

    private fun String.normalizeForMatch(): String =
        this.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun availabilityOverlap(a: String, b: String): Int {
        if (a.isBlank() || b.isBlank()) return 0
        val setA = a.split(",").mapNotNull { it.trim().lowercase().takeIf { it.isNotEmpty() } }.toSet()
        val setB = b.split(",").mapNotNull { it.trim().lowercase().takeIf { it.isNotEmpty() } }.toSet()
        return setA.intersect(setB).size
    }
}
