package com.geardex.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central Firebase access point.
 * Both [auth] and [firestore] are null when Firebase is not configured
 * (i.e. firebase.properties has firebase.enabled=false or is missing).
 */
@Singleton
class FirebaseManager @Inject constructor() {

    val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    val isConfigured: Boolean
        get() = auth != null && firestore != null

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    val isLoggedIn: Boolean
        get() = currentUser != null

    fun signOut() {
        auth?.signOut()
    }
}
