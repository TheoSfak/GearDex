package com.geardex.app.ui.marketplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.model.MarketplacePart
import com.geardex.app.data.model.PartCondition
import com.geardex.app.databinding.ItemMarketplacePartBinding

class MarketplacePartAdapter(
    private val onClick: (MarketplacePart) -> Unit
) : ListAdapter<MarketplacePart, MarketplacePartAdapter.PartViewHolder>(DiffCallback) {

    inner class PartViewHolder(private val binding: ItemMarketplacePartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(part: MarketplacePart) {
            val ctx = binding.root.context
            binding.tvPartName.text = part.partName
            binding.tvPartPrice.text = "€${"%.2f".format(part.price)}"
            binding.tvPartCondition.text = conditionName(ctx, part.condition)
            binding.tvPartVehicle.text = if (part.vehicleMake.isNotEmpty())
                "${part.vehicleMake} ${part.vehicleModel} ${if (part.vehicleYear > 0) part.vehicleYear else ""}"
            else ""
            binding.tvPartSeller.text = part.sellerName.ifEmpty { "anonymous" }
            binding.tvPartRegion.text = part.region.ifEmpty { "—" }

            binding.root.setOnClickListener { onClick(part) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val binding = ItemMarketplacePartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<MarketplacePart>() {
        override fun areItemsTheSame(old: MarketplacePart, new: MarketplacePart) = old.id == new.id
        override fun areContentsTheSame(old: MarketplacePart, new: MarketplacePart) = old == new
    }
}

fun conditionName(ctx: android.content.Context, condition: PartCondition): String = when (condition) {
    PartCondition.NEW -> ctx.getString(R.string.marketplace_condition_new)
    PartCondition.USED_LIKE_NEW -> ctx.getString(R.string.marketplace_condition_like_new)
    PartCondition.USED_GOOD -> ctx.getString(R.string.marketplace_condition_good)
    PartCondition.USED_FAIR -> ctx.getString(R.string.marketplace_condition_fair)
    PartCondition.FOR_PARTS -> ctx.getString(R.string.marketplace_condition_parts)
}
