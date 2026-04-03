package com.geardex.app.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.data.local.entity.MaintenanceReminder
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.databinding.ItemReminderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAdapter(
    private val onMarkDone: (MaintenanceReminder) -> Unit,
    private val onDelete: (MaintenanceReminder) -> Unit
) : ListAdapter<MaintenanceReminder, ReminderAdapter.ViewHolder>(DiffCallback) {

    var vehicleNames: Map<Long, String> = emptyMap()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    companion object DiffCallback : DiffUtil.ItemCallback<MaintenanceReminder>() {
        override fun areItemsTheSame(old: MaintenanceReminder, new: MaintenanceReminder) = old.id == new.id
        override fun areContentsTheSame(old: MaintenanceReminder, new: MaintenanceReminder) = old == new
    }

    inner class ViewHolder(private val binding: ItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MaintenanceReminder) {
            binding.tvReminderTitle.text = item.title
            binding.chipReminderType.text = if (item.type == ReminderType.KM_BASED) "KM" else "Date"

            binding.tvReminderTarget.text = when (item.type) {
                ReminderType.KM_BASED -> "Target: ${item.targetKm} km"
                ReminderType.DATE_BASED -> {
                    val date = item.targetDate?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "—"
                    "Due: $date"
                }
            }

            binding.tvReminderVehicle.text = vehicleNames[item.vehicleId] ?: ""
            binding.btnMarkDone.setOnClickListener { onMarkDone(item) }
            binding.btnDeleteReminder.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
