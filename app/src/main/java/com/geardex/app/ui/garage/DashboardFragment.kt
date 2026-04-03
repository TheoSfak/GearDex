package com.geardex.app.ui.garage

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.databinding.FragmentDashboardBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private val args: DashboardFragmentArgs by navArgs()

    private lateinit var timelineAdapter: TimelineAdapter
    private lateinit var insightAdapter: InsightAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupButtons()
        observeData()
    }

    private fun setupRecyclerViews() {
        timelineAdapter = TimelineAdapter()
        binding.rvTimeline.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = timelineAdapter
        }

        insightAdapter = InsightAdapter()
        binding.rvInsights.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = insightAdapter
        }
    }

    private fun setupButtons() {
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

        binding.btnExportPdf.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                exportVehiclePdf()
            }
        }

        binding.btnEditVehicle.setOnClickListener {
            val action = DashboardFragmentDirections.actionDashboardToEditVehicle(args.vehicleId)
            findNavController().navigate(action)
        }

        binding.cardHealth.setOnClickListener {
            val action = DashboardFragmentDirections.actionDashboardToHealthDetail(args.vehicleId)
            findNavController().navigate(action)
        }

        binding.btnDriveMode.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_driveMode)
        }

        binding.btnMarketplace.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_marketplace)
        }

        binding.btnShops.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_shopDirectory)
        }

        binding.btnParking.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_parking)
        }

        binding.btnDeleteVehicle.setOnClickListener {
            val vehicleToDelete = viewModel.vehicle.value ?: return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.confirm_delete_vehicle_message))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    binding.btnDeleteVehicle.isEnabled = false
                    viewModel.deleteVehicle(vehicleToDelete) {
                        binding.btnDeleteVehicle.post {
                            findNavController().popBackStack(R.id.garageFragment, false)
                        }
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Vehicle header
                launch {
                    viewModel.vehicle.collect { vehicle ->
                        vehicle ?: return@collect
                        binding.tvDashboardVehicleName.text = "${vehicle.make} ${vehicle.model}"
                        binding.tvDashboardVehicleInfo.text = "${vehicle.year}  ·  ${vehicle.licensePlate}  ·  ${vehicle.currentKm} km"
                        binding.tvDashboardTypeBadge.text = when (vehicle.type) {
                            VehicleType.CAR -> getString(R.string.vehicle_type_car)
                            VehicleType.MOTORCYCLE -> getString(R.string.vehicle_type_motorcycle)
                            VehicleType.ATV -> getString(R.string.vehicle_type_atv)
                        }

                        // Vehicle-type-specific placeholder gradient
                        val placeholderBg = when (vehicle.type) {
                            VehicleType.CAR -> R.drawable.bg_vehicle_placeholder
                            VehicleType.MOTORCYCLE -> R.drawable.bg_vehicle_placeholder_moto
                            VehicleType.ATV -> R.drawable.bg_vehicle_placeholder_atv
                        }
                        binding.viewDashboardPlaceholder.setBackgroundResource(placeholderBg)

                        if (binding.etUpdateKm.text.isNullOrEmpty()) {
                            binding.etUpdateKm.setText(vehicle.currentKm.toString())
                        }

                        // Vehicle image (downsampled to save memory)
                        val imagePath = vehicle.imagePath
                        if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
                            val bmp = withContext(Dispatchers.IO) {
                                decodeSampledBitmap(imagePath, 800, 400)
                            }
                            if (bmp != null) {
                                binding.ivDashboardVehicleImage.setImageBitmap(bmp)
                                binding.ivDashboardVehicleImage.visibility = View.VISIBLE
                                binding.viewHeaderGradient.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                // Health score
                launch {
                    viewModel.healthScore.collect { score ->
                        binding.tvHealthValue.text = score.toString()
                        val colorRes = when {
                            score >= 70 -> R.color.score_good
                            score >= 40 -> R.color.score_fair
                            else -> R.color.score_poor
                        }
                        binding.tvHealthValue.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
                    }
                }

                // Cost summary
                launch {
                    viewModel.costSummary.collect { summary ->
                        summary ?: return@collect
                        binding.tvCostKmValue.text = "€${"%.2f".format(summary.totalCostPerKm)}"
                        binding.tvFuelTotalValue.text = "€${"%.0f".format(summary.totalFuelCost)}"
                        binding.tvMonthlyValue.text = "€${"%.0f".format(summary.monthlyAvgSpend)}"
                    }
                }

                // Insights
                launch {
                    viewModel.insights.collect { insights ->
                        if (insights.isEmpty()) {
                            binding.layoutInsights.visibility = View.GONE
                        } else {
                            binding.layoutInsights.visibility = View.VISIBLE
                            insightAdapter.submitList(insights)
                        }
                    }
                }

                // Timeline
                launch {
                    viewModel.timeline.collect { events ->
                        binding.tvTimelineEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvTimeline.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
                        timelineAdapter.submitList(events)
                    }
                }
            }
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): android.graphics.Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        val (h, w) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (h > reqH || w > reqW) {
            val halfH = h / 2; val halfW = w / 2
            while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) inSampleSize *= 2
        }
        return inSampleSize
    }

    private suspend fun exportVehiclePdf() {
        val file = viewModel.exportVehiclePdf() ?: return
        sharePdf(file)
    }

    private fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.pdf_share_title)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
