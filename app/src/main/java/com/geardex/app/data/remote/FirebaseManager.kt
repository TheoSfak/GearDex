package com.geardex.app.data.remote

import android.util.Log
import com.geardex.app.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseManager"

/**
 * Central Firebase access point.
 * Both [auth] and [firestore] are null when Firebase is not configured
 * (i.e. firebase.properties has firebase.enabled=false or is missing).
 */
@Singleton
class FirebaseManager @Inject constructor() {

    val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }
        .onFailure { Log.w(TAG, "Firebase Auth unavailable", it) }
        .getOrNull()
    val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }
        .onFailure { Log.w(TAG, "Firestore unavailable", it) }
        .getOrNull()

    val isConfigured: Boolean
        get() = auth != null && firestore != null

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    val isLoggedIn: Boolean
        get() = currentUser != null

    val webClientId: String
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID

    fun signOut() {
        auth?.signOut()
    }
}
