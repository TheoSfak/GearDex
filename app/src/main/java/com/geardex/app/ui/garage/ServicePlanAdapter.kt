package com.geardex.app.ui.garage

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.repository.ServicePlanStatus
import com.geardex.app.data.repository.ServicePlanSummary
import com.geardex.app.databinding.ItemServicePlanBinding

class ServicePlanAdapter(
    private val onDone: (ServicePlanSummary) -> Unit,
    private val onDelete: (ServicePlanSummary) -> Unit
) : ListAdapter<ServicePlanSummary, ServicePlanAdapter.ServicePlanViewHolder>(DiffCallback) {

    inner class ServicePlanViewHolder(private val binding: ItemServicePlanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: ServicePlanSummary) {
            val ctx = binding.root.context
            val color = statusColor(summary.status)
            binding.tvPlanTitle.text = ctx.servicePlanTitle(summary.plan)
            binding.tvPlanInterval.text = buildList {
                summary.plan.intervalKm?.let { add(ctx.getString(R.string.service_plan_every_km, it)) }
                summary.plan.intervalMonths?.let { add(ctx.getString(R.string.service_plan_every_months, it)) }
            }.joinToString("  ·  ")
            binding.tvPlanRemaining.text = buildList {
                ctx.servicePlanDistanceText(summary.kmRemaining)?.let { add(it) }
                ctx.servicePlanDateText(summary.daysRemaining)?.let { add(it) }
            }.joinToString("  ·  ")
            binding.chipPlanStatus.text = ctx.servicePlanStatusLabel(summary.status)
            binding.chipPlanStatus.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, color))
            binding.progressPlan.progress = summary.progressPercent
            binding.progressPlan.progressTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, color))
            binding.btnPlanDone.setOnClickListener { onDone(summary) }
            binding.btnPlanDelete.setOnClickListener { onDelete(summary) }
        }

        private fun statusColor(status: ServicePlanStatus): Int = when (status) {
            ServicePlanStatus.OK -> R.color.color_success
            ServicePlanStatus.SOON -> R.color.color_warning
            ServicePlanStatus.DUE -> R.color.geardex_orange
            ServicePlanStatus.OVERDUE -> R.color.color_error
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicePlanViewHolder {
        val binding = ItemServicePlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServicePlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServicePlanViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<ServicePlanSummary>() {
        override fun areItemsTheSame(old: ServicePlanSummary, new: ServicePlanSummary): Boolean =
            old.plan.id == new.plan.id

        override fun areContentsTheSame(old: ServicePlanSummary, new: ServicePlanSummary): Boolean =
            old == new
    }
}
