package com.geardex.app.ui.garage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.geardex.app.R
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.databinding.FragmentVehicleDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VehicleDetailFragment : Fragment() {

    private var _binding: FragmentVehicleDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VehicleDetailViewModel by viewModels()
    private val args: VehicleDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVehicleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vehicle.collect { vehicle ->
                    vehicle ?: return@collect
                    binding.tvDetailName.text = "${vehicle.make} ${vehicle.model}"
                    binding.tvDetailYear.text = vehicle.year.toString()
                    binding.tvDetailPlate.text = vehicle.licensePlate
                    binding.tvDetailKm.text = "${vehicle.currentKm} km"
                    binding.tvDetailTypeBadge.text = when (vehicle.type) {
                        VehicleType.CAR -> getString(R.string.vehicle_type_car)
                        VehicleType.MOTORCYCLE -> getString(R.string.vehicle_type_motorcycle)
                        VehicleType.ATV -> getString(R.string.vehicle_type_atv)
                    }
                    // Pre-fill KM field with current value
                    if (binding.etUpdateKm.text.isNullOrEmpty()) {
                        binding.etUpdateKm.setText(vehicle.currentKm.toString())
                    }
                }
            }
        }

        binding.btnUpdateKm.setOnClickListener {
            val kmStr = binding.etUpdateKm.text?.toString()?.trim() ?: ""
            val km = kmStr.toIntOrNull()
            if (km == null || km < 0) {
                binding.tilUpdateKm.error = getString(R.string.error_invalid_number)
            } else {
                binding.tilUpdateKm.error = null
                viewModel.updateKm(km)
                Snackbar.make(binding.root, getString(R.string.vehicle_km_updated), Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnEditVehicle.setOnClickListener {
            val action = VehicleDetailFragmentDirections.actionVehicleDetailToEditVehicle(args.vehicleId)
            findNavController().navigate(action)
        }

        binding.btnDeleteVehicle.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.confirm_delete_vehicle_message))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    viewModel.vehicle.value?.let { v ->
                        viewModel.deleteVehicle(v)
                        findNavController().popBackStack(R.id.garageFragment, false)
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
