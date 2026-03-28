package com.geardex.app.ui.garage

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.databinding.FragmentAddVehicleBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AddVehicleFragment : Fragment() {

    private var _binding: FragmentAddVehicleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GarageViewModel by viewModels()
    private var selectedType: VehicleType = VehicleType.CAR
    private var selectedImagePath: String? = null
    private var cameraUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { copyImageToInternal(it) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraUri?.let { uri ->
                selectedImagePath = uri.path ?: getFileFromUri(uri)
                showSelectedImage(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Default selection
        binding.toggleVehicleType.check(R.id.btn_type_car)

        binding.toggleVehicleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedType = when (checkedId) {
                    R.id.btn_type_car -> VehicleType.CAR
                    R.id.btn_type_motorcycle -> VehicleType.MOTORCYCLE
                    R.id.btn_type_atv -> VehicleType.ATV
                    else -> VehicleType.CAR
                }
            }
        }

        binding.btnPickImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnTakePhoto.setOnClickListener {
            val file = File(requireContext().filesDir, "vehicle_images")
            file.mkdirs()
            val imageFile = File(file, "vehicle_${System.currentTimeMillis()}.jpg")
            cameraUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                imageFile
            )
            selectedImagePath = imageFile.absolutePath
            takePhoto.launch(cameraUri)
        }

        binding.btnRemoveImage.setOnClickListener {
            selectedImagePath = null
            binding.ivVehiclePhoto.setImageResource(R.drawable.ic_car)
            binding.ivVehiclePhoto.scaleType = android.widget.ImageView.ScaleType.CENTER
            binding.btnRemoveImage.visibility = View.GONE
        }

        binding.btnSaveVehicle.setOnClickListener {
            if (validateAndSave()) {
                findNavController().popBackStack()
            }
        }
    }

    private fun copyImageToInternal(uri: Uri) {
        val dir = File(requireContext().filesDir, "vehicle_images")
        dir.mkdirs()
        val dest = File(dir, "vehicle_${System.currentTimeMillis()}.jpg")
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        selectedImagePath = dest.absolutePath
        showSelectedImage(Uri.fromFile(dest))
    }

    private fun showSelectedImage(uri: Uri) {
        binding.ivVehiclePhoto.setImageURI(uri)
        binding.ivVehiclePhoto.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        binding.btnRemoveImage.visibility = View.VISIBLE
    }

    private fun getFileFromUri(uri: Uri): String {
        return uri.lastPathSegment ?: ""
    }

    private fun validateAndSave(): Boolean {
        val make = binding.etMake.text?.toString()?.trim() ?: ""
        val model = binding.etModel.text?.toString()?.trim() ?: ""
        val yearStr = binding.etYear.text?.toString()?.trim() ?: ""
        val plate = binding.etPlate.text?.toString()?.trim() ?: ""
        val kmStr = binding.etKm.text?.toString()?.trim() ?: ""

        if (make.isEmpty()) { binding.tilMake.error = getString(R.string.error_required_field); return false }
        else binding.tilMake.error = null

        if (model.isEmpty()) { binding.tilModel.error = getString(R.string.error_required_field); return false }
        else binding.tilModel.error = null

        val year = yearStr.toIntOrNull()
        if (year == null || year < 1900 || year > 2100) {
            binding.tilYear.error = getString(R.string.error_invalid_number); return false
        } else binding.tilYear.error = null

        if (plate.isEmpty()) { binding.tilPlate.error = getString(R.string.error_required_field); return false }
        else binding.tilPlate.error = null

        val km = kmStr.toIntOrNull()
        if (km == null || km < 0) {
            binding.tilKm.error = getString(R.string.error_invalid_number); return false
        } else binding.tilKm.error = null

        viewModel.addVehicle(selectedType, make, model, year, plate, km, selectedImagePath)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
