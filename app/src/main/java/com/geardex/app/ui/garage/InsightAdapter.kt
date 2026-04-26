package com.geardex.app.ui.garage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.ExpenseCategory
import com.geardex.app.databinding.ItemInsightCardBinding

class InsightAdapter : ListAdapter<DashboardInsight, InsightAdapter.InsightViewHolder>(DiffCallback) {

    inner class InsightViewHolder(private val binding: ItemInsightCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(insight: DashboardInsight) {
            val ctx = binding.root.context

            // Severity bar color
            val severityColor = when (insight.severity) {
                InsightSeverity.HIGH -> R.color.color_error
                InsightSeverity.MEDIUM -> R.color.color_warning
                InsightSeverity.LOW -> R.color.accent_ekdromes
            }
            binding.viewSeverityBar.setBackgroundColor(ContextCompat.getColor(ctx, severityColor))

            when (insight) {
                is DashboardInsight.FuelEconomyDrop -> {
                    binding.ivInsightIcon.setImageResource(R.drawable.ic_fuel)
                    binding.ivInsightIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_fuel))
                    binding.tvInsightTitle.text = ctx.getString(R.string.insight_fuel_drop_title)
                    binding.tvInsightDescription.text = ctx.getString(
                        R.string.insight_fuel_drop_desc, insight.percentDrop, insight.recentAvg
                    )
                }
                is DashboardInsight.ServiceDueSoon -> {
                    binding.ivInsightIcon.setImageResource(R.drawable.ic_mechanic)
                    binding.ivInsightIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_service))
                    binding.tvInsightTitle.text = if (insight.estimatedDaysLeft <= 0)
                        ctx.getString(R.string.insight_service_overdue_title)
                    else
                        ctx.getString(R.string.insight_service_due_title)
                    binding.tvInsightDescription.text = ctx.resources.getQuantityString(
                        R.plurals.insight_service_due_desc,
                        insight.estimatedDaysLeft,
                        insight.kmSinceLastService,
                        insight.estimatedDaysLeft
                    )
                }
                is DashboardInsight.DocumentExpiring -> {
                    binding.ivInsightIcon.setImageResource(R.drawable.ic_document)
                    binding.ivInsightIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_glovebox))
                    val docName = when (insight.documentType) {
                        DocumentType.KTEO -> ctx.getString(R.string.doc_type_kteo)
                        DocumentType.INSURANCE -> ctx.getString(R.string.doc_type_insurance)
                        DocumentType.ROAD_TAX -> ctx.getString(R.string.doc_type_road_tax)
                        DocumentType.RECEIPT -> ctx.getString(R.string.doc_type_receipt)
                        DocumentType.OTHER -> ctx.getString(R.string.doc_type_other)
                    }
                    binding.tvInsightTitle.text = if (insight.daysLeft <= 0)
                        ctx.getString(R.string.insight_doc_expired_title, docName)
                    else
                        ctx.getString(R.string.insight_doc_expiring_title, docName)
                    binding.tvInsightDescription.text = if (insight.daysLeft <= 0) {
                        val daysExpired = -insight.daysLeft
                        ctx.resources.getQuantityString(
                            R.plurals.insight_doc_expired_desc,
                            daysExpired,
                            daysExpired
                        )
                    } else {
                        ctx.resources.getQuantityString(
                            R.plurals.insight_doc_expiring_desc,
                            insight.daysLeft,
                            insight.daysLeft
                        )
                    }
                }
                is DashboardInsight.HighMonthlySpend -> {
                    binding.ivInsightIcon.setImageResource(R.drawable.ic_money)
                    binding.ivInsightIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.color_warning))
                    binding.tvInsightTitle.text = ctx.getString(R.string.insight_high_spend_title)
                    binding.tvInsightDescription.text = ctx.getString(
                        R.string.insight_high_spend_desc, insight.currentMonthCost, insight.avgMonthCost
                    )
                }
                is DashboardInsight.MileageMilestone -> {
                    binding.ivInsightIcon.setImageResource(R.drawable.ic_odometer)
                    binding.ivInsightIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_ekdromes))
                    binding.tvInsightTitle.text = ctx.getString(R.string.insight_milestone_title)
                    binding.tvInsightDescription.text = ctx.getString(
                        R.string.insight_milestone_desc, insight.nextMilestone, insight.nextMilestone - insight.currentKm
                    )
                }
                is DashboardInsight.ExpenseCategorySpike -> {
                    binding.ivInsightIcon.setImageResource(R.drawable.ic_expense)
                    binding.ivInsightIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_expense))
                    val catName = com.geardex.app.ui.logs.categoryName(ctx, insight.category)
                    binding.tvInsightTitle.text = ctx.getString(R.string.insight_expense_spike_title, catName)
                    binding.tvInsightDescription.text = ctx.getString(
                        R.string.insight_expense_spike_desc, insight.recentAmount, insight.avgAmount
                    )
                }
                is DashboardInsight.PredictedCostAlert -> {
                    binding.ivInsightIcon.setImageResource(R.drawable.ic_wallet)
                    binding.ivInsightIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_expense))
                    binding.tvInsightTitle.text = ctx.getString(R.string.insight_predicted_cost_title)
                    binding.tvInsightDescription.text = ctx.getString(
                        R.string.insight_predicted_cost_desc, insight.predictedMonthly, insight.historicalAvg
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsightViewHolder {
        val binding = ItemInsightCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InsightViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InsightViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<DashboardInsight>() {
        override fun areItemsTheSame(old: DashboardInsight, new: DashboardInsight) = old == new
        override fun areContentsTheSame(old: DashboardInsight, new: DashboardInsight) = old == new
    }
}
