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
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.databinding.FragmentAddReminderBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddReminderFragment : Fragment() {

    private var _binding: FragmentAddReminderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RemindersViewModel by viewModels()

    private var vehicles = emptyList<com.geardex.app.data.local.entity.Vehicle>()
    private var selectedDate: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Default: KM type selected
        binding.toggleReminderType.check(R.id.btn_type_km)
        binding.layoutTargetKm.visibility = View.VISIBLE
        binding.layoutTargetDate.visibility = View.GONE

        binding.toggleReminderType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.layoutTargetKm.visibility = if (checkedId == R.id.btn_type_km) View.VISIBLE else View.GONE
            binding.layoutTargetDate.visibility = if (checkedId == R.id.btn_type_date) View.VISIBLE else View.GONE
        }

        binding.btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.reminder_pick_date))
                .build()
            picker.addOnPositiveButtonClickListener { ms ->
                selectedDate = ms
                binding.tvSelectedDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ms))
            }
            picker.show(parentFragmentManager, "date_picker")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vehicles.collect { list ->
                    vehicles = list
                    val names = list.map { "${it.make} ${it.model}" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerReminderVehicle.adapter = adapter
                }
            }
        }

        binding.btnSaveReminder.setOnClickListener { save() }
    }

    private fun save() {
        val title = binding.etReminderTitle.text?.toString()?.trim() ?: ""
        if (title.isBlank()) {
            Snackbar.make(binding.root, getString(R.string.error_required_field), Snackbar.LENGTH_SHORT).show()
            return
        }

        val vehicleIndex = binding.spinnerReminderVehicle.selectedItemPosition
        if (vehicles.isEmpty() || vehicleIndex < 0) {
            Snackbar.make(binding.root, getString(R.string.logs_no_vehicle), Snackbar.LENGTH_SHORT).show()
            return
        }
        val vehicleId = vehicles[vehicleIndex].id

        val isKm = binding.toggleReminderType.checkedButtonId == R.id.btn_type_km

        if (isKm) {
            val kmStr = binding.etTargetKm.text?.toString()?.trim() ?: ""
            val km = kmStr.toIntOrNull()
            if (km == null || km <= 0) {
                Snackbar.make(binding.root, getString(R.string.error_invalid_number), Snackbar.LENGTH_SHORT).show()
                return
            }
            viewModel.addReminder(vehicleId, title, ReminderType.KM_BASED, targetKm = km, targetDate = null)
        } else {
            if (selectedDate == null) {
                Snackbar.make(binding.root, getString(R.string.reminder_pick_date), Snackbar.LENGTH_SHORT).show()
                return
            }
            viewModel.addReminder(vehicleId, title, ReminderType.DATE_BASED, targetKm = null, targetDate = selectedDate)
        }

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
