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
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.databinding.FragmentFleetDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FleetDashboardFragment : Fragment() {

    private var _binding: FragmentFleetDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FleetDashboardViewModel by viewModels()
    private val fleetAdapter = FleetVehicleAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFleetDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerFleet.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFleet.adapter = fleetAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicles.collect { vehicles ->
                        viewModel.refreshStats(vehicles)
                    }
                }
                launch {
                    viewModel.stats.collect { stats ->
                        binding.tvTotalVehicles.text = stats.totalVehicles.toString()
                        binding.tvTotalKm.text = "%,d km".format(stats.totalKm)
                        binding.tvTotalCost.text = "€%,.0f".format(stats.totalCost)
                        binding.tvCheapestVehicle.text = stats.cheapestVehicle
                        fleetAdapter.submitList(stats.vehicleRankings)
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
