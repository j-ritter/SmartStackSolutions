package com.ritter.smartstackbills

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirebaseUserProfile {
    private const val TAG = "FirebaseUserProfile"

    fun ensureExists(user: FirebaseUser?, provider: String): Task<Void>? {
        if (user == null) {
            return null
        }

        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)

        return userRef.get().continueWithTask { snapshotTask ->
            if (!snapshotTask.isSuccessful) {
                return@continueWithTask Tasks.forException<Void>(
                    snapshotTask.exception ?: IllegalStateException("Could not load user profile.")
                )
            }

            val profileData = mutableMapOf<String, Any>(
                "uid" to user.uid,
                "email" to user.email.orEmpty(),
                "displayName" to user.displayName.orEmpty(),
                "provider" to provider,
                "lastLoginAt" to FieldValue.serverTimestamp()
            )

            if (!snapshotTask.result.exists()) {
                profileData["createdAt"] = FieldValue.serverTimestamp()
            }

            userRef.set(profileData, SetOptions.merge())
        }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Could not ensure user profile document.", exception)
            }
    }
}
