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
            binding.tvDocName.text = doc.fileName
            binding.tvDocType.text = when (doc.documentType) {
                DocumentType.KTEO -> binding.root.context.getString(R.string.doc_type_kteo)
                DocumentType.INSURANCE -> binding.root.context.getString(R.string.doc_type_insurance)
                DocumentType.ROAD_TAX -> binding.root.context.getString(R.string.doc_type_road_tax)
                DocumentType.RECEIPT -> binding.root.context.getString(R.string.doc_type_receipt)
                DocumentType.OTHER -> binding.root.context.getString(R.string.doc_type_other)
            }
            binding.tvDocExpiry.text = doc.expiryDate?.let {
                binding.root.context.getString(R.string.doc_expires_on, dateFormat.format(Date(it)))
            } ?: binding.root.context.getString(R.string.doc_no_expiry)

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
