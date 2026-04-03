package com.geardex.app.ui.garage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.databinding.FragmentHealthDetailBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HealthDetailFragment : Fragment() {

    private var _binding: FragmentHealthDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHealthDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicle.collect { v ->
                        v ?: return@collect
                        binding.tvHealthVehicleName.text = "${v.make} ${v.model} · ${v.year}"
                    }
                }
                launch {
                    viewModel.healthBreakdown.collect { bd ->
                        bd ?: return@collect

                        // Gauge
                        binding.gaugeHealth.setScore(bd.overall)

                        // Overall label
                        binding.tvOverallLabel.text = when {
                            bd.overall >= 80 -> getString(R.string.health_condition_excellent)
                            bd.overall >= 60 -> getString(R.string.health_condition_good)
                            bd.overall >= 40 -> getString(R.string.health_condition_fair)
                            else -> getString(R.string.health_condition_poor)
                        }
                        binding.tvOverallLabel.setTextColor(
                            ContextCompat.getColor(requireContext(), when {
                                bd.overall >= 70 -> R.color.score_good
                                bd.overall >= 40 -> R.color.score_fair
                                else -> R.color.score_poor
                            })
                        )

                        // Predicted cost
                        if (bd.predictedMonthlyCost != null) {
                            binding.cardPredictedCost.visibility = View.VISIBLE
                            binding.tvPredictedCost.text = "€${"%.0f".format(bd.predictedMonthlyCost)}"
                        } else {
                            binding.cardPredictedCost.visibility = View.GONE
                        }

                        // Pillar bars
                        setupPillar(binding.pillarMaintenance.root,
                            R.drawable.ic_reminder_bell, R.color.accent_reminder,
                            getString(R.string.health_pillar_maintenance), bd.maintenanceCompliance)
                        setupPillar(binding.pillarFuel.root,
                            R.drawable.ic_fuel, R.color.accent_fuel,
                            getString(R.string.health_pillar_fuel), bd.fuelEfficiency)
                        setupPillar(binding.pillarService.root,
                            R.drawable.ic_mechanic, R.color.accent_service,
                            getString(R.string.health_pillar_service), bd.serviceRegularity)
                        setupPillar(binding.pillarExpense.root,
                            R.drawable.ic_expense, R.color.accent_expense,
                            getString(R.string.health_pillar_expense), bd.expensePattern)
                        setupPillar(binding.pillarCondition.root,
                            R.drawable.ic_car, R.color.geardex_orange,
                            getString(R.string.health_pillar_condition), bd.vehicleCondition)

                        // Tips based on weakest pillars
                        binding.tvHealthTips.text = generateTips(bd)
                    }
                }
            }
        }
    }

    private fun setupPillar(pillarView: View, iconRes: Int, colorRes: Int, name: String, score: Int) {
        val icon = pillarView.findViewById<ImageView>(R.id.iv_pillar_icon)
        val tvName = pillarView.findViewById<TextView>(R.id.tv_pillar_name)
        val tvScore = pillarView.findViewById<TextView>(R.id.tv_pillar_score)
        val progress = pillarView.findViewById<LinearProgressIndicator>(R.id.progress_pillar)

        icon.setImageResource(iconRes)
        icon.setColorFilter(ContextCompat.getColor(requireContext(), colorRes))
        tvName.text = name
        tvScore.text = score.toString()

        val barColor = ContextCompat.getColor(requireContext(), when {
            score >= 70 -> R.color.score_good
            score >= 40 -> R.color.score_fair
            else -> R.color.score_poor
        })
        progress.setIndicatorColor(barColor)
        progress.trackColor = ContextCompat.getColor(requireContext(), R.color.surface_elevated)
        progress.setProgressCompat(score, true)
    }

    private fun generateTips(bd: HealthScoreBreakdown): String {
        val tips = mutableListOf<String>()

        if (bd.maintenanceCompliance < 60)
            tips.add(getString(R.string.health_tip_maintenance))
        if (bd.fuelEfficiency < 60)
            tips.add(getString(R.string.health_tip_fuel))
        if (bd.serviceRegularity < 60)
            tips.add(getString(R.string.health_tip_service))
        if (bd.expensePattern < 60)
            tips.add(getString(R.string.health_tip_expense))
        if (bd.vehicleCondition < 50)
            tips.add(getString(R.string.health_tip_condition))

        if (tips.isEmpty())
            tips.add(getString(R.string.health_tip_great))

        return tips.joinToString("\n") { "• $it" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
