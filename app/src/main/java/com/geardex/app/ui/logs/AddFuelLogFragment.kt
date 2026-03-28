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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.geardex.app.R
import com.geardex.app.databinding.FragmentAddFuelLogBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class AddFuelLogFragment : Fragment() {

    private var _binding: FragmentAddFuelLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogsViewModel by viewModels()

    private var cameraImageUri: Uri? = null

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
        _binding = FragmentAddFuelLogBinding.inflate(inflater, container, false)
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

        binding.btnSaveFuel.setOnClickListener {
            val vehicleId = viewModel.selectedVehicleId.value
            if (vehicleId < 0) return@setOnClickListener

            val odometer = binding.etOdometer.text?.toString()?.toIntOrNull()
            val liters = binding.etLiters.text?.toString()?.toDoubleOrNull()
            val cost = binding.etCost.text?.toString()?.toDoubleOrNull()
            val notes = binding.etNotes.text?.toString() ?: ""

            if (odometer == null) { binding.tilOdometer.error = getString(R.string.error_invalid_number); return@setOnClickListener }
            if (liters == null) { binding.tilLiters.error = getString(R.string.error_invalid_number); return@setOnClickListener }
            if (cost == null) { binding.tilCost.error = getString(R.string.error_invalid_number); return@setOnClickListener }

            viewModel.addFuelLog(vehicleId, odometer, liters, cost, System.currentTimeMillis(), notes)
            findNavController().popBackStack()
        }
    }

    private fun launchCamera() {
        val tmpFile = File(requireContext().cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
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
                    val bitmap = BitmapFactory.decodeStream(
                        requireContext().contentResolver.openInputStream(uri)
                    )
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

    private fun applyOcrResult(result: OcrResult): Boolean {
        var any = false
        result.odometer?.let { binding.etOdometer.setText(it.toString()); any = true }
        result.liters?.let { binding.etLiters.setText("%.2f".format(it)); any = true }
        result.cost?.let { binding.etCost.setText("%.2f".format(it)); any = true }
        return any
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

