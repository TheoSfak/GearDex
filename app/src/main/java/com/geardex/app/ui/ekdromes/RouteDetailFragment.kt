package com.geardex.app.ui.ekdromes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.data.model.EkdromeDifficulty
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.data.model.EkdromeTag
import com.geardex.app.data.repository.BuiltinRouteRepository
import com.geardex.app.databinding.FragmentRouteDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class RouteDetailFragment : Fragment() {

    private var _binding: FragmentRouteDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkdromesViewModel by viewModels()

    @Inject lateinit var builtinRouteRepository: BuiltinRouteRepository

    private val isGreek: Boolean
        get() = Locale.getDefault().language == "el"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRouteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val routeId = arguments?.getInt("routeId") ?: return
        val sourceType = arguments?.getString("sourceType") ?: "builtin"
        val firestoreId = arguments?.getString("firestoreId") ?: ""

        // Find route
        val route = findRoute(routeId, sourceType, firestoreId) ?: run {
            findNavController().popBackStack()
            return
        }

        bindRoute(route)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Save FAB
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.savedRouteKeys.collect { keys ->
                    val key = viewModel.routeKey(route)
                    val isSaved = keys.contains(key)
                    binding.fabSaveRoute.setImageResource(
                        if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
                    )
                }
            }
        }

        binding.fabSaveRoute.setOnClickListener {
            viewModel.toggleSaveRoute(route)
        }

        // Reviews
        val reviewAdapter = ReviewAdapter()
        binding.recyclerReviews.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerReviews.adapter = reviewAdapter

        val routeReviewId = viewModel.getRouteReviewId(route)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observeReviews(routeReviewId).collect { reviews ->
                    reviewAdapter.submitList(reviews)
                    binding.tvReviewsEmpty.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerReviews.visibility = if (reviews.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }

        binding.btnAddReview.setOnClickListener {
            if (!viewModel.isFirebaseConfigured) {
                Toast.makeText(requireContext(), getString(R.string.community_firebase_required), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            showRateDialog(routeReviewId)
        }

        // Review submit result
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

    private fun findRoute(routeId: Int, sourceType: String, firestoreId: String): EkdromeRoute? {
        return when (sourceType) {
            "builtin" -> builtinRouteRepository.routes.find { it.id == routeId }
            "community" -> null // Community routes are loaded from Firestore; we reconstruct from args
            "saved" -> builtinRouteRepository.routes.find { it.id == routeId }
            else -> null
        } ?: reconstructRouteFromArgs()
    }

    private fun reconstructRouteFromArgs(): EkdromeRoute? {
        val args = arguments ?: return null
        val nameEn = args.getString("nameEn") ?: return null
        val nameEl = args.getString("nameEl") ?: nameEn
        val region = runCatching { EkdromeRegion.valueOf(args.getString("region") ?: "CRETE") }.getOrDefault(EkdromeRegion.CRETE)
        val difficulty = runCatching { EkdromeDifficulty.valueOf(args.getString("difficulty") ?: "MEDIUM") }.getOrDefault(EkdromeDifficulty.MEDIUM)
        val tagsStr = args.getString("tags") ?: ""
        val tags = tagsStr.split(",").mapNotNull { runCatching { EkdromeTag.valueOf(it.trim()) }.getOrNull() }
        return EkdromeRoute(
            id = args.getInt("routeId"),
            nameEn = nameEn,
            nameEl = nameEl,
            region = region,
            tags = tags,
            difficulty = difficulty,
            distanceKm = args.getInt("distanceKm"),
            rating = args.getFloat("rating"),
            descriptionEn = args.getString("descriptionEn") ?: "",
            descriptionEl = args.getString("descriptionEl") ?: "",
            latitude = args.getDouble("latitude"),
            longitude = args.getDouble("longitude"),
            startLocation = args.getString("startLocation") ?: "",
            endLocation = args.getString("endLocation") ?: "",
            waypoints = (args.getString("waypoints") ?: "").split(",").filter { it.isNotBlank() },
            firestoreId = args.getString("firestoreId") ?: "",
            reviewCount = args.getInt("reviewCount")
        )
    }

    private fun bindRoute(route: EkdromeRoute) {
        val greek = isGreek
        binding.tvRouteName.text = if (greek) route.nameEl else route.nameEn
        binding.tvRating.text = "%.1f".format(route.rating)
        binding.tvDifficulty.text = if (greek) route.difficulty.displayEl else route.difficulty.displayEn
        binding.tvDistance.text = "${route.distanceKm} km"
        binding.tvRegion.text = if (greek) route.region.displayEl else route.region.displayEn
        binding.tvTags.text = route.tags.joinToString(" · ") { if (greek) it.displayEl else it.displayEn }
        binding.tvDescription.text = if (greek) route.descriptionEl else route.descriptionEn

        // Route path
        if (route.startLocation.isNotEmpty() && route.endLocation.isNotEmpty()) {
            val waypointText = if (route.waypoints.isNotEmpty()) {
                " → ${route.waypoints.joinToString(" → ")}"
            } else ""
            binding.tvRoutePath.text = "${route.startLocation}$waypointText → ${route.endLocation}"
            binding.cardRoutePath.visibility = View.VISIBLE
        }

        // View on Map
        binding.btnViewOnMap.setOnClickListener {
            val label = if (greek) route.nameEl else route.nameEn
            val geoUri = Uri.parse("geo:${route.latitude},${route.longitude}?q=${route.latitude},${route.longitude}(${Uri.encode(label)})")
            startActivity(Intent(Intent.ACTION_VIEW, geoUri))
        }

        // Navigate
        if (route.startLocation.isNotEmpty() && route.endLocation.isNotEmpty()) {
            binding.btnNavigate.visibility = View.VISIBLE
            binding.btnNavigate.setOnClickListener {
                val allPoints = mutableListOf(route.startLocation)
                allPoints.addAll(route.waypoints)
                allPoints.add(route.endLocation)
                val path = allPoints.joinToString("/") { Uri.encode(it) }
                val mapsUrl = "https://www.google.com/maps/dir/$path"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
            }
        }

        // Calculate cost
        binding.btnCalculateCost.setOnClickListener { showTripCostDialog(route) }
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
        val etConsumption = dialogView.findViewById<TextInputEditText>(R.id.et_consumption)
        val etFuelPrice = dialogView.findViewById<TextInputEditText>(R.id.et_fuel_price)
        val cardResult = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_result)
        val tvCostResult = dialogView.findViewById<TextView>(R.id.tv_cost_result)
        val tvBreakdown = dialogView.findViewById<TextView>(R.id.tv_cost_breakdown)

        val routeName = if (isGreek) route.nameEl else route.nameEn
        tvDistance.text = getString(R.string.trip_cost_route_distance, routeName, route.distanceKm)

        dropdown.setOnItemClickListener { _, _, position, _ ->
            val vehicle = vehicles[position]
            viewLifecycleOwner.lifecycleScope.launch {
                val consumption = viewModel.getConsumptionForVehicle(vehicle.id, vehicle.type)
                etConsumption.setText("%.1f".format(consumption))
                val isDefault = consumption == viewModel.defaultConsumption(vehicle.type)
                tvConsumptionInfo.text = if (isDefault)
                    getString(R.string.trip_cost_consumption_default, vehicle.type.name)
                else getString(R.string.trip_cost_consumption_from_logs)
                tvConsumptionInfo.visibility = View.VISIBLE
            }
        }

        if (vehicles.size == 1) {
            dropdown.setText(vehicleNames[0], false)
            val vehicle = vehicles[0]
            viewLifecycleOwner.lifecycleScope.launch {
                val consumption = viewModel.getConsumptionForVehicle(vehicle.id, vehicle.type)
                etConsumption.setText("%.1f".format(consumption))
                val isDefault = consumption == viewModel.defaultConsumption(vehicle.type)
                tvConsumptionInfo.text = if (isDefault)
                    getString(R.string.trip_cost_consumption_default, vehicle.type.name)
                else getString(R.string.trip_cost_consumption_from_logs)
                tvConsumptionInfo.visibility = View.VISIBLE
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
                    val consumption = etConsumption.text?.toString()?.toDoubleOrNull()
                    val fuelPrice = etFuelPrice.text?.toString()?.toDoubleOrNull()
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
                    cardResult.visibility = View.VISIBLE
                }
            }
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
                if (rating > 0) viewModel.submitReview(routeId, rating, comment)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.community_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
