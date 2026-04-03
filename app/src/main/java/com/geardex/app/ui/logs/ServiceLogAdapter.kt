package com.geardex.app.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.databinding.ItemServiceLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServiceLogAdapter : ListAdapter<ServiceLog, ServiceLogAdapter.ServiceLogViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class ServiceLogViewHolder(private val binding: ItemServiceLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: ServiceLog) {
            binding.tvServiceDate.text = dateFormat.format(Date(log.date))
            binding.tvServiceKm.text = "${log.odometer} km"
            binding.tvServiceCost.text = "€${log.cost}"
            binding.tvServiceMechanic.text = log.mechanicName.ifEmpty { "—" }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceLogViewHolder {
        val binding = ItemServiceLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServiceLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceLogViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<ServiceLog>() {
        override fun areItemsTheSame(o: ServiceLog, n: ServiceLog) = o.id == n.id
        override fun areContentsTheSame(o: ServiceLog, n: ServiceLog) = o == n
    }
}
