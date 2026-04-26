package com.geardex.app.ui.garage

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.geardex.app.R
import com.geardex.app.data.local.entity.ParkingSpot
import com.geardex.app.databinding.FragmentGarageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        binding.btnParkingSave.setOnClickListener {
            findNavController().navigate(R.id.parkingFragment)
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
                    viewModel.latestParkingSpot.collect { spot ->
                        bindLatestParkingSpot(spot)
                    }
                }
            }
        }
    }

    private fun bindLatestParkingSpot(spot: ParkingSpot?) {
        if (spot == null) {
            binding.tvLastParkingMeta.text = getString(R.string.parking_last_empty_title)
            binding.tvLastParkingAddress.text = getString(R.string.parking_last_empty_body)
            binding.tvLastParkingNotes.visibility = View.GONE
            binding.btnParkingMap.visibility = View.GONE
            binding.btnParkingShare.visibility = View.GONE
            return
        }

        val dateFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        binding.tvLastParkingMeta.text =
            getString(R.string.parking_last_saved_at, dateFmt.format(Date(spot.savedAt)))
        binding.tvLastParkingAddress.text =
            spot.address.ifBlank { getString(R.string.parking_no_address) }
        binding.tvLastParkingNotes.text = spot.notes
        binding.tvLastParkingNotes.visibility = if (spot.notes.isBlank()) View.GONE else View.VISIBLE
        binding.btnParkingMap.visibility = View.VISIBLE
        binding.btnParkingShare.visibility = View.VISIBLE
        binding.btnParkingMap.setOnClickListener { openInMaps(spot) }
        binding.btnParkingShare.setOnClickListener { shareLocation(spot) }
    }

    private fun openInMaps(spot: ParkingSpot) {
        val uri = if (spot.latitude != 0.0 || spot.longitude != 0.0) {
            val label = Uri.encode(spot.address.ifBlank { getString(R.string.parking_spot_label) })
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
