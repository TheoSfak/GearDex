package com.geardex.app.ui.garage

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.data.local.entity.ServicePlanType
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.databinding.DialogServicePlanBinding
import com.geardex.app.databinding.FragmentServicePlanBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ServicePlanFragment : Fragment() {

    private var _binding: FragmentServicePlanBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ServicePlanViewModel by viewModels()
    private lateinit var adapter: ServicePlanAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServicePlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        binding.btnAddPlan.setOnClickListener { showAddPlanDialog() }
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = ServicePlanAdapter(
            onDone = { summary ->
                viewModel.markDone(summary.plan)
                Snackbar.make(binding.root, R.string.service_plan_marked_done, Snackbar.LENGTH_SHORT).show()
            },
            onDelete = { summary ->
                viewModel.deletePlan(summary.plan)
                Snackbar.make(binding.root, R.string.service_plan_deleted, Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.rvServicePlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvServicePlans.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicle.collect { vehicle ->
                        vehicle ?: return@collect
                        binding.tvServicePlanVehicle.text = "${vehicle.make} ${vehicle.model}  ·  ${vehicle.currentKm} km"
                    }
                }
                launch {
                    viewModel.summaries.collect { summaries ->
                        adapter.submitList(summaries)
                        binding.tvServicePlanEmpty.visibility =
                            if (summaries.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvServicePlans.visibility =
                            if (summaries.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showAddPlanDialog() {
        val vehicle = viewModel.vehicle.value ?: return
        val dialogBinding = DialogServicePlanBinding.inflate(layoutInflater)
        val types = ServicePlanType.values()
        val labels = types.map { requireContext().servicePlanTypeLabel(it) }
        dialogBinding.actvPlanType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
        )

        fun applyDefaults(type: ServicePlanType) {
            val defaults = defaultIntervals(type, vehicle.type)
            dialogBinding.etPlanTitle.setText(requireContext().servicePlanTypeLabel(type))
            dialogBinding.etPlanKm.setText(defaults.first?.toString().orEmpty())
            dialogBinding.etPlanMonths.setText(defaults.second?.toString().orEmpty())
        }

        var selectedType = ServicePlanType.OIL_CHANGE
        dialogBinding.actvPlanType.setText(labels.first(), false)
        applyDefaults(selectedType)
        dialogBinding.actvPlanType.setOnItemClickListener { _, _, position, _ ->
            selectedType = types[position]
            applyDefaults(selectedType)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.service_plan_add)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = dialogBinding.etPlanTitle.text?.toString()?.trim().orEmpty()
                val intervalKm = dialogBinding.etPlanKm.text?.toString()?.trim()?.toIntOrNull()
                val intervalMonths = dialogBinding.etPlanMonths.text?.toString()?.trim()?.toIntOrNull()
                if (title.isBlank() || (intervalKm == null && intervalMonths == null)) {
                    Snackbar.make(binding.root, R.string.service_plan_invalid, Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.addPlan(selectedType, title, intervalKm, intervalMonths)
            }
            .show()
    }

    private fun defaultIntervals(type: ServicePlanType, vehicleType: VehicleType): Pair<Int?, Int?> = when (type) {
        ServicePlanType.OIL_CHANGE -> 6_000 to 6
        ServicePlanType.AIR_FILTER -> 12_000 to 12
        ServicePlanType.BRAKE_PADS -> 20_000 to null
        ServicePlanType.TIMING_BELT -> 90_000 to 60
        ServicePlanType.CABIN_FILTER -> 12_000 to 12
        ServicePlanType.CHAIN_LUBE -> if (vehicleType == VehicleType.ATV) 1_000 to 2 else 800 to 1
        ServicePlanType.VALVE_CLEARANCE -> 24_000 to 24
        ServicePlanType.FORK_OIL -> 24_000 to 24
        ServicePlanType.TIRE_CHECK -> 10_000 to 12
        ServicePlanType.CUSTOM -> null to null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
