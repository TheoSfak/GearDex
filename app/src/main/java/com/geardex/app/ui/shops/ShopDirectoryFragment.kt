package com.geardex.app.ui.shops

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.data.model.ShopCategory
import com.geardex.app.databinding.FragmentShopDirectoryBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShopDirectoryFragment : Fragment() {

    private var _binding: FragmentShopDirectoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShopDirectoryViewModel by viewModels()
    private lateinit var shopAdapter: ServiceShopAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShopDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shopAdapter = ServiceShopAdapter { shop -> viewModel.toggleFavorite(shop) }
        binding.recyclerShops.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerShops.adapter = shopAdapter

        // Search
        binding.etSearch.doAfterTextChanged { viewModel.searchQuery.value = it?.toString() ?: "" }

        // Category chips
        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.marketplace_cat_other).replace("Other", "All")
            isCheckable = true
            isChecked = true
            setOnClickListener { viewModel.selectedCategory.value = null }
        }
        binding.chipGroupCategory.addView(allChip)

        ShopCategory.entries.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = shopCategoryName(category)
                isCheckable = true
                setOnClickListener { viewModel.selectedCategory.value = category }
            }
            binding.chipGroupCategory.addView(chip)
        }

        binding.fabAddShop.setOnClickListener {
            findNavController().navigate(R.id.action_shopDirectory_to_addShop)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredShops.collect { shops ->
                        shopAdapter.submitList(shops)
                        binding.tvEmpty.visibility = if (shops.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerShops.visibility = if (shops.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.favoriteIds.collect { ids ->
                        shopAdapter.favoriteIds = ids
                    }
                }
            }
        }
    }

    private fun shopCategoryName(cat: ShopCategory): String = when (cat) {
        ShopCategory.GENERAL -> getString(R.string.shop_cat_general)
        ShopCategory.ENGINE_SPECIALIST -> getString(R.string.shop_cat_engine)
        ShopCategory.BODY_SHOP -> getString(R.string.shop_cat_body)
        ShopCategory.TIRES -> getString(R.string.shop_cat_tires)
        ShopCategory.ELECTRICAL -> getString(R.string.shop_cat_electrical)
        ShopCategory.MOTORCYCLE -> getString(R.string.shop_cat_motorcycle)
        ShopCategory.PERFORMANCE -> getString(R.string.shop_cat_performance)
        ShopCategory.DETAILING -> getString(R.string.shop_cat_detailing)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
