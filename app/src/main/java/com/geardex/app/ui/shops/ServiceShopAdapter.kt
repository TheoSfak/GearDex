package com.geardex.app.ui.shops

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.model.ServiceShop
import com.geardex.app.data.model.ShopCategory
import com.geardex.app.databinding.ItemServiceShopBinding
import java.util.Locale

class ServiceShopAdapter(
    private val onFavoriteClick: (ServiceShop) -> Unit = {}
) : ListAdapter<ServiceShop, ServiceShopAdapter.ViewHolder>(DiffCallback) {

    var favoriteIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceShopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemServiceShopBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(shop: ServiceShop) {
            val isGreek = Locale.getDefault().language == "el"
            binding.tvShopName.text = if (isGreek && shop.nameEl.isNotBlank()) shop.nameEl else shop.nameEn
            binding.tvShopCategory.text = shopCategoryName(shop.category)
            binding.tvShopRegion.text = shop.region
            binding.tvShopRating.text = "%.1f".format(shop.rating)
            binding.tvShopReviews.text = "${shop.reviewCount} reviews"

            val isFav = favoriteIds.contains(shop.id)
            binding.ivFavorite.setImageResource(
                if (isFav) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
            )
            binding.ivFavorite.setOnClickListener { onFavoriteClick(shop) }
        }

        private fun shopCategoryName(cat: ShopCategory): String = when (cat) {
            ShopCategory.GENERAL -> itemView.context.getString(R.string.shop_cat_general)
            ShopCategory.ENGINE_SPECIALIST -> itemView.context.getString(R.string.shop_cat_engine)
            ShopCategory.BODY_SHOP -> itemView.context.getString(R.string.shop_cat_body)
            ShopCategory.TIRES -> itemView.context.getString(R.string.shop_cat_tires)
            ShopCategory.ELECTRICAL -> itemView.context.getString(R.string.shop_cat_electrical)
            ShopCategory.MOTORCYCLE -> itemView.context.getString(R.string.shop_cat_motorcycle)
            ShopCategory.PERFORMANCE -> itemView.context.getString(R.string.shop_cat_performance)
            ShopCategory.DETAILING -> itemView.context.getString(R.string.shop_cat_detailing)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ServiceShop>() {
        override fun areItemsTheSame(a: ServiceShop, b: ServiceShop) = a.id == b.id
        override fun areContentsTheSame(a: ServiceShop, b: ServiceShop) = a == b
    }
}
