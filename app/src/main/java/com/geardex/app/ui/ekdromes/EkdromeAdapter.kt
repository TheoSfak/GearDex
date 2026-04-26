package com.geardex.app.ui.ekdromes

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.databinding.ItemEkdromeBinding
import java.util.Locale

class EkdromeAdapter(
    private val onReviewClick: ((EkdromeRoute) -> Unit)? = null,
    private val onCalculateCostClick: ((EkdromeRoute) -> Unit)? = null,
    private val onItemClick: ((EkdromeRoute) -> Unit)? = null,
    private val onBookmarkClick: ((EkdromeRoute) -> Unit)? = null
) : ListAdapter<EkdromeRoute, EkdromeAdapter.EkdromeViewHolder>(DiffCallback) {

    private var savedKeys: Set<String> = emptySet()

    fun updateSavedKeys(keys: Set<String>) {
        savedKeys = keys
        notifyItemRangeChanged(0, itemCount)
    }

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

            // Bookmark icon
            val key = if (route.firestoreId.isNotBlank()) route.firestoreId else "builtin_${route.id}"
            val isSaved = savedKeys.contains(key)
            binding.btnBookmark.setImageResource(
                if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
            )
            binding.btnBookmark.setOnClickListener { onBookmarkClick?.invoke(route) }

            // Card click → detail
            binding.root.setOnClickListener { onItemClick?.invoke(route) }

            // Route info (start → end)
            if (route.startLocation.isNotEmpty() && route.endLocation.isNotEmpty()) {
                val waypointText = if (route.waypoints.isNotEmpty()) {
                    " → ${route.waypoints.joinToString(" → ")}"
                } else ""
                binding.tvEkdromeRouteInfo.text = "${route.startLocation}$waypointText → ${route.endLocation}"
                binding.tvEkdromeRouteInfo.visibility = View.VISIBLE
            } else {
                binding.tvEkdromeRouteInfo.visibility = View.GONE
            }

            // View on Map
            binding.btnViewOnMap.setOnClickListener {
                val label = if (greek) route.nameEl else route.nameEn
                val geoUri = "geo:${route.latitude},${route.longitude}?q=${route.latitude},${route.longitude}(${Uri.encode(label)})".toUri()
                val intent = Intent(Intent.ACTION_VIEW, geoUri)
                runCatching {
                    it.context.startActivity(Intent.createChooser(intent, it.context.getString(R.string.ekdrome_view_on_map)))
                }.onFailure {
                    android.widget.Toast.makeText(
                        binding.root.context,
                        R.string.ekdrome_no_map_app,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Navigate button
            if (route.startLocation.isNotEmpty() && route.endLocation.isNotEmpty()) {
                binding.btnNavigate.visibility = View.VISIBLE
                binding.btnNavigate.setOnClickListener {
                    val allPoints = mutableListOf(route.startLocation)
                    allPoints.addAll(route.waypoints)
                    allPoints.add(route.endLocation)
                    val path = allPoints.joinToString("/") { Uri.encode(it) }
                    val mapsUrl = "https://www.google.com/maps/dir/$path"
                    val intent = Intent(Intent.ACTION_VIEW, mapsUrl.toUri())
                    runCatching {
                        it.context.startActivity(Intent.createChooser(intent, it.context.getString(R.string.ekdrome_navigate)))
                    }.onFailure {
                        android.widget.Toast.makeText(
                            binding.root.context,
                            R.string.ekdrome_no_map_app,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                binding.btnNavigate.visibility = View.GONE
            }

            // Reviews pill
            binding.tvReviewCount.text = if (route.reviewCount > 0) {
                "${route.reviewCount}"
            } else {
                if (greek) "Αξιολόγηση" else "Rate"
            }
            binding.layoutReviewsPill.setOnClickListener {
                onReviewClick?.invoke(route)
            }

            // Calculate Cost
            binding.btnCalculateCost.setOnClickListener {
                onCalculateCostClick?.invoke(route)
            }
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
