package com.geardex.app.ui.parking

import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.geardex.app.data.local.entity.ParkingSpot
import com.geardex.app.data.repository.ParkingSpotRepository
import com.geardex.app.notifications.ParkingMeterWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ParkingViewModel @Inject constructor(
    private val repo: ParkingSpotRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val spots = repo.getAllSpots()

    private val _currentLat = MutableStateFlow(0.0)
    val currentLat: StateFlow<Double> = _currentLat.asStateFlow()

    private val _currentLng = MutableStateFlow(0.0)

    private val _detectedAddress = MutableStateFlow("")
    val detectedAddress: StateFlow<String> = _detectedAddress.asStateFlow()

    private var meterExpiryMs: Long = 0L

    fun setMeterExpiry(hour: Int, minute: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        meterExpiryMs = cal.timeInMillis
    }

    @Suppress("MissingPermission")
    fun fetchCurrentLocation(context: Context) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                _currentLat.value = location.latitude
                _currentLng.value = location.longitude
                resolveAddress(context, location.latitude, location.longitude)
            }
        } catch (_: Exception) {}
    }

    @Suppress("DEPRECATION")
    private fun resolveAddress(context: Context, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        _detectedAddress.value = addresses.firstOrNull()?.getAddressLine(0) ?: ""
                    }
                } else {
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    _detectedAddress.value = addresses?.firstOrNull()?.getAddressLine(0) ?: ""
                }
            } catch (_: Exception) {}
        }
    }

    fun saveCurrent(address: String, notes: String) {
        viewModelScope.launch {
            val spot = ParkingSpot(
                latitude = _currentLat.value,
                longitude = _currentLng.value,
                address = address,
                notes = notes,
                meterExpiryMs = meterExpiryMs,
                savedAt = System.currentTimeMillis()
            )
            repo.saveSpot(spot)
            if (meterExpiryMs > System.currentTimeMillis()) {
                scheduleMeterWork(address)
            }
            meterExpiryMs = 0L
            _currentLat.value = 0.0
            _currentLng.value = 0.0
            _detectedAddress.value = ""
        }
    }

    private fun scheduleMeterWork(address: String) {
        val warningMs = 5 * 60 * 1000L
        val delay = meterExpiryMs - System.currentTimeMillis() - warningMs
        if (delay > 0) {
            val request = OneTimeWorkRequestBuilder<ParkingMeterWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ParkingMeterWorker.KEY_ADDRESS to address))
                .build()
            WorkManager.getInstance(appContext)
                .enqueueUniqueWork("parking_meter", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun deleteSpot(spot: ParkingSpot) {
        viewModelScope.launch { repo.deleteSpot(spot) }
    }
}
