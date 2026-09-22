---
description: "Use when creating or modifying ViewModels, Repositories, or Fragments in GearDex. Covers StateFlow/flatMapLatest patterns, Hilt injection, and lifecycle-safe collection."
applyTo: "app/src/main/java/com/geardex/app/ui/**/*.kt"
---
# MVVM + Repository Pattern

## ViewModel

- Annotate with `@HiltViewModel` and inject via `@Inject constructor`.
- Expose state as `StateFlow`, never `LiveData`.
- Use `SharingStarted.WhileSubscribed(5000)` with `stateIn()`.
- Filter per-vehicle data with `flatMapLatest` on `selectedVehicleId`:

```kotlin
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logRepository: LogRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    val vehicles = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicleId = MutableStateFlow<Long>(-1L)

    val fuelLogs: StateFlow<List<FuelLog>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptyList()) else logRepository.getFuelLogs(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

- Perform mutations via `viewModelScope.launch { repository.insert(entity) }`.
- Do **not** hold references to Context, View, or Fragment inside a ViewModel.

## Repository

- Annotate with `@Singleton` and inject via `@Inject constructor`.
- Return `Flow<List<T>>` from DAO calls directly — do not collect inside the repository.
- Use `suspend` functions for insert/update/delete operations.

```kotlin
@Singleton
class LogRepository @Inject constructor(
    private val fuelLogDao: FuelLogDao
) {
    fun getFuelLogs(vehicleId: Long): Flow<List<FuelLog>> =
        fuelLogDao.getFuelLogsForVehicle(vehicleId)

    suspend fun addFuelLog(log: FuelLog): Long = fuelLogDao.insertFuelLog(log)
}
```

## Fragment

- Annotate with `@AndroidEntryPoint`.
- Obtain ViewModel via `by viewModels()` (or `by activityViewModels()` for shared VM).
- Collect all flows inside a single `repeatOnLifecycle(STARTED)` block:

```kotlin
@AndroidEntryPoint
class LogsFragment : Fragment() {

    private val viewModel: LogsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.vehicles.collect { updateSpinner(it) } }
                launch { viewModel.fuelLogs.collect { adapter.submitList(it) } }
            }
        }
    }
}
```

## Dependency Injection

When adding a new DAO/Repository:
1. Add `abstract fun newEntityDao(): NewEntityDao` to `GearDexDatabase`.
2. Add `@Provides fun provideNewEntityDao(db: GearDexDatabase) = db.newEntityDao()` to `DatabaseModule`.
3. Inject the new DAO/Repository via constructor injection — never field injection (`@Inject` on a field).
