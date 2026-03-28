package com.geardex.app.ui.garage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.databinding.ItemVehicleBinding

class VehicleAdapter(
    private val onClick: (Vehicle) -> Unit
) : ListAdapter<Vehicle, VehicleAdapter.VehicleViewHolder>(DiffCallback) {

    inner class VehicleViewHolder(private val binding: ItemVehicleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(vehicle: Vehicle) {
            binding.tvVehicleName.text = "${vehicle.make} ${vehicle.model} (${vehicle.year})"
            binding.tvVehiclePlate.text = vehicle.licensePlate
            binding.tvVehicleKm.text = "${vehicle.currentKm} km"
            binding.tvVehicleTypeBadge.text = when (vehicle.type) {
                VehicleType.CAR -> binding.root.context.getString(R.string.vehicle_type_car)
                VehicleType.MOTORCYCLE -> binding.root.context.getString(R.string.vehicle_type_motorcycle)
                VehicleType.ATV -> binding.root.context.getString(R.string.vehicle_type_atv)
            }
            val iconRes = when (vehicle.type) {
                VehicleType.CAR -> R.drawable.ic_car
                VehicleType.MOTORCYCLE -> R.drawable.ic_motorcycle
                VehicleType.ATV -> R.drawable.ic_atv
            }
            binding.ivVehicleTypeIcon.setImageResource(iconRes)
            binding.root.setOnClickListener { onClick(vehicle) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val binding = ItemVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Vehicle>() {
        override fun areItemsTheSame(oldItem: Vehicle, newItem: Vehicle) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Vehicle, newItem: Vehicle) = oldItem == newItem
    }
}
