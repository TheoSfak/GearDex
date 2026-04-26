package com.geardex.app.ui.ekdromes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.animation.AnimationUtils
import com.geardex.app.R
import com.geardex.app.data.model.EkdromeDifficulty
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.data.model.EkdromeTag
import com.geardex.app.databinding.FragmentEkdromesBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EkdromesFragment : Fragment() {

    private var _binding: FragmentEkdromesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkdromesViewModel by viewModels()
    private val adapter = EkdromeAdapter(
        onReviewClick = { route -> showReviewsBottomSheet(route) },
        onCalculateCostClick = { route -> showTripCostDialog(route) },
        onItemClick = { route -> navigateToDetail(route) },
        onBookmarkClick = { route -> viewModel.toggleSaveRoute(route) }
    )

    private val isGreek: Boolean
        get() = java.util.Locale.getDefault().language == "el"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEkdromesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerEkdromes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@EkdromesFragment.adapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_slide_up)
        }

        setupRegionChips()
        setupTagChips()
        setupTabs()

        binding.fabSuggestEkdrome.setOnClickListener {
            findNavController().navigate(R.id.action_ekdromes_to_suggestRoute)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredRoutes.collect { routes ->
                    adapter.submitList(routes)
                    val currentTab = viewModel.selectedTab.value
                    val isEmpty = routes.isEmpty() && currentTab != RouteTab.BUILTIN
                    binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.recyclerEkdromes.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    if (isEmpty && currentTab == RouteTab.SAVED) {
                        binding.tvEmptyState.text = getString(R.string.saved_routes_empty)
                    } else if (isEmpty) {
                        binding.tvEmptyState.text = getString(R.string.community_empty)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.savedRouteKeys.collect { keys ->
                    adapter.updateSavedKeys(keys)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.submitResult.collect { result ->
                    if (result == null) return@collect
                    val msg = if (result) getString(R.string.community_submit_success)
                    else getString(R.string.community_submit_error)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearSubmitResult()
                    if (result) viewModel.selectTab(RouteTab.COMMUNITY)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reviewSubmitResult.collect { result ->
                    if (result == null) return@collect
                    val msg = if (result) getString(R.string.review_submit_success)
                    else getString(R.string.review_submit_error)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearReviewSubmitResult()
                }
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayoutRoutes.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val routeTab = when (tab.position) {
                    0 -> RouteTab.BUILTIN
                    1 -> RouteTab.COMMUNITY
                    2 -> RouteTab.SAVED
                    else -> RouteTab.BUILTIN
                }
                viewModel.selectTab(routeTab)
                updateFabForTab(routeTab)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun updateFabForTab(tab: RouteTab) {
        when (tab) {
            RouteTab.SAVED -> binding.fabSuggestEkdrome.hide()
            else -> {
                binding.fabSuggestEkdrome.show()
                binding.fabSuggestEkdrome.text = getString(R.string.community_submit_route)
                binding.fabSuggestEkdrome.setIconResource(R.drawable.ic_nav_ekdromes)
            }
        }
    }

    private fun showSubmitDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_submit_route, null)

        // Region dropdown
        val regions = EkdromeRegion.values().filter { it != EkdromeRegion.ALL }
        val regionNames = regions.map { if (isGreek) it.displayEl else it.displayEn }
        val regionDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.dropdown_region)
        regionDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, regionNames))

        // Difficulty dropdown
        val difficulties = EkdromeDifficulty.values().toList()
        val diffNames = difficulties.map { if (isGreek) it.displayEl else it.displayEn }
        val diffDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.dropdown_difficulty)
        diffDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, diffNames))

        // Tag chips
        val chipGroup = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_tags)
        val allTags = EkdromeTag.values()
        allTags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = if (isGreek) tag.displayEl else tag.displayEn
                isCheckable = true
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
            }
            chip.tag = tag
            chipGroup.addView(chip)
        }

        // Waypoints dynamic section
        val waypointsLayout = dialogView.findViewById<LinearLayout>(R.id.layout_waypoints)
        val btnAddWaypoint = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_waypoint)
        val waypointFields = mutableListOf<TextInputEditText>()

        btnAddWaypoint.setOnClickListener {
            addWaypointField(waypointsLayout, waypointFields)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.community_submit_route))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.community_submit)) { dialog, _ ->
                val nameEn = dialogView.findViewById<TextInputEditText>(R.id.et_name_en).text?.toString()?.trim() ?: ""
                val nameEl = dialogView.findViewById<TextInputEditText>(R.id.et_name_el).text?.toString()?.trim() ?: ""
                val distStr = dialogView.findViewById<TextInputEditText>(R.id.et_distance).text?.toString()?.trim() ?: "0"
                val descEn = dialogView.findViewById<TextInputEditText>(R.id.et_desc_en).text?.toString()?.trim() ?: ""
                val descEl = dialogView.findViewById<TextInputEditText>(R.id.et_desc_el).text?.toString()?.trim() ?: ""
                val latStr = dialogView.findViewById<TextInputEditText>(R.id.et_latitude).text?.toString()?.trim() ?: "0"
                val lngStr = dialogView.findViewById<TextInputEditText>(R.id.et_longitude).text?.toString()?.trim() ?: "0"
                val startLoc = dialogView.findViewById<TextInputEditText>(R.id.et_start_location).text?.toString()?.trim() ?: ""
                val endLoc = dialogView.findViewById<TextInputEditText>(R.id.et_end_location).text?.toString()?.trim() ?: ""

                val regionIdx = regionNames.indexOf(regionDropdown.text.toString())
                val region = if (regionIdx >= 0) regions[regionIdx] else EkdromeRegion.CRETE

                val diffIdx = diffNames.indexOf(diffDropdown.text.toString())
                val difficulty = if (diffIdx >= 0) difficulties[diffIdx] else EkdromeDifficulty.MEDIUM

                val selectedTags = mutableListOf<EkdromeTag>()
                for (i in 0 until chipGroup.childCount) {
                    val chip = chipGroup.getChildAt(i) as? Chip
                    if (chip?.isChecked == true) {
                        (chip.tag as? EkdromeTag)?.let { selectedTags.add(it) }
                    }
                }

                val waypoints = waypointFields.mapNotNull { it.text?.toString()?.trim()?.takeIf { wp -> wp.isNotEmpty() } }

                if (nameEn.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.community_name_required), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
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
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.community_cancel), null)
            .show()
    }

    private fun addWaypointField(container: LinearLayout, fieldList: MutableList<TextInputEditText>) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4 }
        }

        val til = TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = getString(R.string.community_waypoint_hint, fieldList.size + 1)
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
            container.removeView(row)
            fieldList.remove(et)
        }
        row.addView(btnRemove)

        container.addView(row)
        fieldList.add(et)
    }

    private fun showReviewsBottomSheet(route: EkdromeRoute) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetParent = android.widget.FrameLayout(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_reviews, sheetParent, false)
        bottomSheet.setContentView(sheetView)

        val routeId = viewModel.getRouteReviewId(route)
        val routeName = if (isGreek) route.nameEl else route.nameEn
        sheetView.findViewById<android.widget.TextView>(R.id.tv_reviews_title).text =
            getString(R.string.review_title_for, routeName)

        val recyclerReviews = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_reviews)
        val reviewAdapter = ReviewAdapter()
        recyclerReviews.layoutManager = LinearLayoutManager(requireContext())
        recyclerReviews.adapter = reviewAdapter

        val tvEmpty = sheetView.findViewById<android.widget.TextView>(R.id.tv_reviews_empty)
        val tvAvgRating = sheetView.findViewById<android.widget.TextView>(R.id.tv_average_rating)

        // Observe reviews — cancel collection when bottom sheet is dismissed
        val job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.observeReviews(routeId).collect { reviews ->
                reviewAdapter.submitList(reviews)
                tvEmpty.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
                recyclerReviews.visibility = if (reviews.isEmpty()) View.GONE else View.VISIBLE
                val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average().toFloat() else route.rating
                tvAvgRating.text = "%.1f".format(avgRating)
            }
        }
        bottomSheet.setOnDismissListener { job.cancel() }

        // Add review button
        sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_review).setOnClickListener {
            showRateDialog(routeId)
        }

        bottomSheet.show()
    }

    private fun showRateDialog(routeId: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_rate_route, null)

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val etComment = dialogView.findViewById<TextInputEditText>(R.id.et_comment)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.review_rate_route))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.community_submit)) { dialog, _ ->
                val rating = ratingBar.rating
                val comment = etComment.text?.toString()?.trim() ?: ""
                if (rating > 0) {
                    viewModel.submitReview(routeId, rating, comment)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.community_cancel), null)
            .show()
    }

    private fun setupRegionChips() {
        binding.chipGroupRegion.removeAllViews()
        val regions = EkdromeRegion.values()
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

    private fun showTripCostDialog(route: EkdromeRoute) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_trip_cost, null)

        val vehicles = viewModel.vehicles.value
        val vehicleNames = vehicles.map { "${it.make} ${it.model} (${it.year})" }
        val dropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.dropdown_vehicle)
        dropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, vehicleNames))

        val tvDistance = dialogView.findViewById<TextView>(R.id.tv_route_distance)
        val tvConsumptionInfo = dialogView.findViewById<TextView>(R.id.tv_consumption_info)
        val etConsumption = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_consumption)
        val etFuelPrice = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_fuel_price)
        val cardResult = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_result)
        val tvCostResult = dialogView.findViewById<TextView>(R.id.tv_cost_result)
        val tvBreakdown = dialogView.findViewById<TextView>(R.id.tv_cost_breakdown)

        val routeName = if (isGreek) route.nameEl else route.nameEn
        tvDistance.text = getString(R.string.trip_cost_route_distance, routeName, route.distanceKm)

        // When vehicle selected, auto-fill consumption
        dropdown.setOnItemClickListener { _, _, position, _ ->
            val vehicle = vehicles[position]
            viewLifecycleOwner.lifecycleScope.launch {
                val consumption = viewModel.getConsumptionForVehicle(vehicle.id, vehicle.type)
                etConsumption.setText("%.1f".format(consumption))
                val isDefault = consumption == viewModel.defaultConsumption(vehicle.type)
                tvConsumptionInfo.text = if (isDefault)
                    getString(R.string.trip_cost_consumption_default, vehicle.type.name)
                else
                    getString(R.string.trip_cost_consumption_from_logs)
                tvConsumptionInfo.visibility = android.view.View.VISIBLE
            }
        }

        // Auto-select first vehicle if only one
        if (vehicles.size == 1) {
            dropdown.setText(vehicleNames[0], false)
            val vehicle = vehicles[0]
            viewLifecycleOwner.lifecycleScope.launch {
                val consumption = viewModel.getConsumptionForVehicle(vehicle.id, vehicle.type)
                etConsumption.setText("%.1f".format(consumption))
                val isDefault = consumption == viewModel.defaultConsumption(vehicle.type)
                tvConsumptionInfo.text = if (isDefault)
                    getString(R.string.trip_cost_consumption_default, vehicle.type.name)
                else
                    getString(R.string.trip_cost_consumption_from_logs)
                tvConsumptionInfo.visibility = android.view.View.VISIBLE
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.trip_cost_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.trip_cost_calculate)) { _, _ -> }
            .setNegativeButton(getString(R.string.community_cancel), null)
            .create()
            .also { dialog ->
                dialog.show()
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val consumption = parseFlexibleDouble(etConsumption.text?.toString())
                    val fuelPrice = parseFlexibleDouble(etFuelPrice.text?.toString())

                    if (consumption == null || consumption <= 0 || fuelPrice == null || fuelPrice <= 0) {
                        Toast.makeText(requireContext(), getString(R.string.trip_cost_fill_fields), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val litersNeeded = (route.distanceKm / 100.0) * consumption
                    val totalCost = litersNeeded * fuelPrice

                    tvCostResult.text = "€%.2f".format(totalCost)
                    tvBreakdown.text = getString(
                        R.string.trip_cost_breakdown,
                        "%.1f".format(litersNeeded),
                        "%.1f".format(consumption),
                        "%.2f".format(fuelPrice)
                    )
                    cardResult.visibility = android.view.View.VISIBLE
                }
            }
    }

    private fun parseFlexibleDouble(value: String?): Double? =
        value?.trim()?.replace(',', '.')?.toDoubleOrNull()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun navigateToDetail(route: EkdromeRoute) {
        val sourceType = when (viewModel.selectedTab.value) {
            RouteTab.BUILTIN -> "builtin"
            RouteTab.COMMUNITY -> "community"
            RouteTab.SAVED -> "saved"
        }
        val bundle = bundleOf(
            "routeId" to route.id,
            "sourceType" to sourceType,
            "firestoreId" to route.firestoreId,
            "nameEn" to route.nameEn,
            "nameEl" to route.nameEl,
            "region" to route.region.name,
            "difficulty" to route.difficulty.name,
            "tags" to route.tags.joinToString(",") { it.name },
            "distanceKm" to route.distanceKm,
            "rating" to route.rating,
            "descriptionEn" to route.descriptionEn,
            "descriptionEl" to route.descriptionEl,
            "latitude" to route.latitude.toFloat(),
            "longitude" to route.longitude.toFloat(),
            "startLocation" to route.startLocation,
            "endLocation" to route.endLocation,
            "waypoints" to route.waypoints.joinToString(","),
            "reviewCount" to route.reviewCount
        )
        findNavController().navigate(R.id.action_ekdromes_to_routeDetail, bundle)
    }
}
