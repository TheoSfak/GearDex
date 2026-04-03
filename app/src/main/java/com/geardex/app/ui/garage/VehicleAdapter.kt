package com.geardex.app.ui.garage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.databinding.ItemVehicleBinding
import java.io.File

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
            val ctx = binding.root.context
            binding.tvVehicleName.text = "${vehicle.make} ${vehicle.model} (${vehicle.year})"
            binding.tvVehiclePlate.text = vehicle.licensePlate
            binding.tvVehicleKm.text = "${vehicle.currentKm} km"
            binding.tvVehicleTypeBadge.text = when (vehicle.type) {
                VehicleType.CAR -> ctx.getString(R.string.vehicle_type_car)
                VehicleType.MOTORCYCLE -> ctx.getString(R.string.vehicle_type_motorcycle)
                VehicleType.ATV -> ctx.getString(R.string.vehicle_type_atv)
            }

            // Per-type icon + placeholder gradient
            val (iconRes, placeholderBgRes) = when (vehicle.type) {
                VehicleType.CAR -> Pair(R.drawable.ic_car, R.drawable.bg_vehicle_placeholder)
                VehicleType.MOTORCYCLE -> Pair(R.drawable.ic_motorcycle, R.drawable.bg_vehicle_placeholder_moto)
                VehicleType.ATV -> Pair(R.drawable.ic_atv, R.drawable.bg_vehicle_placeholder_atv)
            }
            binding.ivPlaceholderIcon.setImageResource(iconRes)
            binding.viewPlaceholderBg.setBackgroundResource(placeholderBgRes)

            // Vehicle hero image — photo sits on top of placeholder
            val imagePath = vehicle.imagePath
            if (imagePath != null && File(imagePath).exists()) {
                binding.ivVehicleImage.visibility = View.VISIBLE
                binding.ivVehicleImage.setImageURI(android.net.Uri.fromFile(File(imagePath)))
                binding.ivPlaceholderIcon.visibility = View.GONE
            } else {
                binding.ivVehicleImage.visibility = View.GONE
                binding.ivPlaceholderIcon.visibility = View.VISIBLE
            }

            // Health score badge with colored circle
            val score = scores[vehicle.id] ?: 100
            binding.tvHealthScore.text = score.toString()
            val color = scoreColor(score)
            val badgeBg = ctx.getDrawable(R.drawable.bg_score_circle)!!.mutate()
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
