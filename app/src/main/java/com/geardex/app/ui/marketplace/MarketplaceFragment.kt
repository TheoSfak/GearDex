package com.geardex.app.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.data.model.PartCategory
import com.geardex.app.databinding.FragmentMarketplaceBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MarketplaceFragment : Fragment() {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MarketplaceViewModel by viewModels()
    private lateinit var partAdapter: MarketplacePartAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMarketplaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        partAdapter = MarketplacePartAdapter { /* part click */ }
        binding.recyclerParts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerParts.adapter = partAdapter

        // Search
        binding.etSearch.doAfterTextChanged { viewModel.searchQuery.value = it?.toString() ?: "" }

        // Category filter chips
        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.marketplace_cat_other).replace("Other", "All")
            isCheckable = true
            isChecked = true
            setChipBackgroundColorResource(R.color.accent_marketplace_bg)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_marketplace))
        }
        allChip.setOnClickListener { viewModel.selectedCategory.value = null }
        binding.chipGroupFilter.addView(allChip)

        PartCategory.entries.forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = partCategoryName(requireContext(), cat)
                isCheckable = true
                setChipBackgroundColorResource(R.color.accent_marketplace_bg)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_marketplace))
            }
            chip.setOnClickListener { viewModel.selectedCategory.value = cat }
            binding.chipGroupFilter.addView(chip)
        }

        binding.fabAddPart.setOnClickListener {
            findNavController().navigate(R.id.action_marketplace_to_addPart)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredParts.collect { parts ->
                    partAdapter.submitList(parts)
                    binding.layoutEmpty.visibility = if (parts.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerParts.visibility = if (parts.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun partCategoryName(ctx: android.content.Context, cat: PartCategory): String = when (cat) {
    PartCategory.ENGINE -> ctx.getString(R.string.marketplace_cat_engine)
    PartCategory.BRAKES -> ctx.getString(R.string.marketplace_cat_brakes)
    PartCategory.SUSPENSION -> ctx.getString(R.string.marketplace_cat_suspension)
    PartCategory.ELECTRICAL -> ctx.getString(R.string.marketplace_cat_electrical)
    PartCategory.BODY -> ctx.getString(R.string.marketplace_cat_body)
    PartCategory.EXHAUST -> ctx.getString(R.string.marketplace_cat_exhaust)
    PartCategory.TIRES -> ctx.getString(R.string.marketplace_cat_tires)
    PartCategory.WHEELS -> ctx.getString(R.string.marketplace_cat_wheels)
    PartCategory.INTERIOR -> ctx.getString(R.string.marketplace_cat_interior)
    PartCategory.ACCESSORIES -> ctx.getString(R.string.marketplace_cat_accessories)
    PartCategory.OTHER -> ctx.getString(R.string.marketplace_cat_other)
}
