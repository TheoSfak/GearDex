package com.geardex.app.ui.ekdromes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.databinding.ItemEkdromeBinding
import java.util.Locale

class EkdromeAdapter : ListAdapter<EkdromeRoute, EkdromeAdapter.EkdromeViewHolder>(DiffCallback) {

    private val isGreek: Boolean
        get() = Locale.getDefault().language == "el"

    inner class EkdromeViewHolder(private val binding: ItemEkdromeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(route: EkdromeRoute) {
            val greek = isGreek
            binding.tvEkdromeName.text = if (greek) route.nameEl else route.nameEn
            binding.tvEkdromeRegion.text = if (greek) route.region.displayEl else route.region.displayEn
            binding.tvEkdromeDistance.text = "${route.distanceKm} km"
            binding.tvEkdromeDifficulty.text = if (greek) route.difficulty.displayEl else route.difficulty.displayEn
            binding.tvEkdromeRating.text = "★ ${"%.1f".format(route.rating)}"
            binding.tvEkdromeTags.text = route.tags.joinToString(" · ") {
                if (greek) it.displayEl else it.displayEn
            }
            binding.tvEkdromeDescription.text = if (greek) route.descriptionEl else route.descriptionEn
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EkdromeViewHolder {
        val binding = ItemEkdromeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EkdromeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EkdromeViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<EkdromeRoute>() {
        override fun areItemsTheSame(o: EkdromeRoute, n: EkdromeRoute) = o.id == n.id
        override fun areContentsTheSame(o: EkdromeRoute, n: EkdromeRoute) = o == n
    }
}
