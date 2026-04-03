package com.geardex.app.ui.garage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.databinding.ItemTimelineEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimelineAdapter : ListAdapter<TimelineEvent, TimelineAdapter.TimelineViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class TimelineViewHolder(private val binding: ItemTimelineEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: TimelineEvent) {
            val ctx = binding.root.context
            binding.tvTimelineDate.text = dateFormat.format(Date(event.timestamp))

            when (event) {
                is TimelineEvent.Fuel -> {
                    tintDot(R.color.accent_fuel)
                    binding.ivTimelineIcon.setImageResource(R.drawable.ic_fuel)
                    binding.ivTimelineIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_fuel))
                    binding.tvTimelineTitle.text = ctx.getString(R.string.dashboard_event_fuel)
                    binding.tvTimelineSubtitle.text = "${event.log.liters} L  ·  ${event.log.odometer} km"
                    binding.tvTimelineValue.text = "€${"%.2f".format(event.log.cost)}"
                }
                is TimelineEvent.Service -> {
                    tintDot(R.color.accent_service)
                    binding.ivTimelineIcon.setImageResource(R.drawable.ic_mechanic)
                    binding.ivTimelineIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_service))
                    binding.tvTimelineTitle.text = ctx.getString(R.string.dashboard_event_service)
                    val parts = buildList {
                        if (event.log.oilChange) add(ctx.getString(R.string.service_oil_change))
                        if (event.log.airFilter) add(ctx.getString(R.string.service_air_filter))
                        if (event.log.brakePads) add(ctx.getString(R.string.service_brake_pads))
                        if (event.log.timingBelt) add(ctx.getString(R.string.service_timing_belt))
                        if (event.log.cabinFilter) add(ctx.getString(R.string.service_cabin_filter))
                        if (event.log.chainLube) add(ctx.getString(R.string.service_chain_lube))
                        if (event.log.valveClearance) add(ctx.getString(R.string.service_valve_clearance))
                        if (event.log.forkOil) add(ctx.getString(R.string.service_fork_oil))
                        if (event.log.tireCheck) add(ctx.getString(R.string.service_tire_check))
                    }
                    binding.tvTimelineSubtitle.text = if (parts.isNotEmpty()) parts.joinToString(", ") else event.log.mechanicName.ifEmpty { "${event.log.odometer} km" }
                    binding.tvTimelineValue.text = "€${"%.2f".format(event.log.cost)}"
                }
                is TimelineEvent.Reminder -> {
                    tintDot(R.color.accent_reminder)
                    binding.ivTimelineIcon.setImageResource(R.drawable.ic_reminder_bell)
                    binding.ivTimelineIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_reminder))
                    binding.tvTimelineTitle.text = event.reminder.title
                    binding.tvTimelineSubtitle.text = when (event.reminder.type) {
                        ReminderType.KM_BASED -> "${ctx.getString(R.string.reminder_target_km)}: ${event.reminder.targetKm} km"
                        ReminderType.DATE_BASED -> event.reminder.targetDate?.let {
                            "${ctx.getString(R.string.reminder_pick_date)}: ${dateFormat.format(Date(it))}"
                        } ?: ""
                    }
                    binding.tvTimelineValue.text = if (event.reminder.isDone) "✓" else ""
                }
                is TimelineEvent.Document -> {
                    tintDot(R.color.accent_glovebox)
                    binding.ivTimelineIcon.setImageResource(R.drawable.ic_document)
                    binding.ivTimelineIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_glovebox))
                    binding.tvTimelineTitle.text = when (event.doc.documentType) {
                        DocumentType.KTEO -> ctx.getString(R.string.doc_type_kteo)
                        DocumentType.INSURANCE -> ctx.getString(R.string.doc_type_insurance)
                        DocumentType.ROAD_TAX -> ctx.getString(R.string.doc_type_road_tax)
                        DocumentType.RECEIPT -> ctx.getString(R.string.doc_type_receipt)
                        DocumentType.OTHER -> ctx.getString(R.string.doc_type_other)
                    }
                    binding.tvTimelineSubtitle.text = event.doc.fileName
                    binding.tvTimelineValue.text = event.doc.expiryDate?.let {
                        dateFormat.format(Date(it))
                    } ?: ""
                }
            }
        }

        private fun tintDot(colorRes: Int) {
            val bg = binding.viewTimelineDot.background?.mutate() ?: return
            DrawableCompat.setTint(bg, ContextCompat.getColor(binding.root.context, colorRes))
            binding.viewTimelineDot.background = bg
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val binding = ItemTimelineEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<TimelineEvent>() {
        override fun areItemsTheSame(old: TimelineEvent, new: TimelineEvent): Boolean {
            return old.eventType == new.eventType && old.timestamp == new.timestamp
        }

        override fun areContentsTheSame(old: TimelineEvent, new: TimelineEvent): Boolean = old == new
    }
}
