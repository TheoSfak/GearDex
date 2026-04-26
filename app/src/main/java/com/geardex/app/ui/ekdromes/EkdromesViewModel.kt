package com.geardex.app.ui.ekdromes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.model.EkdromeDifficulty
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.data.model.EkdromeTag
import com.geardex.app.data.model.RouteReview
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.data.repository.BuiltinRouteRepository
import com.geardex.app.data.repository.CustomRouteRepository
import com.geardex.app.data.repository.LocalRouteReviewRepository
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.SavedRouteRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RouteTab { BUILTIN, COMMUNITY, SAVED }

@HiltViewModel
class EkdromesViewModel @Inject constructor(
    private val logRepository: LogRepository,
    private val vehicleRepository: VehicleRepository,
    private val builtinRouteRepository: BuiltinRouteRepository,
    private val savedRouteRepository: SavedRouteRepository,
    private val customRouteRepository: CustomRouteRepository,
    private val localRouteReviewRepository: LocalRouteReviewRepository
) : ViewModel() {

    private val _selectedRegion = MutableStateFlow(EkdromeRegion.ALL)
    val selectedRegion: StateFlow<EkdromeRegion> = _selectedRegion

    private val _selectedTab = MutableStateFlow(RouteTab.BUILTIN)
    val selectedTab: StateFlow<RouteTab> = _selectedTab

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting

    private val _submitResult = MutableStateFlow<Boolean?>(null)
    val submitResult: StateFlow<Boolean?> = _submitResult

    private val _selectedTags = MutableStateFlow<Set<EkdromeTag>>(emptySet())
    val selectedTags: StateFlow<Set<EkdromeTag>> = _selectedTags

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredRoutes: Flow<List<EkdromeRoute>> = _selectedTab.flatMapLatest { tab ->
        when (tab) {
            RouteTab.BUILTIN -> combine(_selectedRegion, _selectedTags) { region, tags ->
                builtinRouteRepository.routes.filter { route ->
                    (region == EkdromeRegion.ALL || route.region == region) &&
                            (tags.isEmpty() || tags.all { it in route.tags })
                }
            }
            RouteTab.COMMUNITY -> combine(
                customRouteRepository.getRoutes(),
                _selectedRegion,
                _selectedTags
            ) { routes, region, tags ->
                routes.filter { route ->
                    (region == EkdromeRegion.ALL || route.region == region) &&
                            (tags.isEmpty() || tags.all { it in route.tags })
                }
            }
            RouteTab.SAVED -> combine(
                savedRouteRepository.getAllSavedRoutes().map { entities ->
                    entities.map { SavedRouteRepository.toModel(it) }
                },
                _selectedRegion,
                _selectedTags
            ) { routes, region, tags ->
                routes.filter { route ->
                    (region == EkdromeRegion.ALL || route.region == region) &&
                            (tags.isEmpty() || tags.all { it in route.tags })
                }
            }
        }
    }

    val savedRouteKeys: StateFlow<Set<String>> = savedRouteRepository.getAllSavedKeys()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    fun selectTab(tab: RouteTab) {
        _selectedTab.value = tab
    }

    fun selectRegion(region: EkdromeRegion) {
        _selectedRegion.value = region
    }

    fun selectTag(tag: EkdromeTag?) {
        if (tag == null) {
            _selectedTags.value = emptySet()
            return
        }
        _selectedTags.value = if (tag in _selectedTags.value) {
            _selectedTags.value - tag
        } else {
            _selectedTags.value + tag
        }
    }

    fun submitRoute(
        nameEn: String,
        nameEl: String,
        region: EkdromeRegion,
        tags: List<EkdromeTag>,
        difficulty: EkdromeDifficulty,
        distanceKm: Int,
        descriptionEn: String,
        descriptionEl: String,
        latitude: Double,
        longitude: Double,
        startLocation: String,
        endLocation: String,
        waypoints: List<String>
    ) {
        viewModelScope.launch {
            _submitting.value = true
            val success = runCatching {
                customRouteRepository.addRoute(
                    nameEn, nameEl, region, tags, difficulty,
                    distanceKm, descriptionEn, descriptionEl,
                    latitude, longitude, startLocation, endLocation, waypoints
                )
            }.isSuccess
            _submitting.value = false
            _submitResult.value = success
        }
    }

    fun getRouteReviewId(route: EkdromeRoute): String {
        return if (route.firestoreId.isNotEmpty()) route.firestoreId else "builtin_${route.id}"
    }

    fun observeReviews(routeId: String): Flow<List<RouteReview>> {
        return localRouteReviewRepository.observeReviews(routeId)
    }

    private val _reviewSubmitResult = MutableStateFlow<Boolean?>(null)
    val reviewSubmitResult: StateFlow<Boolean?> = _reviewSubmitResult

    fun submitReview(routeId: String, rating: Float, comment: String) {
        viewModelScope.launch {
            val success = runCatching {
                localRouteReviewRepository.addReview(routeId, rating, comment)
            }.isSuccess
            _reviewSubmitResult.value = success
        }
    }

    fun clearReviewSubmitResult() {
        _reviewSubmitResult.value = null
    }

    fun clearSubmitResult() {
        _submitResult.value = null
    }

    fun toggleSaveRoute(route: EkdromeRoute) {
        viewModelScope.launch {
            savedRouteRepository.toggleSave(route)
        }
    }

    fun routeKey(route: EkdromeRoute): String = savedRouteRepository.routeKey(route)

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun getConsumptionForVehicle(vehicleId: Long, vehicleType: VehicleType): Double {
        val avg = logRepository.getAverageFuelEconomy(vehicleId)
        if (avg != null && avg > 0) return avg
        return when (vehicleType) {
            VehicleType.CAR -> 7.5
            VehicleType.MOTORCYCLE -> 4.5
            VehicleType.ATV -> 10.0
        }
    }

    fun defaultConsumption(vehicleType: VehicleType): Double = when (vehicleType) {
        VehicleType.CAR -> 7.5
        VehicleType.MOTORCYCLE -> 4.5
        VehicleType.ATV -> 10.0
    }
}
