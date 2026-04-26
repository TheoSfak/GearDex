package com.geardex.app.ui.parking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.ParkingSpot
import com.geardex.app.databinding.FragmentParkingBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class ParkingFragment : Fragment() {

    private var _binding: FragmentParkingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ParkingViewModel by viewModels()
    private lateinit var adapter: ParkingSpotAdapter

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            viewModel.fetchCurrentLocation(requireContext())
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.parking_location_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = ParkingSpotAdapter(
            onOpenMap = { spot -> openInMaps(spot) },
            onShare = { spot -> shareLocation(spot) },
            onDelete = { spot -> viewModel.deleteSpot(spot) }
        )
        binding.rvParkingHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ParkingFragment.adapter
        }
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val spot = adapter.currentList.getOrNull(vh.adapterPosition) ?: return
                viewModel.deleteSpot(spot)
                Snackbar.make(binding.root, R.string.parking_deleted, Snackbar.LENGTH_SHORT).show()
            }
        }).attachToRecyclerView(binding.rvParkingHistory)
    }

    private fun setupButtons() {
        binding.btnSaveCurrent.setOnClickListener {
            val address = binding.etParkingAddress.text?.toString()?.trim() ?: ""
            val notes = binding.etParkingNotes.text?.toString()?.trim() ?: ""
            viewModel.saveCurrent(address, notes)
            binding.etParkingAddress.setText("")
            binding.etParkingNotes.setText("")
            Snackbar.make(binding.root, R.string.parking_saved, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnGetLocation.setOnClickListener { requestLocationAndFetch() }
        binding.btnSetTimer.setOnClickListener { showTimePicker() }
    }

    private fun requestLocationAndFetch() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            viewModel.fetchCurrentLocation(requireContext())
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(cal.get(Calendar.HOUR_OF_DAY))
            .setMinute(cal.get(Calendar.MINUTE))
            .setTitleText(getString(R.string.parking_set_timer_title))
            .build()
        picker.addOnPositiveButtonClickListener {
            viewModel.setMeterExpiry(picker.hour, picker.minute)
            binding.tvTimerStatus.text =
                getString(R.string.parking_timer_set, "%02d:%02d".format(picker.hour, picker.minute))
            binding.tvTimerStatus.visibility = View.VISIBLE
        }
        picker.show(childFragmentManager, "time_picker")
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.spots.collect { spots ->
                        adapter.submitList(spots)
                        binding.tvEmptyParking.visibility =
                            if (spots.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvParkingHistory.visibility =
                            if (spots.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.detectedAddress.collect { address ->
                        if (address.isNotBlank()) binding.etParkingAddress.setText(address)
                    }
                }
                launch {
                    viewModel.currentLat.collect { lat ->
                        if (lat != 0.0) {
                            binding.tvLocationStatus.text =
                                getString(R.string.parking_location_acquired)
                            binding.tvLocationStatus.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun openInMaps(spot: ParkingSpot) {
        val uri = if (spot.latitude != 0.0 || spot.longitude != 0.0) {
            val label = Uri.encode(
                spot.address.ifBlank { getString(R.string.parking_spot_label) }
            )
            "geo:${spot.latitude},${spot.longitude}?q=${spot.latitude},${spot.longitude}($label)".toUri()
        } else {
            "geo:0,0?q=${Uri.encode(spot.address)}".toUri()
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_VIEW, uri),
                getString(R.string.parking_open_map)
            )
        )
    }

    private fun shareLocation(spot: ParkingSpot) {
        val text = buildString {
            append(getString(R.string.parking_share_prefix))
            if (spot.address.isNotBlank()) append(" ${spot.address}")
            if (spot.latitude != 0.0) {
                append("\nhttps://maps.google.com/?q=${spot.latitude},${spot.longitude}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.parking_share_title)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
