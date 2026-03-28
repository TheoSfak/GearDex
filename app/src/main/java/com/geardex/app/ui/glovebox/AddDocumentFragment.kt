package com.geardex.app.ui.glovebox

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.databinding.FragmentAddDocumentBinding
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddDocumentFragment : Fragment() {

    private var _binding: FragmentAddDocumentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GloveboxViewModel by viewModels()
    private var pickedFileUri: Uri? = null
    private var pickedFileName: String = ""
    private var selectedExpiryDate: Long? = null
    private var cameraImageUri: Uri? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val documentTypes = listOf(
        DocumentType.KTEO, DocumentType.INSURANCE,
        DocumentType.ROAD_TAX, DocumentType.RECEIPT, DocumentType.OTHER
    )

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pickedFileUri = it
            pickedFileName = getFileName(it)
            binding.tvPickedFile.text = pickedFileName
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                pickedFileUri = uri
                pickedFileName = "photo_${System.currentTimeMillis()}.jpg"
                binding.tvPickedFile.text = pickedFileName
            }
        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(), getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Document type spinner
        val typeLabels = listOf(
            getString(R.string.doc_type_kteo), getString(R.string.doc_type_insurance),
            getString(R.string.doc_type_road_tax), getString(R.string.doc_type_receipt),
            getString(R.string.doc_type_other)
        )
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeLabels)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDocType.adapter = typeAdapter

        binding.btnPickFile.setOnClickListener {
            pickFileLauncher.launch("*/*")
        }

        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnPickExpiry.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.doc_expiry_date))
                .build()
            picker.addOnPositiveButtonClickListener { ms ->
                selectedExpiryDate = ms
                binding.tvExpiryDate.text = dateFormat.format(Date(ms))
            }
            picker.show(parentFragmentManager, "expiry_picker")
        }

        binding.btnSaveDocument.setOnClickListener {
            val uri = pickedFileUri
            if (uri == null) {
                binding.tvPickedFile.error = getString(R.string.error_required_field)
                return@setOnClickListener
            }
            val vehicles = viewModel.vehicles.value
            if (vehicles.isEmpty()) return@setOnClickListener

            val selectedVehiclePos = binding.spinnerVehicle.selectedItemPosition
            val vehicleId = vehicles.getOrNull(selectedVehiclePos)?.id ?: return@setOnClickListener
            val docType = documentTypes[binding.spinnerDocType.selectedItemPosition]

            val stream = requireContext().contentResolver.openInputStream(uri) ?: return@setOnClickListener
            viewModel.saveDocument(vehicleId, stream, pickedFileName, docType, selectedExpiryDate)
            findNavController().popBackStack()
        }

        // Vehicle spinner
        val vehicles = viewModel.vehicles.value
        val vehicleNames = vehicles.map { "${it.make} ${it.model}" }
        val vehicleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehicleNames)
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVehicle.adapter = vehicleAdapter
    }

    private fun launchCamera() {
        val photoFile = File(requireContext().cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        cameraImageUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun getFileName(uri: Uri): String {
        var name = "document_${System.currentTimeMillis()}"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx)
            }
        }
        return name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

