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

        // Lazily insert Reminders and Analytics child fragments
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.container_reminders, RemindersFragment())
                .replace(R.id.container_analytics, AnalyticsFragment())
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicles.collect { vehicles ->
                        val names = vehicles.map { "${it.make} ${it.model}" }
                        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.spinnerVehicle.adapter = spinnerAdapter
                        binding.spinnerVehicle.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                                if (vehicles.isNotEmpty()) viewModel.selectVehicle(vehicles[pos].id)
                            }
                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                        }
                        if (vehicles.isNotEmpty()) viewModel.selectVehicle(vehicles[0].id)
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
        binding.fabAddReminder.visibility = if (tab == 2) View.VISIBLE else View.GONE

        if (tab in 0..1) {
            binding.recyclerLogs.adapter = if (tab == 0) fuelAdapter else serviceAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

