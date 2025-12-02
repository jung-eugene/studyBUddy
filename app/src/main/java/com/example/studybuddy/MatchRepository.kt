package com.example.studybuddy

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val TAG_MATCH = "MatchRepository"

/**
 * Handles all Firestore reads/writes related to likes and matches.
 *
 * Firestore structure:
 *  - likes/{uid}/liked/{otherUid}
 *  - matches/{uid}/paired/{otherUid}
 */
class MatchRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Records that [currentUid] liked [targetUid].
     * If the target has already liked current, creates a mutual match for both users.
     *
     * @return true if this action created a NEW mutual match, false otherwise.
     */
    suspend fun sendLike(currentUid: String, targetUid: String): Boolean {
        Log.d(TAG_MATCH, "sendLike: $currentUid -> $targetUid")

        return try {
            // --------
            // 1) Write like doc
            // --------
            val likeRef = db.collection("likes")
                .document(currentUid)
                .collection("liked")
                .document(targetUid)

            likeRef.set(
                mapOf(
                    "likedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Log.d(TAG_MATCH, "Like saved for $currentUid → $targetUid")

            // --------
            // 2) Check reciprocal like
            // --------
            val reciprocalLike = db.collection("likes")
                .document(targetUid)
                .collection("liked")
                .document(currentUid)
                .get()
                .await()

            val isMutual = reciprocalLike.exists()
            Log.d(TAG_MATCH, "Reciprocal like for $currentUid & $targetUid = $isMutual")

            if (isMutual) {
                createMutualMatch(currentUid, targetUid)
            }

            isMutual

        } catch (e: Exception) {
            Log.e(TAG_MATCH, "sendLike ERROR: $currentUid -> $targetUid", e)
            false
        }
    }

    /**
     * Creates match docs for both users under /matches/{uid}/paired/{otherUid}
     */
    private suspend fun createMutualMatch(uidA: String, uidB: String) {
        Log.d(TAG_MATCH, "createMutualMatch: $uidA <-> $uidB")

        try {
            val matchA = db.collection("matches")
                .document(uidA)
                .collection("paired")
                .document(uidB)

            val matchB = db.collection("matches")
                .document(uidB)
                .collection("paired")
                .document(uidA)

            val payloadA = mapOf(
                "userId" to uidB,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val payloadB = mapOf(
                "userId" to uidA,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.runBatch { batch ->
                batch.set(matchA, payloadA)
                batch.set(matchB, payloadB)
            }.await()

            Log.d(TAG_MATCH, "Match successfully created between $uidA and $uidB")

        } catch (e: Exception) {
            Log.e(TAG_MATCH, "createMutualMatch ERROR: $uidA ↔ $uidB", e)
        }
    }

    /**
     * Returns list of matched userIds for the given [uid].
     */
    suspend fun getMatchIdsForUser(uid: String): List<String> {
        Log.d(TAG_MATCH, "getMatchIdsForUser: uid=$uid")

        return try {
            val snapshot = db.collection("matches")
                .document(uid)
                .collection("paired")
                .get()
                .await()

            val ids = snapshot.documents.map { it.id }
            Log.d(TAG_MATCH, "getMatchIdsForUser: found ${ids.size} matches → $ids")

            ids

        } catch (e: Exception) {
            Log.e(TAG_MATCH, "getMatchIdsForUser ERROR for $uid", e)
            emptyList()
        }
    }

    /**
     * Returns list of userIds that [uid] has liked (stored under /likes/{uid}/liked/{otherUid}).
     */
    suspend fun getLikedIdsForUser(uid: String): List<String> {
        Log.d(TAG_MATCH, "getLikedIdsForUser: uid=$uid")

        return try {
            val snapshot = db.collection("likes")
                .document(uid)
                .collection("liked")
                .get()
                .await()

            val ids = snapshot.documents.map { it.id }
            Log.d(TAG_MATCH, "getLikedIdsForUser: found ${ids.size} likes → $ids")

            ids

        } catch (e: Exception) {
            Log.e(TAG_MATCH, "getLikedIdsForUser ERROR for $uid", e)
            emptyList()
        }
    }
}
