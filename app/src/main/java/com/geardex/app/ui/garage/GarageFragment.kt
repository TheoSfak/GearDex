package com.geardex.app.ui.garage

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    outRect.bottom = resources.getDimensionPixelSize(R.dimen.spacing_sm)
                }
            })
            layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_slide_up)
        }

        binding.fabAddVehicle.setOnClickListener {
            findNavController().navigate(R.id.action_garage_to_addVehicle)
        }

        binding.btnAddFromEmpty.setOnClickListener {
            findNavController().navigate(R.id.action_garage_to_addVehicle)
        }

        binding.btnFleetDashboard.setOnClickListener {
            findNavController().navigate(R.id.action_garage_to_fleetDashboard)
        }

        binding.rowOnboardingVehicle.setOnClickListener {
            findNavController().navigate(R.id.action_garage_to_addVehicle)
        }

        binding.rowOnboardingReminder.setOnClickListener {
            findNavController().navigate(R.id.addReminderFragment)
        }

        binding.rowOnboardingDocument.setOnClickListener {
            findNavController().navigate(R.id.addDocumentFragment)
        }

        binding.rowOnboardingService.setOnClickListener {
            findNavController().navigate(R.id.addServiceLogFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicles.collect { vehicles ->
                        adapter.submitList(vehicles) {
                            if (vehicles.isNotEmpty()) {
                                binding.recyclerVehicles.scheduleLayoutAnimation()
                            }
                        }
                        val isEmpty = vehicles.isEmpty()
                        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        binding.recyclerVehicles.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        binding.fabAddVehicle.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        binding.btnFleetDashboard.visibility = if (vehicles.size >= 2) View.VISIBLE else View.GONE
                        binding.tvGarageSubtitle.text = if (isEmpty) "" else {
                            val count = vehicles.size
                            resources.getQuantityString(R.plurals.garage_vehicle_count, count, count)
                        }
                    }
                }
                launch {
                    viewModel.scores.collect { scores ->
                        adapter.scores = scores
                    }
                }
                launch {
                    viewModel.onboardingState.collect { state ->
                        bindOnboarding(state)
                    }
                }
            }
        }
    }


    private fun bindOnboarding(state: OnboardingState) {
        binding.cardOnboarding.visibility = if (state.isComplete) View.GONE else View.VISIBLE
        binding.progressOnboarding.progress = state.completedCount

        val progressText = getString(R.string.onboarding_subtitle, state.completedCount, state.totalCount)
        binding.tvOnboardingSubtitle.text = progressText
        binding.tvOnboardingCount.text = progressText

        bindOnboardingStep(binding.iconOnboardingVehicle, binding.tvOnboardingVehicle, state.hasVehicle)
        bindOnboardingStep(binding.iconOnboardingReminder, binding.tvOnboardingReminder, state.hasReminder)
        bindOnboardingStep(binding.iconOnboardingDocument, binding.tvOnboardingDocument, state.hasDocument)
        bindOnboardingStep(binding.iconOnboardingService, binding.tvOnboardingService, state.hasServiceLog)

        setOnboardingStepEnabled(binding.rowOnboardingReminder, state.hasVehicle)
        setOnboardingStepEnabled(binding.rowOnboardingDocument, state.hasVehicle)
        setOnboardingStepEnabled(binding.rowOnboardingService, state.hasVehicle)
    }

    private fun bindOnboardingStep(icon: ImageView, label: TextView, isComplete: Boolean) {
        val color = if (isComplete) R.color.color_success else R.color.text_hint
        icon.setColorFilter(ContextCompat.getColor(requireContext(), color))
        icon.alpha = if (isComplete) 1f else 0.45f
        label.setTextColor(ContextCompat.getColor(requireContext(), if (isComplete) R.color.text_secondary else R.color.text_primary))
    }

    private fun setOnboardingStepEnabled(row: View, isEnabled: Boolean) {
        row.isEnabled = isEnabled
        row.alpha = if (isEnabled) 1f else 0.45f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
