package com.geardex.app.ui.shops

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.data.model.ServiceShop
import com.geardex.app.data.model.ShopCategory
import com.geardex.app.databinding.FragmentAddShopBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddShopFragment : Fragment() {

    private var _binding: FragmentAddShopBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShopDirectoryViewModel by viewModels()
    private var selectedCategory = ShopCategory.GENERAL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryNames = ShopCategory.entries.map { shopCategoryName(it) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.spinnerCategory.setAdapter(adapter)
        binding.spinnerCategory.setOnItemClickListener { _, _, pos, _ ->
            selectedCategory = ShopCategory.entries[pos]
        }

        binding.btnSubmit.setOnClickListener {
            val nameEn = binding.etNameEn.text?.toString()?.trim() ?: ""
            val nameEl = binding.etNameEl.text?.toString()?.trim() ?: ""
            if (nameEn.isBlank()) {
                Snackbar.make(binding.root, getString(R.string.error_required_field), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val shop = ServiceShop(
                id = "",
                nameEn = nameEn,
                nameEl = nameEl,
                category = selectedCategory,
                region = binding.etRegion.text?.toString()?.trim() ?: "",
                address = binding.etAddress.text?.toString()?.trim() ?: "",
                phone = binding.etPhone.text?.toString()?.trim() ?: "",
                rating = 0f,
                reviewCount = 0,
                latitude = 0.0,
                longitude = 0.0,
                submittedBy = "",
                submittedByUid = "",
                createdAt = System.currentTimeMillis()
            )

            viewLifecycleOwner.lifecycleScope.launch {
                val success = viewModel.submitShop(shop)
                if (success) {
                    Snackbar.make(binding.root, getString(R.string.community_submit_success), Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Snackbar.make(binding.root, getString(R.string.community_submit_error), Snackbar.LENGTH_SHORT).show()
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
