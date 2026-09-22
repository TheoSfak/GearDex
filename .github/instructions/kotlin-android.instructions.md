---
description: "Use when writing Kotlin code for GearDex Android: naming conventions, language idioms, coroutine patterns, enums, and data classes."
applyTo: "**/*.kt"
---
# Kotlin & Android Style

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Fragment | `{Feature}Fragment` | `EkdromesFragment` |
| ViewModel | `{Feature}ViewModel` | `LogsViewModel` |
| Adapter | `{Entity}Adapter` | `FuelLogAdapter` |
| Entity | singular PascalCase | `FuelLog`, `ServiceLog` |
| DAO | `{Entity}Dao` | `FuelLogDao` |
| Repository | `{Domain}Repository` | `LogRepository` |
| Layout files | `fragment_`, `item_`, `activity_` prefix | `fragment_logs.xml` |
| View IDs | `snake_case` | `btn_save`, `tv_vehicle_name` |
| String keys | `domain_description` | `garage_add_vehicle`, `log_fuel_economy` |
| Enum members | `UPPER_SNAKE_CASE` | `VehicleType.MOTORCYCLE`, `ExpenseCategory.FUEL` |

## Kotlin Idioms

- Prefer `when` over `if-else` chains for multi-branch logic:
  ```kotlin
  when (item.itemId) {
      R.id.action_edit -> editItem()
      R.id.action_delete -> deleteItem()
      else -> return false
  }
  ```
- Use **data classes** for entities and DTOs; always include `val id: Long = 0` with default.
- Use **default parameter values** in constructors; avoid overloaded constructors.
- Use `?.let { }` for nullable chaining instead of null checks.
- Avoid `!!` (non-null assertion); handle nullability explicitly.

## Coroutines

- Launch coroutines from `viewModelScope` in ViewModels.
- Use `suspend` for one-shot database operations; `Flow` for reactive streams.
- Collect flows in Fragments inside `repeatOnLifecycle(STARTED)`:
  ```kotlin
  viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
          launch { viewModel.someFlow.collect { /* update UI */ } }
      }
  }
  ```
- Never collect flows in `onCreate` or outside lifecycle-aware scopes.

## Enums

- Store enums as `TEXT` in Room via `TypeConverter` (`.name` / `valueOf()`).
- Declare enum TypeConverters in `GearDexDatabase.Converters`.

## General

- Package names: `com.geardex.app.ui.{feature}`, `com.geardex.app.data.local.entity`, etc.
- Never hardcode user-visible strings — use `strings.xml` keys only.
- All user-visible text must exist in **both** `res/values/strings.xml` (EN) and `res/values-el/strings.xml` (EL).
