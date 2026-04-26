package com.geardex.app.ui.ekdromes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.data.model.EkdromeDifficulty
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeTag
import com.geardex.app.databinding.FragmentSuggestRouteBinding
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class SuggestRouteFragment : Fragment() {

    private var _binding: FragmentSuggestRouteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkdromesViewModel by viewModels()
    private val waypointFields = mutableListOf<TextInputEditText>()

    private val isGreek: Boolean
        get() = Locale.getDefault().language == "el"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSuggestRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Region dropdown
        val regions = EkdromeRegion.entries.filter { it != EkdromeRegion.ALL }
        val regionNames = regions.map { if (isGreek) it.displayEl else it.displayEn }
        binding.dropdownRegion.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, regionNames)
        )

        // Difficulty dropdown
        val difficulties = EkdromeDifficulty.entries.toList()
        val diffNames = difficulties.map { if (isGreek) it.displayEl else it.displayEn }
        binding.dropdownDifficulty.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, diffNames)
        )

        // Tag chips
        EkdromeTag.entries.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = if (isGreek) tag.displayEl else tag.displayEn
                isCheckable = true
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
                this.tag = tag
            }
            binding.chipGroupTags.addView(chip)
        }

        // Waypoint management
        binding.btnAddWaypoint.setOnClickListener {
            addWaypointField()
        }

        // Submit
        binding.btnSubmit.setOnClickListener { submitRoute(regions, regionNames, difficulties, diffNames) }

        // Observe submit result
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.submitResult.collect { result ->
                    if (result == null) return@collect
                    val msg = if (result) getString(R.string.community_submit_success)
                    else getString(R.string.community_submit_error)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearSubmitResult()
                    if (result) findNavController().popBackStack()
                }
            }
        }
    }

    private fun submitRoute(
        regions: List<EkdromeRegion>,
        regionNames: List<String>,
        difficulties: List<EkdromeDifficulty>,
        diffNames: List<String>
    ) {
        val nameEn = binding.etNameEn.text?.toString()?.trim() ?: ""
        val nameEl = binding.etNameEl.text?.toString()?.trim() ?: ""
        val distStr = binding.etDistance.text?.toString()?.trim() ?: "0"
        val descEn = binding.etDescEn.text?.toString()?.trim() ?: ""
        val descEl = binding.etDescEl.text?.toString()?.trim() ?: ""
        val latStr = binding.etLatitude.text?.toString()?.trim() ?: "0"
        val lngStr = binding.etLongitude.text?.toString()?.trim() ?: "0"
        val startLoc = binding.etStartLocation.text?.toString()?.trim() ?: ""
        val endLoc = binding.etEndLocation.text?.toString()?.trim() ?: ""

        if (nameEn.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.community_name_required), Toast.LENGTH_SHORT).show()
            return
        }

        val regionIdx = regionNames.indexOf(binding.dropdownRegion.text.toString())
        val region = if (regionIdx >= 0) regions[regionIdx] else EkdromeRegion.CRETE

        val diffIdx = diffNames.indexOf(binding.dropdownDifficulty.text.toString())
        val difficulty = if (diffIdx >= 0) difficulties[diffIdx] else EkdromeDifficulty.MEDIUM

        val selectedTags = mutableListOf<EkdromeTag>()
        for (i in 0 until binding.chipGroupTags.childCount) {
            val chip = binding.chipGroupTags.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                (chip.tag as? EkdromeTag)?.let { selectedTags.add(it) }
            }
        }

        val waypoints = waypointFields.mapNotNull {
            it.text?.toString()?.trim()?.takeIf { wp -> wp.isNotEmpty() }
        }

        viewModel.submitRoute(
            nameEn = nameEn,
            nameEl = nameEl.ifEmpty { nameEn },
            region = region,
            tags = selectedTags,
            difficulty = difficulty,
            distanceKm = distStr.toIntOrNull() ?: 0,
            descriptionEn = descEn,
            descriptionEl = descEl.ifEmpty { descEn },
            latitude = latStr.toDoubleOrNull() ?: 0.0,
            longitude = lngStr.toDoubleOrNull() ?: 0.0,
            startLocation = startLoc,
            endLocation = endLoc,
            waypoints = waypoints
        )
    }

    private fun addWaypointField() {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4 }
        }

        val til = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = getString(R.string.community_waypoint_hint, waypointFields.size + 1)
        }
        val et = TextInputEditText(til.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            maxLines = 1
        }
        til.addView(et)
        row.addView(til)

        val btnRemove = com.google.android.material.button.MaterialButton(
            requireContext(), null, com.google.android.material.R.attr.materialIconButtonStyle
        ).apply {
            text = "✕"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        btnRemove.setOnClickListener {
            binding.layoutWaypoints.removeView(row)
            waypointFields.remove(et)
        }
        row.addView(btnRemove)

        binding.layoutWaypoints.addView(row)
        waypointFields.add(et)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
