package com.geardex.app.ui.logs

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
import com.geardex.app.databinding.FragmentAddServiceLogBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddServiceLogFragment : Fragment() {

    private var _binding: FragmentAddServiceLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogsViewModel by viewModels()
    private var selectedVehicleType: VehicleType = VehicleType.CAR

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddServiceLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show/hide vehicle-type specific fields based on selected vehicle
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vehicles.collect { vehicles ->
                    val id = viewModel.selectedVehicleId.value
                    val vehicle = vehicles.find { it.id == id }
                    if (vehicle != null) {
                        selectedVehicleType = vehicle.type
                        updateConditionalFields(vehicle.type)
                    }
                }
            }
        }

        binding.btnSaveService.setOnClickListener {
            val vehicleId = viewModel.selectedVehicleId.value
            if (vehicleId < 0) return@setOnClickListener

            val km = binding.etServiceKm.text?.toString()?.toIntOrNull()
            val cost = binding.etServiceCost.text?.toString()?.toDoubleOrNull()

            if (km == null) { binding.tilServiceKm.error = getString(R.string.error_invalid_number); return@setOnClickListener }
            if (cost == null) { binding.tilServiceCost.error = getString(R.string.error_invalid_number); return@setOnClickListener }

            val checks = mapOf(
                "oilChange" to binding.cbOilChange.isChecked,
                "airFilter" to binding.cbAirFilter.isChecked,
                "brakePads" to binding.cbBrakePads.isChecked,
                "timingBelt" to binding.cbTimingBelt.isChecked,
                "cabinFilter" to binding.cbCabinFilter.isChecked,
                "chainLube" to binding.cbChainLube.isChecked,
                "valveClearance" to binding.cbValveClearance.isChecked,
                "forkOil" to binding.cbForkOil.isChecked,
                "tireCheck" to binding.cbTireCheck.isChecked
            )

            viewModel.addServiceLog(
                vehicleId, km, cost, System.currentTimeMillis(),
                binding.etMechanic.text?.toString() ?: "",
                binding.etServiceNotes.text?.toString() ?: "",
                selectedVehicleType, checks
            )
            findNavController().popBackStack()
        }
    }

    private fun updateConditionalFields(type: VehicleType) {
        binding.sectionCar.visibility = if (type == VehicleType.CAR) View.VISIBLE else View.GONE
        binding.sectionMoto.visibility = if (type == VehicleType.MOTORCYCLE || type == VehicleType.ATV) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
