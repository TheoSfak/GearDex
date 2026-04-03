package com.geardex.app.ui.garage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.databinding.FragmentEditVehicleBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditVehicleFragment : Fragment() {

    private var _binding: FragmentEditVehicleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditVehicleViewModel by viewModels()
    private var selectedType: VehicleType = VehicleType.CAR
    private var dataLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toggleVehicleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedType = when (checkedId) {
                    R.id.btn_type_car -> VehicleType.CAR
                    R.id.btn_type_motorcycle -> VehicleType.MOTORCYCLE
                    R.id.btn_type_atv -> VehicleType.ATV
                    else -> VehicleType.CAR
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vehicle.collect { vehicle ->
                    if (vehicle != null && !dataLoaded) {
                        dataLoaded = true
                        selectedType = vehicle.type
                        binding.etMake.setText(vehicle.make)
                        binding.etModel.setText(vehicle.model)
                        binding.etYear.setText(vehicle.year.toString())
                        binding.etPlate.setText(vehicle.licensePlate)
                        binding.etKm.setText(vehicle.currentKm.toString())
                        val id = when (vehicle.type) {
                            VehicleType.CAR -> R.id.btn_type_car
                            VehicleType.MOTORCYCLE -> R.id.btn_type_motorcycle
                            VehicleType.ATV -> R.id.btn_type_atv
                        }
                        binding.toggleVehicleType.check(id)
                    }
                }
            }
        }

        binding.btnSaveVehicle.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val make = binding.etMake.text?.toString()?.trim() ?: ""
        val model = binding.etModel.text?.toString()?.trim() ?: ""
        val yearStr = binding.etYear.text?.toString()?.trim() ?: ""
        val plate = binding.etPlate.text?.toString()?.trim() ?: ""
        val kmStr = binding.etKm.text?.toString()?.trim() ?: ""

        if (make.isEmpty()) { binding.tilMake.error = getString(R.string.error_required_field); return }
        else binding.tilMake.error = null

        if (model.isEmpty()) { binding.tilModel.error = getString(R.string.error_required_field); return }
        else binding.tilModel.error = null

        val year = yearStr.toIntOrNull()
        if (year == null || year < 1900 || year > 2100) {
            binding.tilYear.error = getString(R.string.error_invalid_number); return
        } else binding.tilYear.error = null

        if (plate.isEmpty()) { binding.tilPlate.error = getString(R.string.error_required_field); return }
        else binding.tilPlate.error = null

        val km = kmStr.toIntOrNull()
        if (km == null || km < 0) {
            binding.tilKm.error = getString(R.string.error_invalid_number); return
        } else binding.tilKm.error = null

        binding.btnSaveVehicle.isEnabled = false
        viewModel.saveChanges(selectedType, make, model, year, plate, km) {
            binding.btnSaveVehicle.post { findNavController().popBackStack() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
