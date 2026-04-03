package com.geardex.app.ui.garage

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.databinding.FragmentDriveModeBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DriveModeFragment : Fragment() {

    private var _binding: FragmentDriveModeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DriveModeViewModel by viewModels()
    private val sessionAdapter = DriveSessionAdapter()
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDriveModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSessions.adapter = sessionAdapter

        binding.btnStartStop.setOnClickListener {
            if (viewModel.isDriving.value) {
                viewModel.stopDrive()
                stopTimer()
                binding.cardEndInputs.visibility = View.VISIBLE
                binding.btnStartStop.text = getString(R.string.drive_start)
                binding.btnStartStop.setIconResource(R.drawable.ic_play)
            } else {
                if (viewModel.selectedVehicleId.value <= 0) {
                    Snackbar.make(binding.root, getString(R.string.drive_select_vehicle), Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.startDrive()
                startTimer()
                binding.cardEndInputs.visibility = View.GONE
                binding.btnStartStop.text = getString(R.string.drive_stop)
                binding.btnStartStop.setIconResource(R.drawable.ic_stop)
            }
        }

        binding.btnSaveSession.setOnClickListener {
            val distance = binding.etDistance.text?.toString()?.toDoubleOrNull() ?: 0.0
            val notes = binding.etNotes.text?.toString()?.trim() ?: ""
            viewModel.saveSession(distance, notes)
            binding.cardEndInputs.visibility = View.GONE
            binding.etDistance.text?.clear()
            binding.etNotes.text?.clear()
            binding.tvDuration.text = "00:00"
            binding.tvDistance.text = "0.0 km"
            binding.tvAvgSpeed.text = "0 km/h"
            Snackbar.make(binding.root, getString(R.string.drive_session_saved), Snackbar.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicles.collect { vehicles ->
                        val names = vehicles.map { "${it.make} ${it.model}" }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                        binding.spinnerVehicle.setAdapter(adapter)
                        binding.spinnerVehicle.setOnItemClickListener { _, _, pos, _ ->
                            if (vehicles.isNotEmpty()) viewModel.selectVehicle(vehicles[pos].id)
                        }
                        if (vehicles.isNotEmpty() && viewModel.selectedVehicleId.value <= 0) {
                            binding.spinnerVehicle.setText(names[0], false)
                            viewModel.selectVehicle(vehicles[0].id)
                        }
                    }
                }
                launch {
                    viewModel.sessions.collect { sessions ->
                        sessionAdapter.submitList(sessions)
                        binding.tvDriveEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerSessions.visibility = if (sessions.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                val elapsed = System.currentTimeMillis() - viewModel.startTime.value
                val totalSeconds = elapsed / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val hours = minutes / 60
                binding.tvDuration.text = if (hours > 0) {
                    "%d:%02d:%02d".format(hours, minutes % 60, seconds)
                } else {
                    "%02d:%02d".format(minutes, seconds)
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    override fun onDestroyView() {
        stopTimer()
        super.onDestroyView()
        _binding = null
    }
}
