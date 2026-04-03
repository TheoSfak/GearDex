package com.geardex.app.ui.garage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.data.local.entity.DriveSession
import com.geardex.app.databinding.ItemDriveSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DriveSessionAdapter : ListAdapter<DriveSession, DriveSessionAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDriveSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemDriveSessionBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        fun bind(session: DriveSession) {
            binding.tvSessionDate.text = dateFmt.format(Date(session.startTime))

            val minutes = session.durationMillis / 60_000
            val hours = minutes / 60
            val mins = minutes % 60
            binding.tvSessionDuration.text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            binding.tvSessionDistance.text = "%.1f km".format(session.distanceKm)
            binding.tvSessionAvgSpeed.text = "%.0f km/h avg".format(session.avgSpeedKmh)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<DriveSession>() {
        override fun areItemsTheSame(a: DriveSession, b: DriveSession) = a.id == b.id
        override fun areContentsTheSame(a: DriveSession, b: DriveSession) = a == b
    }
}
