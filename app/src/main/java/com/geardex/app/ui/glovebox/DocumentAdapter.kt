package com.geardex.app.ui.glovebox

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.GloveboxDocument
import com.geardex.app.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentAdapter(
    private val onOpenClick: (GloveboxDocument) -> Unit,
    private val onDeleteClick: (GloveboxDocument) -> Unit
) : ListAdapter<GloveboxDocument, DocumentAdapter.DocumentViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class DocumentViewHolder(private val binding: ItemDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(doc: GloveboxDocument) {
            val ctx = binding.root.context
            binding.tvDocName.text = doc.fileName
            binding.tvDocType.text = when (doc.documentType) {
                DocumentType.KTEO -> ctx.getString(R.string.doc_type_kteo)
                DocumentType.INSURANCE -> ctx.getString(R.string.doc_type_insurance)
                DocumentType.ROAD_TAX -> ctx.getString(R.string.doc_type_road_tax)
                DocumentType.RECEIPT -> ctx.getString(R.string.doc_type_receipt)
                DocumentType.OTHER -> ctx.getString(R.string.doc_type_other)
            }
            binding.tvDocExpiry.text = doc.expiryDate?.let {
                ctx.getString(R.string.doc_expires_on, dateFormat.format(Date(it)))
            } ?: ctx.getString(R.string.doc_no_expiry)

            // Expiry status badge
            val now = System.currentTimeMillis()
            val thirtyDays = 30L * 24 * 60 * 60 * 1000
            val expiry = doc.expiryDate
            when {
                expiry == null -> {
                    binding.tvDocStatus.text = "—"
                    binding.tvDocStatus.setBackgroundResource(R.drawable.bg_pill_glovebox)
                    binding.tvDocStatus.setTextColor(ctx.getColor(R.color.text_secondary))
                }
                expiry < now -> {
                    binding.tvDocStatus.text = ctx.getString(R.string.doc_status_expired)
                    binding.tvDocStatus.setBackgroundResource(R.drawable.bg_pill_expired)
                    binding.tvDocStatus.setTextColor(ctx.getColor(R.color.color_error))
                }
                expiry < now + thirtyDays -> {
                    binding.tvDocStatus.text = ctx.getString(R.string.doc_status_expiring)
                    binding.tvDocStatus.setBackgroundResource(R.drawable.bg_pill_warning)
                    binding.tvDocStatus.setTextColor(ctx.getColor(R.color.color_warning))
                }
                else -> {
                    binding.tvDocStatus.text = ctx.getString(R.string.doc_status_valid)
                    binding.tvDocStatus.setBackgroundResource(R.drawable.bg_pill_valid)
                    binding.tvDocStatus.setTextColor(ctx.getColor(R.color.color_success))
                }
            }

            binding.root.setOnClickListener { onOpenClick(doc) }
            binding.btnDeleteDoc.setOnClickListener { onDeleteClick(doc) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<GloveboxDocument>() {
        override fun areItemsTheSame(o: GloveboxDocument, n: GloveboxDocument) = o.id == n.id
        override fun areContentsTheSame(o: GloveboxDocument, n: GloveboxDocument) = o == n
    }
}
