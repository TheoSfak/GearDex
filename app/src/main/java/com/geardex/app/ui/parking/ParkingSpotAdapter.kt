package com.geardex.app.ui.parking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.ParkingSpot
import com.geardex.app.databinding.ItemParkingSpotBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParkingSpotAdapter(
    private val onOpenMap: (ParkingSpot) -> Unit,
    private val onShare: (ParkingSpot) -> Unit,
    private val onDelete: (ParkingSpot) -> Unit
) : ListAdapter<ParkingSpot, ParkingSpotAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ParkingSpot>() {
            override fun areItemsTheSame(old: ParkingSpot, new: ParkingSpot) = old.id == new.id
            override fun areContentsTheSame(old: ParkingSpot, new: ParkingSpot) = old == new
        }
    }

    inner class VH(private val b: ItemParkingSpotBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(spot: ParkingSpot) {
            val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            b.tvParkingDate.text = dateFmt.format(Date(spot.savedAt))
            b.tvParkingAddress.text =
                spot.address.ifBlank { b.root.context.getString(R.string.parking_no_address) }
            b.tvParkingNotes.text = spot.notes
            b.tvParkingNotes.visibility = if (spot.notes.isBlank()) View.GONE else View.VISIBLE

            val hasMeter = spot.meterExpiryMs > 0
            b.tvMeterExpiry.visibility = if (hasMeter) View.VISIBLE else View.GONE
            if (hasMeter) {
                val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                b.tvMeterExpiry.text = b.root.context.getString(
                    R.string.parking_meter_until,
                    timeFmt.format(Date(spot.meterExpiryMs))
                )
            }

            b.btnOpenMap.setOnClickListener { onOpenMap(spot) }
            b.btnShare.setOnClickListener { onShare(spot) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemParkingSpotBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
