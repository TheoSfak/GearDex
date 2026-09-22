---
description: "Use when adding or modifying Room entities, DAOs, migrations, or the GearDexDatabase class. Covers entity annotations, DAO patterns, TypeConverters, and version bumping."
applyTo: "app/src/main/java/com/geardex/app/data/local/**/*.kt"
---
# Room Database

## Current State

- **DB version**: 7 (increment for every schema change)
- AutoMigration chain: 1→2→3→4→5→6→7
- Schema export: `app/schemas/com.geardex.app.data.local.GearDexDatabase/`
- `exportSchema = true` — schema JSON files must be committed

## Entity Rules

- One entity per file under `data.local.entity`.
- Use `@PrimaryKey(autoGenerate = true)` with `val id: Long = 0`.
- Add `@ForeignKey` with `onDelete = ForeignKey.CASCADE` for child entities.
- Always add `@Index` on foreign key columns to avoid performance warnings.
- Use `Long` (Unix epoch ms) for date/time columns, never `String`.

```kotlin
@Entity(
    tableName = "fuel_logs",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class FuelLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Long,           // Unix epoch ms
    val odometer: Int,
    val liters: Double,
    val cost: Double,
    val fuelEconomy: Double? = null,
    val notes: String = ""
)
```

## DAO Rules

- Return `Flow<List<T>>` for list queries (reactive, observed by ViewModel).
- Use `suspend` for single-result queries and all write operations.
- Use `@Insert(onConflict = OnConflictStrategy.REPLACE)` for upserts.
- Use `@Transaction` when a function performs multiple write operations.

```kotlin
@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getFuelLogsForVehicle(vehicleId: Long): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY odometer DESC LIMIT 1")
    suspend fun getLastFuelLog(vehicleId: Long): FuelLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(fuelLog: FuelLog): Long

    @Delete
    suspend fun deleteFuelLog(fuelLog: FuelLog)
}
```

## TypeConverters

- All enum TypeConverters live in the `Converters` inner class in `GearDexDatabase.kt`.
- Store enums as `TEXT` using `.name` / `enumClass.valueOf()`:

```kotlin
@TypeConverter fun fromVehicleType(value: VehicleType): String = value.name
@TypeConverter fun toVehicleType(value: String): VehicleType = VehicleType.valueOf(value)
```

## Bumping the DB Version

1. Increment `version` in `@Database` in `GearDexDatabase.kt`.
2. Add `AutoMigration(from = N, to = N+1)` to the `autoMigrations` list.
3. If the migration requires custom logic, create an `AutoMigrationSpec` class.
4. Run the project once to generate the new `N+1.json` schema file; commit it.
5. **Never** use `fallbackToDestructiveMigration` to skip a migration — it destroys user data.

> **Pitfall**: When writing `N.json` baseline files manually, omit the `junction` field from `foreignKeys` entries — Room's schema parser will reject unknown keys.
