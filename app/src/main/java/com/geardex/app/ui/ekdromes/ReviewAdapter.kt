package com.geardex.app.ui.ekdromes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.data.model.RouteReview
import com.geardex.app.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewAdapter : ListAdapter<RouteReview, ReviewAdapter.ReviewViewHolder>(DiffCallback) {

    inner class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: RouteReview) {
            binding.tvReviewUser.text = review.userName.ifEmpty { "Anonymous" }
            binding.ratingBarSmall.rating = review.rating

            if (review.comment.isNotEmpty()) {
                binding.tvReviewComment.text = review.comment
                binding.tvReviewComment.visibility = View.VISIBLE
            } else {
                binding.tvReviewComment.visibility = View.GONE
            }

            if (review.createdAt > 0) {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                binding.tvReviewDate.text = sdf.format(Date(review.createdAt))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<RouteReview>() {
        override fun areItemsTheSame(o: RouteReview, n: RouteReview) = o.id == n.id
        override fun areContentsTheSame(o: RouteReview, n: RouteReview) = o == n
    }
}
