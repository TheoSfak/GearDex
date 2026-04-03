package com.geardex.app.ui.garage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.databinding.ItemFleetVehicleBinding

data class FleetVehicleItem(
    val vehicleId: Long,
    val name: String,
    val totalKm: Int,
    val totalCost: Double,
    val costPerKm: Double,
    val rank: Int
)

class FleetVehicleAdapter : ListAdapter<FleetVehicleItem, FleetVehicleAdapter.FleetViewHolder>(DiffCallback) {

    inner class FleetViewHolder(private val binding: ItemFleetVehicleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FleetVehicleItem) {
            binding.tvRank.text = item.rank.toString()
            binding.tvVehicleName.text = item.name
            binding.tvVehicleKm.text = "%,d km".format(item.totalKm)
            binding.tvCostPerKm.text = "€${"%.2f".format(item.costPerKm)}/km"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FleetViewHolder {
        val binding = ItemFleetVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FleetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FleetViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<FleetVehicleItem>() {
        override fun areItemsTheSame(old: FleetVehicleItem, new: FleetVehicleItem) = old.vehicleId == new.vehicleId
        override fun areContentsTheSame(old: FleetVehicleItem, new: FleetVehicleItem) = old == new
    }
}
