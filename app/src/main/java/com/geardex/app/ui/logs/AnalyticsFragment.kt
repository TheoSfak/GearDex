package com.geardex.app.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.geardex.app.databinding.FragmentAnalyticsBinding
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by viewModels()

    private val economyProducer = CartesianChartModelProducer()
    private val monthlyProducer = CartesianChartModelProducer()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chartEconomy.modelProducer = economyProducer
        binding.chartMonthly.modelProducer = monthlyProducer

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.vehicles.collect { vehicles ->
                        val names = vehicles.map { "${it.make} ${it.model}" }
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            names
                        ).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }
                        binding.spinnerAnalyticsVehicle.adapter = adapter
                        binding.spinnerAnalyticsVehicle.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    v: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    if (position in vehicles.indices) {
                                        viewModel.selectVehicle(vehicles[position].id)
                                    }
                                }
                                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                            }
                        if (vehicles.isNotEmpty() && viewModel.selectedVehicleId.value < 0) {
                            viewModel.selectVehicle(vehicles[0].id)
                        }
                    }
                }

                launch {
                    viewModel.stats.collect { stats ->
                        // Stat cards
                        binding.tvAvgEconomy.text = stats.avgEconomy
                            ?.let { "%.1f L/100".format(it) } ?: "—"
                        binding.tvTotalFuelCost.text = "€%.2f".format(stats.totalFuelCost)
                        binding.tvTotalServiceCost.text = "€%.2f".format(stats.totalServiceCost)
                        binding.tvTotalKm.text = "${stats.totalRecordedKm} km"

                        val hasData = stats.economyPoints.isNotEmpty() ||
                                stats.monthlySpend.isNotEmpty()
                        binding.tvNoData.visibility = if (hasData) View.GONE else View.VISIBLE

                        // Economy line chart
                        if (stats.economyPoints.isNotEmpty()) {
                            val yValues = stats.economyPoints.map { it.second.toDouble() }
                            economyProducer.runTransaction {
                                lineSeries { series(yValues) }
                            }
                            binding.chartEconomy.visibility = View.VISIBLE
                        } else {
                            binding.chartEconomy.visibility = View.GONE
                        }

                        // Monthly spend bar chart
                        if (stats.monthlySpend.isNotEmpty()) {
                            val spendValues = stats.monthlySpend.values.toList()
                            monthlyProducer.runTransaction {
                                columnSeries { series(spendValues) }
                            }
                            binding.chartMonthly.visibility = View.VISIBLE
                        } else {
                            binding.chartMonthly.visibility = View.GONE
                        }
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
