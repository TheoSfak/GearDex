package com.geardex.app.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.remote.FirebaseManager
import com.geardex.app.data.remote.SyncManager
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.PdfReportGenerator
import com.geardex.app.data.repository.VehicleRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    data class LoggedIn(val user: FirebaseUser) : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val firebaseManager: FirebaseManager,
    private val vehicleRepository: VehicleRepository,
    private val syncManager: SyncManager,
    private val logRepository: LogRepository,
    private val pdfReportGenerator: PdfReportGenerator,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(
        if (firebaseManager.isLoggedIn)
            AuthState.LoggedIn(firebaseManager.currentUser!!)
        else
            AuthState.LoggedOut
    )
    val authState: StateFlow<AuthState> = _authState

    private val _syncMessage = MutableSharedFlow<String>()
    val syncMessage: SharedFlow<String> = _syncMessage

    val isFirebaseConfigured: Boolean get() = firebaseManager.isConfigured

    val webClientId: String get() = firebaseManager.webClientId

    fun signIn(email: String, password: String) {
        val auth = firebaseManager.auth ?: return
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Authentication failed")
                _authState.value = AuthState.LoggedIn(user)
                runCatching { syncManager.syncAfterLogin() }
                _syncMessage.emit("Sync complete")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun register(email: String, password: String) {
        val auth = firebaseManager.auth ?: return
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Registration failed")
                _authState.value = AuthState.LoggedIn(user)
                runCatching { syncManager.syncAfterLogin() }
                _syncMessage.emit("Account created. Sync complete")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        val auth = firebaseManager.auth ?: return
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: throw Exception("Google sign-in failed")
                _authState.value = AuthState.LoggedIn(user)
                runCatching { syncManager.syncAfterLogin() }
                _syncMessage.emit("Sync complete")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google sign-in failed")
            }
        }
    }

    fun signOut() {
        firebaseManager.signOut()
        _authState.value = AuthState.LoggedOut
    }

    fun syncNow() {
        if (!firebaseManager.isLoggedIn) return
        viewModelScope.launch {
            try {
                syncManager.uploadAll()
                _syncMessage.emit("Sync complete")
            } catch (e: Exception) {
                _syncMessage.emit("Sync failed: ${e.message}")
            }
        }
    }

    suspend fun exportAllVehiclesPdf(): java.io.File? {
        val vehicles = vehicleRepository.getAllVehiclesSync()
        if (vehicles.isEmpty()) return null
        val fuelMap = mutableMapOf<Long, List<com.geardex.app.data.local.entity.FuelLog>>()
        val serviceMap = mutableMapOf<Long, List<com.geardex.app.data.local.entity.ServiceLog>>()
        for (v in vehicles) {
            fuelMap[v.id] = logRepository.getFuelLogsSync(v.id)
            serviceMap[v.id] = logRepository.getServiceLogsSync(v.id)
        }
        return pdfReportGenerator.generateAllVehiclesReport(vehicles, fuelMap, serviceMap)
    }

    fun getSavedApiKey(): String? = prefs.getString("google_maps_api_key", null)

    fun saveApiKey(key: String) {
        prefs.edit().putString("google_maps_api_key", key).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove("google_maps_api_key").apply()
    }
}
