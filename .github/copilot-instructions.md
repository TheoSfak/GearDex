# GearDex — Copilot Instructions

Android vehicle management app (Kotlin, MVVM). Bilingual: English + Greek (Ελληνικά).

## Build & Run

```powershell
# JAVA_HOME must be set before any Gradle command
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"

.\gradlew.bat assembleDebug --no-daemon     # build APK
.\gradlew.bat installDebug --no-daemon      # build + install on connected device/emulator
.\gradlew.bat clean assembleDebug           # clean build
```

- Debug APK gets `.debug` app ID suffix (`com.geardex.app.debug`)
- Release builds require `keystore.properties` (not checked in)
- Firebase optional — requires `firebase.properties` (not checked in); app works offline without it

## Architecture

**MVVM + Repository.** Single-Activity (`MainActivity`) with Navigation Component.

```
Fragment (ViewBinding)
  └─ ViewModel (@HiltViewModel, StateFlow)
       └─ Repository (@Singleton)
            ├─ DAO (Room Flow queries)
            └─ FirestoreSyncRepository (optional cloud)
```

- Fragments observe `StateFlow`/`Flow` via `lifecycleScope.launch { repeatOnLifecycle(STARTED) { … } }`
- Adapters extend `ListAdapter<T, VH>` with a `DiffCallback` inner object
- ViewModels use `flatMapLatest` on a `selectedVehicleId: MutableStateFlow<Long>(-1L)` pattern to filter per-vehicle data

## Package Structure

```
com.geardex.app
├── ui.{garage,logs,glovebox,ekdromes,settings}   # Fragments + ViewModels + Adapters
├── data.local.entity                              # Room entities
├── data.local.dao                                 # Room DAOs
├── data.repository                               # Business logic
├── data.remote                                   # Firebase sync
├── di                                            # Hilt modules (DatabaseModule)
└── notifications                                 # WorkManager + NotificationHelper
```

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Fragment | `{Feature}Fragment` | `GarageFragment` |
| ViewModel | `{Feature}ViewModel` | `LogsViewModel` |
| Adapter | `{Entity}Adapter` | `VehicleAdapter` |
| Entity | singular PascalCase | `FuelLog`, `Vehicle` |
| DAO | `{Entity}Dao` | `MaintenanceReminderDao` |
| Repository | `{Domain}Repository` | `ReminderRepository` |
| Layout | `fragment_`, `item_`, `activity_` prefix | `fragment_logs.xml` |
| View IDs | `snake_case` | `btn_save`, `tv_vehicle_name` |
| String keys | `domain_description` | `garage_add_vehicle`, `log_fuel_economy` |
| Enum members | `UPPER_SNAKE_CASE` | `VehicleType.MOTORCYCLE` |

## Tech Stack

| Layer | Library | Version |
|-------|---------|---------|
| Language | Kotlin | 2.1.20 |
| Build | AGP | 8.9.0 |
| UI | Material 3 | 1.12.0 |
| Navigation | Navigation Component + SafeArgs | 2.8.9 |
| DI | Hilt | 2.56 |
| Database | Room | 2.7.0 |
| Async | Coroutines + Flow | 1.10.1 |
| Background | WorkManager | 2.10.0 |
| Charts | Vico Views | 2.0.2 |
| Cloud | Firebase BOM | 33.7.0 |

Versions are centralized in `gradle/libs.versions.toml` — always update there first.

## Database

- Room DB version: **2** (AutoMigration 1→2 adds `maintenance_reminders` table)
- `exportSchema = true`; schema files live in `app/schemas/com.geardex.app.data.local.GearDexDatabase/`
- Enums stored as `TEXT` via `TypeConverter`s in `GearDexDatabase.kt` `Converters` class
- When bumping DB version: increment version in `GearDexDatabase.kt`, add `AutoMigration(from=N, to=N+1)`, and provide `N.json` baseline if it doesn't exist
- `fallbackToDestructiveMigration(dropAllTables = false)` preserves existing tables on unexpected migrations

## Strings / Localization

All user-visible text must have entries in **both**:
- `res/values/strings.xml` (English)
- `res/values-el/strings.xml` (Greek)

Never hardcode strings in layouts or code.

## Dependency Injection

`DatabaseModule.kt` provides all DAOs. When adding a new DAO:
1. Add `abstract fun newEntityDao(): NewEntityDao` to `GearDexDatabase`
2. Add `@Provides fun provideNewEntityDao(db: GearDexDatabase) = db.newEntityDao()` to `DatabaseModule`

## Navigation

- Nav graph: `res/navigation/nav_graph.xml`
- Navigate with Action IDs: `findNavController().navigate(R.id.action_source_to_dest)`
- Child fragments embedded in `FragmentContainerView` (e.g., Reminders + Analytics inside LogsFragment) are **not** in the nav graph — use `childFragmentManager`

## Key Pitfalls

- `JAVA_HOME` must point to JDK 17 before running Gradle — Gradle won't pick up the system JDK automatically on this machine
- `CartesianChartView` (Vico) requires a `CartesianChartModelProducer` set in code; charts are not data-driven from XML
- When writing `1.json` schema baselines manually, omit the `junction` field from `foreignKeys` entries — this version of Room's schema parser rejects unknown keys
- The `LogsFragment` tabs use child fragments, not the nav graph — visibility is controlled manually via `showTab(position)`
