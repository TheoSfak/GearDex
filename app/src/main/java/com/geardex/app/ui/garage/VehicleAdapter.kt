package com.geardex.app.ui.garage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
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

    var scores: Map<Long, Int> = emptyMap()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

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

            // Health score badge
            val score = scores[vehicle.id] ?: 100
            binding.tvHealthScore.text = score.toString()
            val color = scoreColor(score)
            val badgeBg = binding.root.context.getDrawable(R.drawable.bg_score_badge)!!.mutate()
            DrawableCompat.setTint(badgeBg, color)
            binding.tvHealthScore.background = badgeBg
            binding.viewScoreStrip.setBackgroundColor(color)

            binding.root.setOnClickListener { onClick(vehicle) }
        }

        private fun scoreColor(score: Int): Int {
            val ctx = binding.root.context
            return when {
                score >= 80 -> ctx.getColor(R.color.score_good)
                score >= 50 -> ctx.getColor(R.color.score_fair)
                else        -> ctx.getColor(R.color.score_poor)
            }
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
