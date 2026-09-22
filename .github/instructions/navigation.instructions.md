---
description: "Use when adding navigation actions, destinations, or SafeArgs in GearDex. Covers nav_graph.xml conventions, action ID naming, child fragments, and deep links."
---
# Navigation Component

## Nav Graph (`res/navigation/nav_graph.xml`)

- All top-level destinations are fragments in the nav graph.
- Action IDs follow the pattern: `action_{source}_{to/from?}_{destination}` e.g. `action_logs_to_addFuel`.
- Navigate using Action IDs — never use destination IDs directly:

```kotlin
findNavController().navigate(R.id.action_logsFragment_to_addFuelFragment)
```

- Pass arguments via SafeArgs directions (generated from `<argument>` tags in the nav graph), not raw `Bundle`.

## Adding a New Destination

1. Add a `<fragment>` element to `nav_graph.xml` with a unique `android:id`.
2. Add `<action>` elements for each transition into/out of the destination.
3. Add `<argument>` elements for required navigation parameters (type-safe via SafeArgs).
4. Rebuild to regenerate the SafeArgs `Directions` and `Args` classes.

## Child Fragments (tabs within a screen)

Child fragments embedded inside a `FragmentContainerView` (e.g., the Reminders and Analytics tabs inside `LogsFragment`) are **not** registered in `nav_graph.xml`.

- Add/remove them via `childFragmentManager`:
  ```kotlin
  childFragmentManager.beginTransaction()
      .replace(R.id.container_tab, TargetFragment())
      .commit()
  ```
- Control visibility manually via `showTab(position)` — do **not** use Navigation Component for these.

## Back Stack

- Use `popBackStack()` or `navigateUp()` for back navigation; avoid `requireActivity().onBackPressed()`.
- If an action should clear the back stack (e.g. after login), use `popUpTo` + `inclusive = true` in the action definition.

## SafeArgs

- Access passed arguments in the destination fragment via the generated `Args` class:
  ```kotlin
  val args: AddFuelFragmentArgs by navArgs()
  val vehicleId = args.vehicleId
  ```
