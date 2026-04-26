package com.geardex.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.BuildConfig
import com.geardex.app.data.remote.FirebaseManager
import com.geardex.app.data.remote.SyncManager
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.PdfReportGenerator
import com.geardex.app.data.repository.VehicleRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    data class LoggedIn(val user: FirebaseUser) : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UpdateRelease(
    val version: String,
    val apkUrl: String
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpToDate(val latestVersion: String) : UpdateState()
    data class Available(val release: UpdateRelease) : UpdateState()
    data class Downloading(val release: UpdateRelease) : UpdateState()
    data class Downloaded(val file: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

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
        prefs.edit() {putString("google_maps_api_key", key)}
    }

    fun clearApiKey() {
        prefs.edit() {remove("google_maps_api_key")}
    }

    fun checkForUpdates() {
        if (!BuildConfig.ENABLE_UPDATE_CHECK) return
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val result = runCatching { fetchLatestRelease() }
            val release = result.getOrElse {
                _updateState.value = UpdateState.Error(it.message ?: "Network error")
                return@launch
            }
            if (release == null) {
                _updateState.value = UpdateState.Error("No APK found")
            } else if (isNewer(release.version, BuildConfig.VERSION_NAME)) {
                _updateState.value = UpdateState.Available(release)
            } else {
                _updateState.value = UpdateState.UpToDate(release.version)
            }
        }
    }

    fun downloadUpdate(release: UpdateRelease) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Downloading(release)
            val result = runCatching { downloadApk(release) }
            _updateState.value = result.fold(
                onSuccess = { UpdateState.Downloaded(it) },
                onFailure = { UpdateState.Error(it.message ?: "Download failed") }
            )
        }
    }

    private suspend fun fetchLatestRelease(): UpdateRelease? = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
        val json = openConnection(url) { connection ->
            connection.inputStream.bufferedReader().use { it.readText() }
        }
        val release = JSONObject(json)
        val version = release.optString("tag_name").trimStart('v')
        val assets = release.optJSONArray("assets") ?: return@withContext null
        var fallbackUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            val assetUrl = asset.optString("browser_download_url")
            if (!name.endsWith(".apk", ignoreCase = true) || assetUrl.isBlank()) continue
            if (name.contains("github", ignoreCase = true)) {
                return@withContext UpdateRelease(version, assetUrl)
            }
            fallbackUrl = fallbackUrl ?: assetUrl
        }
        fallbackUrl?.let { UpdateRelease(version, it) }
    }

    private suspend fun downloadApk(release: UpdateRelease): File = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "updates/GearDex-${release.version}.apk")
        target.parentFile?.mkdirs()
        openConnection(URL(release.apkUrl)) { connection ->
            connection.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        target
    }

    private fun <T> openConnection(url: URL, block: (HttpURLConnection) -> T): T {
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            block(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun isNewer(candidate: String, current: String): Boolean {
        val c = candidate.split(".").mapNotNull { it.toIntOrNull() }
        val cur = current.split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(c.size, cur.size)
        for (i in 0 until size) {
            val cv = c.getOrElse(i) { 0 }
            val curv = cur.getOrElse(i) { 0 }
            if (cv > curv) return true
            if (cv < curv) return false
        }
        return false
    }
}
