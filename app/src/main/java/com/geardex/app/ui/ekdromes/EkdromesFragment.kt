package com.geardex.app.ui.ekdromes

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeTag
import com.geardex.app.databinding.FragmentEkdromesBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EkdromesFragment : Fragment() {

    private var _binding: FragmentEkdromesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkdromesViewModel by viewModels()
    private val adapter = EkdromeAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEkdromesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerEkdromes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@EkdromesFragment.adapter
        }

        setupRegionChips()
        setupTagChips()

        binding.fabSuggestEkdrome.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("ekdromes@geardex.app"))
                putExtra(Intent.EXTRA_SUBJECT, "Suggest an Ekdrome")
                putExtra(Intent.EXTRA_TEXT, "Route name:\nRegion:\nDistance (km):\nDifficulty:\nDescription:\n")
            }
            startActivity(Intent.createChooser(intent, getString(R.string.ekdromes_suggest)))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredRoutes.collect { routes ->
                    adapter.submitList(routes)
                }
            }
        }
    }

    private fun setupRegionChips() {
        binding.chipGroupRegion.removeAllViews()
        val regions = EkdromeRegion.values()
        val isGreek = java.util.Locale.getDefault().language == "el"
        regions.forEach { region ->
            val chip = Chip(requireContext()).apply {
                text = if (isGreek) region.displayEl else region.displayEn
                isCheckable = true
                isChecked = region == EkdromeRegion.ALL
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
            }
            chip.setOnClickListener {
                viewModel.selectRegion(region)
            }
            binding.chipGroupRegion.addView(chip)
        }
    }

    private fun setupTagChips() {
        binding.chipGroupTags.removeAllViews()
        val tags = EkdromeTag.values()
        val isGreek = java.util.Locale.getDefault().language == "el"
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = if (isGreek) tag.displayEl else tag.displayEn
                isCheckable = true
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
            }
            chip.setOnClickListener {
                val isChecked = chip.isChecked
                viewModel.selectTag(if (isChecked) tag else null)
            }
            binding.chipGroupTags.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
