package com.geardex.app.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.databinding.FragmentTripsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TripsViewModel by viewModels()
    private var tripAdapter: TripAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripAdapter = TripAdapter(
            onDelete = { trip ->
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirm_delete))
                    .setPositiveButton(getString(R.string.delete)) { _, _ -> viewModel.deleteTrip(trip) }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        )
        binding.recyclerTrips.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTrips.adapter = tripAdapter
        binding.recyclerTrips.layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_slide_up)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicles.collect { vehicles ->
                        if (vehicles.isNotEmpty() && viewModel.selectedVehicleId.value < 0) {
                            viewModel.selectVehicle(vehicles[0].id)
                        }
                    }
                }
                launch {
                    viewModel.trips.collect { trips ->
                        tripAdapter?.submitList(trips)
                        binding.layoutTripsEmpty.visibility =
                            if (trips.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerTrips.visibility =
                            if (trips.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.totalDistance.collect { total ->
                        binding.tvTotalDistance.text = "%,d km".format(total)
                    }
                }
                launch {
                    viewModel.trips.collect { trips ->
                        binding.tvTotalTrips.text = trips.size.toString()
                    }
                }
            }
        }
    }

    fun updateVehicle(vehicleId: Long) {
        viewModel.selectVehicle(vehicleId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
