package com.geardex.app.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.data.model.PartCategory
import com.geardex.app.data.model.PartCondition
import com.geardex.app.databinding.FragmentAddPartBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddPartFragment : Fragment() {

    private var _binding: FragmentAddPartBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MarketplaceViewModel by viewModels()

    private var selectedCategory = PartCategory.OTHER
    private var selectedCondition = PartCondition.USED_GOOD

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Category dropdown
        val categories = PartCategory.entries.toList()
        val categoryNames = categories.map { partCategoryName(requireContext(), it) }
        val catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.spinnerCategory.setAdapter(catAdapter)
        binding.spinnerCategory.setOnItemClickListener { _, _, pos, _ ->
            selectedCategory = categories[pos]
        }

        // Condition dropdown
        val conditions = PartCondition.entries.toList()
        val conditionNames = conditions.map { conditionName(requireContext(), it) }
        val condAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, conditionNames)
        binding.spinnerCondition.setAdapter(condAdapter)
        binding.spinnerCondition.setText(conditionNames[2], false) // Default to Used Good
        binding.spinnerCondition.setOnItemClickListener { _, _, pos, _ ->
            selectedCondition = conditions[pos]
        }

        binding.btnSubmitPart.setOnClickListener { validateAndSubmit() }
    }

    private fun validateAndSubmit() {
        val name = binding.etPartName.text?.toString()?.trim() ?: ""
        if (name.isBlank()) {
            binding.etPartName.error = getString(R.string.error_required_field)
            return
        }
        val price = binding.etPrice.text?.toString()?.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.etPrice.error = getString(R.string.error_invalid_number)
            return
        }

        viewModel.submitPart(
            partName = name,
            category = selectedCategory,
            vehicleMake = binding.etVehicleMake.text?.toString()?.trim() ?: "",
            vehicleModel = binding.etVehicleModel.text?.toString()?.trim() ?: "",
            price = price,
            condition = selectedCondition,
            description = binding.etDescription.text?.toString()?.trim() ?: "",
            contactInfo = binding.etContact.text?.toString()?.trim() ?: "",
            region = binding.etRegion.text?.toString()?.trim() ?: ""
        )
        Snackbar.make(binding.root, getString(R.string.marketplace_add_part), Snackbar.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
