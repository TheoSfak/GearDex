package com.geardex.app.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServiceLogAdapter : ListAdapter<ServiceLog, ServiceLogAdapter.ServiceLogViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class ServiceLogViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tv_service_date)
        val tvKm: TextView = itemView.findViewById(R.id.tv_service_km)
        val tvCost: TextView = itemView.findViewById(R.id.tv_service_cost)
        val tvMechanic: TextView = itemView.findViewById(R.id.tv_service_mechanic)

        fun bind(log: ServiceLog) {
            tvDate.text = dateFormat.format(Date(log.date))
            tvKm.text = "${log.odometer} km"
            tvCost.text = "€${log.cost}"
            tvMechanic.text = log.mechanicName.ifEmpty { "—" }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceLogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service_log, parent, false)
        return ServiceLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceLogViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<ServiceLog>() {
        override fun areItemsTheSame(o: ServiceLog, n: ServiceLog) = o.id == n.id
        override fun areContentsTheSame(o: ServiceLog, n: ServiceLog) = o == n
    }
}
