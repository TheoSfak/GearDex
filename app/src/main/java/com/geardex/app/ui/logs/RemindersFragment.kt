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
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.databinding.FragmentRemindersBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RemindersViewModel by viewModels()
    private var reminderAdapter: ReminderAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerReminders.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicles.collect { vehicles ->
                        val vehicleNames = vehicles.associate { it.id to "${it.make} ${it.model}" }
                        reminderAdapter = ReminderAdapter(
                            vehicleNames = vehicleNames,
                            onMarkDone = { viewModel.markDone(it) },
                            onDelete = { viewModel.deleteReminder(it) }
                        )
                        binding.recyclerReminders.adapter = reminderAdapter

                        if (vehicles.isNotEmpty() && viewModel.selectedVehicleId.value < 0) {
                            viewModel.selectVehicle(vehicles[0].id)
                        }
                    }
                }
                launch {
                    viewModel.reminders.collect { reminders ->
                        reminderAdapter?.submitList(reminders)
                        binding.layoutRemindersEmpty.visibility =
                            if (reminders.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerReminders.visibility =
                            if (reminders.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
