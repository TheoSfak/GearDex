package com.geardex.app.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.databinding.FragmentAddFuelLogBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddFuelLogFragment : Fragment() {

    private var _binding: FragmentAddFuelLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddFuelLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveFuel.setOnClickListener {
            val vehicleId = viewModel.selectedVehicleId.value
            if (vehicleId < 0) return@setOnClickListener

            val odometer = binding.etOdometer.text?.toString()?.toIntOrNull()
            val liters = binding.etLiters.text?.toString()?.toDoubleOrNull()
            val cost = binding.etCost.text?.toString()?.toDoubleOrNull()
            val notes = binding.etNotes.text?.toString() ?: ""

            if (odometer == null) { binding.tilOdometer.error = getString(R.string.error_invalid_number); return@setOnClickListener }
            if (liters == null) { binding.tilLiters.error = getString(R.string.error_invalid_number); return@setOnClickListener }
            if (cost == null) { binding.tilCost.error = getString(R.string.error_invalid_number); return@setOnClickListener }

            viewModel.addFuelLog(vehicleId, odometer, liters, cost, System.currentTimeMillis(), notes)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
