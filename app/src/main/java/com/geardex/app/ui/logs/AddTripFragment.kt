package com.geardex.app.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.data.local.entity.TripPurpose
import com.geardex.app.databinding.FragmentAddTripBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddTripFragment : Fragment() {

    private var _binding: FragmentAddTripBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TripsViewModel by viewModels()

    private var vehicles = emptyList<com.geardex.app.data.local.entity.Vehicle>()
    private var selectedVehicleIndex = 0
    private var selectedPurpose = TripPurpose.COMMUTE
    private var selectedDate: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etTripDate.setText(dateFormat.format(Date(selectedDate)))

        // Purpose dropdown
        val purposes = TripPurpose.entries.toList()
        val purposeNames = purposes.map { purposeName(requireContext(), it.name) }
        val purposeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, purposeNames)
        binding.spinnerPurpose.setAdapter(purposeAdapter)
        binding.spinnerPurpose.setText(purposeNames[0], false)
        binding.spinnerPurpose.setOnItemClickListener { _, _, pos, _ ->
            if (pos in purposes.indices) selectedPurpose = purposes[pos]
        }

        // Date picker
        binding.etTripDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.trip_date))
                .setSelection(selectedDate)
                .build()
            picker.addOnPositiveButtonClickListener { ms ->
                selectedDate = ms
                binding.etTripDate.setText(dateFormat.format(Date(ms)))
            }
            picker.show(parentFragmentManager, "trip_date_picker")
        }

        // Vehicle dropdown
        binding.spinnerTripVehicle.setOnItemClickListener { _, _, pos, _ ->
            selectedVehicleIndex = pos
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vehicles.collect { list ->
                    vehicles = list
                    val names = list.map { "${it.make} ${it.model}" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                    binding.spinnerTripVehicle.setAdapter(adapter)
                    if (names.isNotEmpty()) {
                        binding.spinnerTripVehicle.setText(names[0], false)
                        selectedVehicleIndex = 0
                    }
                }
            }
        }

        binding.btnSaveTrip.setOnClickListener { validateAndSave() }
    }

    private fun validateAndSave() {
        val startOdo = binding.etStartOdometer.text?.toString()?.toIntOrNull()
        val endOdo = binding.etEndOdometer.text?.toString()?.toIntOrNull()

        if (startOdo == null) {
            binding.etStartOdometer.error = getString(R.string.error_invalid_number)
            return
        }
        if (endOdo == null || endOdo <= startOdo) {
            binding.tilEndOdometer.error = getString(R.string.error_invalid_number)
            return
        }
        binding.tilEndOdometer.error = null

        if (vehicles.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.logs_no_vehicle), Snackbar.LENGTH_SHORT).show()
            return
        }

        val vehicleId = vehicles[selectedVehicleIndex].id
        val fuelUsed = binding.etFuelUsed.text?.toString()?.toDoubleOrNull()
        val cost = binding.etTripCost.text?.toString()?.toDoubleOrNull()
        val notes = binding.etTripNotes.text?.toString()?.trim() ?: ""

        viewModel.addTrip(
            vehicleId = vehicleId,
            startOdometer = startOdo,
            endOdometer = endOdo,
            date = selectedDate,
            purpose = selectedPurpose.name,
            notes = notes,
            fuelUsedLiters = fuelUsed,
            costEuro = cost
        )
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
