package com.geardex.app.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.Expense
import com.geardex.app.data.local.entity.ExpenseCategory
import com.geardex.app.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    private val onDelete: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            val ctx = binding.root.context
            binding.tvExpenseDate.text = dateFormat.format(Date(expense.date))
            binding.tvExpenseAmount.text = "€${"%.2f".format(expense.amount)}"
            binding.tvExpenseCategory.text = categoryName(ctx, expense.category)
            binding.tvExpenseDescription.text = expense.description.ifEmpty {
                categoryName(ctx, expense.category)
            }
            binding.tvExpenseDescription.visibility =
                if (expense.description.isNotEmpty()) View.VISIBLE else View.GONE
            binding.ivRecurring.visibility = if (expense.isRecurring) View.VISIBLE else View.GONE

            binding.ivExpenseIcon.setImageResource(categoryIcon(expense.category))

            binding.root.setOnLongClickListener {
                onDelete(expense)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(old: Expense, new: Expense) = old.id == new.id
        override fun areContentsTheSame(old: Expense, new: Expense) = old == new
    }
}

fun categoryName(ctx: android.content.Context, cat: ExpenseCategory): String = when (cat) {
    ExpenseCategory.FUEL -> ctx.getString(R.string.expense_cat_fuel)
    ExpenseCategory.SERVICE -> ctx.getString(R.string.expense_cat_service)
    ExpenseCategory.INSURANCE -> ctx.getString(R.string.expense_cat_insurance)
    ExpenseCategory.ROAD_TAX -> ctx.getString(R.string.expense_cat_road_tax)
    ExpenseCategory.PARKING -> ctx.getString(R.string.expense_cat_parking)
    ExpenseCategory.TOLLS -> ctx.getString(R.string.expense_cat_tolls)
    ExpenseCategory.FINES -> ctx.getString(R.string.expense_cat_fines)
    ExpenseCategory.TIRES -> ctx.getString(R.string.expense_cat_tires)
    ExpenseCategory.MODIFICATIONS -> ctx.getString(R.string.expense_cat_modifications)
    ExpenseCategory.OTHER -> ctx.getString(R.string.expense_cat_other)
}

fun categoryIcon(cat: ExpenseCategory): Int = when (cat) {
    ExpenseCategory.FUEL -> R.drawable.ic_fuel
    ExpenseCategory.SERVICE -> R.drawable.ic_mechanic
    ExpenseCategory.INSURANCE -> R.drawable.ic_shield
    ExpenseCategory.ROAD_TAX -> R.drawable.ic_document
    ExpenseCategory.PARKING -> R.drawable.ic_location
    ExpenseCategory.TOLLS -> R.drawable.ic_speed
    ExpenseCategory.FINES -> R.drawable.ic_reminder_bell
    ExpenseCategory.TIRES -> R.drawable.ic_tools
    ExpenseCategory.MODIFICATIONS -> R.drawable.ic_wrench
    ExpenseCategory.OTHER -> R.drawable.ic_expense
}
