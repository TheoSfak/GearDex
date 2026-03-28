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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.databinding.FragmentGarageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GarageFragment : Fragment() {

    private var _binding: FragmentGarageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GarageViewModel by viewModels()
    private lateinit var adapter: VehicleAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGarageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = VehicleAdapter { vehicle ->
            val action = GarageFragmentDirections.actionGarageToVehicleDetail(vehicle.id)
            findNavController().navigate(action)
        }

        binding.recyclerVehicles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GarageFragment.adapter
        }

        binding.fabAddVehicle.setOnClickListener {
            findNavController().navigate(R.id.action_garage_to_addVehicle)
        }

        binding.btnAddFromEmpty.setOnClickListener {
            findNavController().navigate(R.id.action_garage_to_addVehicle)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vehicles.collect { vehicles ->
                    adapter.submitList(vehicles)
                    val isEmpty = vehicles.isEmpty()
                    binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.recyclerVehicles.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.tvGarageSubtitle.text = if (isEmpty) "" else {
                        val count = vehicles.size
                        resources.getQuantityString(R.plurals.garage_vehicle_count, count, count)
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
