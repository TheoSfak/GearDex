package com.geardex.app.data.remote

import android.content.Context
import android.util.Log
import com.geardex.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseManager"

/**
 * Central Firebase access point.
 * Both [auth] and [firestore] are null when Firebase is not configured
 * (i.e. firebase.properties has firebase.enabled=false or is missing).
 */
@Singleton
class FirebaseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val hasConfiguredApp: Boolean
        get() = BuildConfig.FIREBASE_ENABLED &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
            FirebaseApp.getApps(context).isNotEmpty()

    val auth: FirebaseAuth?
        get() = if (hasConfiguredApp) {
            runCatching { FirebaseAuth.getInstance() }
                .onFailure { Log.w(TAG, "Firebase Auth unavailable", it) }
                .getOrNull()
        } else {
            null
        }

    val firestore: FirebaseFirestore?
        get() = if (hasConfiguredApp) {
            runCatching { FirebaseFirestore.getInstance() }
                .onFailure { Log.w(TAG, "Firestore unavailable", it) }
                .getOrNull()
        } else {
            null
        }

    val isConfigured: Boolean
        get() = hasConfiguredApp && auth != null && firestore != null

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
