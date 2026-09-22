---
description: "Use when building UI in GearDex: ViewBinding setup in Fragments, ListAdapter with DiffCallback, Material 3 theming, and RecyclerView adapter patterns."
applyTo: "app/src/main/java/com/geardex/app/ui/**/*.kt"
---
# UI — Material 3 & ViewBinding

## ViewBinding in Fragments

Use the standard inflate + nullify pattern to avoid memory leaks:

```kotlin
class LogsFragment : Fragment(R.layout.fragment_logs) {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null   // prevent memory leaks
    }
}
```

- Never access `binding` after `onDestroyView()`.
- Do **not** use `findViewById` — always use generated binding properties.

## RecyclerView Adapters

Extend `ListAdapter<T, VH>` with a companion `DiffCallback`:

```kotlin
class FuelLogAdapter(
    private val onDelete: (FuelLog) -> Unit
) : ListAdapter<FuelLog, FuelLogAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemFuelLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FuelLog) {
            binding.tvDate.text = formatDate(item.date)
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemFuelLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<FuelLog>() {
        override fun areItemsTheSame(old: FuelLog, new: FuelLog) = old.id == new.id
        override fun areContentsTheSame(old: FuelLog, new: FuelLog) = old == new
    }
}
```

- Always use `submitList()` — never mutate the backing list directly.
- Pass callbacks (lambdas) for item actions; do not hold ViewModel references in adapters.

## Material 3 Theming

- Use Material 3 color roles (`colorPrimary`, `colorOnSurface`, `colorSurfaceVariant`, etc.) from the theme — never hardcode color values.
- Use `MaterialAlertDialogBuilder` for dialogs, not `AlertDialog.Builder`.
- Use `TextInputLayout` + `TextInputEditText` for all form fields.
- Prefer `MaterialCardView` over plain `CardView`.
- Use `MaterialButton` (not `Button` or `AppCompatButton`) for all buttons.

## Vico Charts

- `CartesianChartView` requires a `CartesianChartModelProducer` set **in code** — charts cannot be data-driven from XML alone.
- Always call `modelProducer.tryRunTransaction { /* update series */ }` from a coroutine.

## Layout Conventions

- Layout file names: `fragment_*.xml`, `item_*.xml`, `activity_*.xml`.
- View IDs: `snake_case` — `btn_save`, `tv_vehicle_name`, `rv_fuel_logs`.
- No hardcoded dimensions in dp/sp except via `@dimen` resources.
- No hardcoded strings in XML — always reference `@string/key`.
