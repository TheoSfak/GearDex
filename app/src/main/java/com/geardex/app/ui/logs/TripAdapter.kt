package com.geardex.app.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.Trip
import com.geardex.app.data.local.entity.TripPurpose
import com.geardex.app.databinding.ItemTripBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter(
    private val onDelete: (Trip) -> Unit
) : ListAdapter<Trip, TripAdapter.TripViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class TripViewHolder(private val binding: ItemTripBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(trip: Trip) {
            val ctx = binding.root.context
            binding.tvTripDate.text = dateFormat.format(Date(trip.date))
            binding.tvTripPurpose.text = purposeName(ctx, trip.purpose)
            binding.tvTripDistance.text = "${trip.distanceKm} km"
            binding.tvTripCost.text = if (trip.costEuro > 0) "€${"%.2f".format(trip.costEuro)}" else ""

            if (trip.notes.isNotEmpty()) {
                binding.tvTripNotes.text = trip.notes
                binding.tvTripNotes.visibility = View.VISIBLE
            } else {
                binding.tvTripNotes.visibility = View.GONE
            }

            binding.root.setOnLongClickListener {
                onDelete(trip)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(old: Trip, new: Trip) = old.id == new.id
        override fun areContentsTheSame(old: Trip, new: Trip) = old == new
    }
}

fun purposeName(ctx: android.content.Context, purpose: String): String = when (purpose) {
    TripPurpose.COMMUTE.name -> ctx.getString(R.string.trip_purpose_commute)
    TripPurpose.LEISURE.name -> ctx.getString(R.string.trip_purpose_leisure)
    TripPurpose.BUSINESS.name -> ctx.getString(R.string.trip_purpose_business)
    else -> ctx.getString(R.string.trip_purpose_other)
}
