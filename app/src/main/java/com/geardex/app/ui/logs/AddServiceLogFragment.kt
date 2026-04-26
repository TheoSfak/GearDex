package com.geardex.app.ui.logs

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.databinding.FragmentAddServiceLogBinding
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class AddServiceLogFragment : Fragment() {

    private var _binding: FragmentAddServiceLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogsViewModel by viewModels()
    private var selectedVehicleType: VehicleType = VehicleType.CAR
    private var cameraImageUri: Uri? = null
    private var scannedReceiptDate: Long? = null

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Snackbar.make(binding.root, getString(R.string.camera_permission_denied), Snackbar.LENGTH_SHORT).show()
    }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { runOcr(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddServiceLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnScanReceipt.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        // Show/hide vehicle-type specific fields based on selected vehicle
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vehicles.collect { vehicles ->
                    val id = viewModel.selectedVehicleId.value
                    val vehicle = vehicles.find { it.id == id }
                    if (vehicle != null) {
                        selectedVehicleType = vehicle.type
                        updateConditionalFields(vehicle.type)
                    }
                }
            }
        }

        binding.btnSaveService.setOnClickListener {
            val vehicleId = viewModel.selectedVehicleId.value
            if (vehicleId < 0) return@setOnClickListener

            val km = binding.etServiceKm.text?.toString()?.toIntOrNull()
            val cost = binding.etServiceCost.text?.toString()?.toDoubleOrNull()

            if (km == null) { binding.tilServiceKm.error = getString(R.string.error_invalid_number); return@setOnClickListener }
            if (cost == null) { binding.tilServiceCost.error = getString(R.string.error_invalid_number); return@setOnClickListener }

            val checks = mapOf(
                "oilChange" to binding.cbOilChange.isChecked,
                "airFilter" to binding.cbAirFilter.isChecked,
                "brakePads" to binding.cbBrakePads.isChecked,
                "timingBelt" to binding.cbTimingBelt.isChecked,
                "cabinFilter" to binding.cbCabinFilter.isChecked,
                "chainLube" to binding.cbChainLube.isChecked,
                "valveClearance" to binding.cbValveClearance.isChecked,
                "forkOil" to binding.cbForkOil.isChecked,
                "tireCheck" to binding.cbTireCheck.isChecked
            )

            viewModel.addServiceLog(
                vehicleId, km, cost, scannedReceiptDate ?: System.currentTimeMillis(),
                binding.etMechanic.text?.toString() ?: "",
                binding.etServiceNotes.text?.toString() ?: "",
                selectedVehicleType, checks
            )
            findNavController().popBackStack()
        }
    }

    private fun updateConditionalFields(type: VehicleType) {
        binding.sectionCar.visibility = if (type == VehicleType.CAR) View.VISIBLE else View.GONE
        binding.sectionMoto.visibility = if (type == VehicleType.MOTORCYCLE || type == VehicleType.ATV) View.VISIBLE else View.GONE
    }

    private fun launchCamera() {
        val tmpFile = File(requireContext().cacheDir, "service_receipt_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            tmpFile
        )
        cameraImageUri = uri
        takePicture.launch(uri)
    }

    private fun runOcr(uri: Uri) {
        binding.progressOcr.visibility = View.VISIBLE
        binding.btnScanReceipt.isEnabled = false

        lifecycleScope.launch {
            try {
                val rawText = withContext(Dispatchers.IO) {
                    val bitmap = decodeSampledBitmap(uri, 1600, 1600)
                        ?: throw IllegalStateException("Cannot decode image")
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(image).await().text
                }
                val result = OcrReceiptParser.parse(rawText)
                val filled = applyOcrResult(result)
                val msg = if (filled) getString(R.string.log_ocr_success) else getString(R.string.log_ocr_no_data)
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Snackbar.make(binding.root, getString(R.string.log_ocr_no_data), Snackbar.LENGTH_SHORT).show()
            } finally {
                binding.progressOcr.visibility = View.GONE
                binding.btnScanReceipt.isEnabled = true
            }
        }
    }

    private fun decodeSampledBitmap(uri: Uri, reqWidth: Int, reqHeight: Int): android.graphics.Bitmap? {
        val resolver = requireContext().contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        val (w, h) = options.outWidth to options.outHeight
        var size = 1
        if (h > reqH || w > reqW) {
            val halfH = h / 2
            val halfW = w / 2
            while (halfH / size >= reqH && halfW / size >= reqW) size *= 2
        }
        return size
    }

    private fun applyOcrResult(result: OcrResult): Boolean {
        var any = false
        result.odometer?.let { binding.etServiceKm.setText(it.toString()); any = true }
        result.cost?.let { binding.etServiceCost.setText(String.format(Locale.US, "%.2f", it)); any = true }
        result.merchantName?.let {
            if (binding.etMechanic.text.isNullOrBlank()) {
                binding.etMechanic.setText(it)
                any = true
            }
        }
        result.date?.let { scannedReceiptDate = it; any = true }
        applyServiceChecks(result.serviceChecks).also { if (it) any = true }
        showScanSummary(result)
        return any
    }

    private fun applyServiceChecks(checks: Set<String>): Boolean {
        var any = false
        fun setCheck(key: String, update: () -> Unit) {
            if (key in checks) {
                update()
                any = true
            }
        }
        setCheck("oilChange") { binding.cbOilChange.isChecked = true }
        setCheck("airFilter") { binding.cbAirFilter.isChecked = true }
        setCheck("brakePads") { binding.cbBrakePads.isChecked = true }
        setCheck("timingBelt") { binding.cbTimingBelt.isChecked = true }
        setCheck("cabinFilter") { binding.cbCabinFilter.isChecked = true }
        setCheck("chainLube") { binding.cbChainLube.isChecked = true }
        setCheck("valveClearance") { binding.cbValveClearance.isChecked = true }
        setCheck("forkOil") { binding.cbForkOil.isChecked = true }
        setCheck("tireCheck") { binding.cbTireCheck.isChecked = true }
        return any
    }

    private fun showScanSummary(result: OcrResult) {
        val parts = buildList {
            result.date?.let { add(getString(R.string.log_ocr_summary_date, formatDate(it))) }
            result.merchantName?.let { add(getString(R.string.log_ocr_summary_shop, it)) }
            if (result.serviceChecks.isNotEmpty()) {
                add(
                    resources.getQuantityString(
                        R.plurals.log_ocr_summary_checks,
                        result.serviceChecks.size,
                        result.serviceChecks.size
                    )
                )
            }
        }
        binding.tvReceiptScanSummary.text = parts.joinToString(" · ")
        binding.tvReceiptScanSummary.visibility = if (parts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
