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
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.animation.AnimationUtils
import com.geardex.app.R
import com.geardex.app.databinding.FragmentLogsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogsViewModel by viewModels()
    private val fuelAdapter = FuelLogAdapter()
    private val serviceAdapter = ServiceLogAdapter()
    private var currentTab = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLogs.adapter = fuelAdapter
        binding.recyclerLogs.layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_slide_up)

        // Lazily insert Reminders, Analytics and Expenses child fragments
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.container_reminders, RemindersFragment())
                .replace(R.id.container_analytics, AnalyticsFragment())
                .replace(R.id.container_expenses, ExpensesFragment())
                .replace(R.id.container_trips, TripsFragment())
                .commitNow()
        }

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                showTab(currentTab)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        binding.btnAddFuel.setOnClickListener {
            findNavController().navigate(R.id.action_logs_to_addFuel)
        }
        binding.btnAddService.setOnClickListener {
            findNavController().navigate(R.id.action_logs_to_addService)
        }
        binding.fabAddReminder.setOnClickListener {
            findNavController().navigate(R.id.action_logs_to_addReminder)
        }
        binding.fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_logs_to_addExpense)
        }
        binding.fabAddTrip.setOnClickListener {
            findNavController().navigate(R.id.action_logs_to_addTrip)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicles.collect { vehicles ->
                        val names = vehicles.map { "${it.make} ${it.model}" }
                        val dropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                        binding.spinnerVehicle.setAdapter(dropdownAdapter)
                        binding.spinnerVehicle.setOnItemClickListener { _, _, pos, _ ->
                            if (vehicles.isNotEmpty()) viewModel.selectVehicle(vehicles[pos].id)
                        }
                        if (vehicles.isNotEmpty()) {
                            binding.spinnerVehicle.setText(names[0], false)
                            viewModel.selectVehicle(vehicles[0].id)
                        }
                    }
                }
                launch { viewModel.fuelLogs.collect { fuelAdapter.submitList(it) } }
                launch { viewModel.serviceLogs.collect { serviceAdapter.submitList(it) } }
            }
        }
    }

    private fun showTab(tab: Int) {
        binding.recyclerLogs.visibility = if (tab in 0..1) View.VISIBLE else View.GONE
        binding.containerReminders.visibility = if (tab == 2) View.VISIBLE else View.GONE
        binding.containerAnalytics.visibility = if (tab == 3) View.VISIBLE else View.GONE
        binding.containerExpenses.visibility = if (tab == 4) View.VISIBLE else View.GONE
        binding.containerTrips.visibility = if (tab == 5) View.VISIBLE else View.GONE
        binding.fabAddReminder.visibility = if (tab == 2) View.VISIBLE else View.GONE
        binding.fabAddExpense.visibility = if (tab == 4) View.VISIBLE else View.GONE
        binding.fabAddTrip.visibility = if (tab == 5) View.VISIBLE else View.GONE

        if (tab in 0..1) {
            binding.recyclerLogs.adapter = if (tab == 0) fuelAdapter else serviceAdapter
        }

        // Notify expenses child fragment of selected vehicle
        if (tab == 4) {
            val expensesFragment = childFragmentManager.findFragmentById(R.id.container_expenses) as? ExpensesFragment
            val vehicleId = viewModel.selectedVehicleId.value
            if (vehicleId > 0) expensesFragment?.updateVehicle(vehicleId)
        }

        // Notify trips child fragment of selected vehicle
        if (tab == 5) {
            val tripsFragment = childFragmentManager.findFragmentById(R.id.container_trips) as? TripsFragment
            val vehicleId = viewModel.selectedVehicleId.value
            if (vehicleId > 0) tripsFragment?.updateVehicle(vehicleId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

