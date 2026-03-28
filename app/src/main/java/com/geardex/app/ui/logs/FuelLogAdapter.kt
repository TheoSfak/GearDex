package com.geardex.app.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.databinding.ItemFuelLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FuelLogAdapter : ListAdapter<FuelLog, FuelLogAdapter.FuelLogViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class FuelLogViewHolder(private val binding: ItemFuelLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(log: FuelLog) {
            binding.tvFuelDate.text = dateFormat.format(Date(log.date))
            binding.tvFuelLiters.text = "${log.liters} L  •  ${log.odometer} km"
            binding.tvFuelEconomy.text = log.fuelEconomy?.let {
                binding.root.context.getString(R.string.log_fuel_economy_result, it)
            } ?: "—"
            binding.tvFuelCost.text = "€${log.cost}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuelLogViewHolder {
        val binding = ItemFuelLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FuelLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FuelLogViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<FuelLog>() {
        override fun areItemsTheSame(oldItem: FuelLog, newItem: FuelLog) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FuelLog, newItem: FuelLog) = oldItem == newItem
    }
}
