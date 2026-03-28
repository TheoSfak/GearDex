package com.geardex.app.ui.ekdromes

import androidx.lifecycle.ViewModel
import com.geardex.app.data.model.EkdromeDifficulty
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.data.model.EkdromeTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class EkdromesViewModel @Inject constructor() : ViewModel() {

    private val _selectedRegion = MutableStateFlow(EkdromeRegion.ALL)
    val selectedRegion: StateFlow<EkdromeRegion> = _selectedRegion

    private val _selectedTag = MutableStateFlow<EkdromeTag?>(null)
    val selectedTag: StateFlow<EkdromeTag?> = _selectedTag

    private val allRoutes = listOf(
        EkdromeRoute(
            1, "Sfakia Mountain Pass", "Πέρασμα Σφακίων",
            EkdromeRegion.CRETE, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY),
            EkdromeDifficulty.MEDIUM, 85, 4.7f,
            "Stunning mountain road through the White Mountains descending to the Libyan Sea.",
            "Εκπληκτικός ορεινός δρόμος μέσα από τα Λευκά Όρη με κατάβαση στο Λιβυκό Πέλαγος."
        ),
        EkdromeRoute(
            2, "Mani Peninsula Loop", "Γύρος Μάνης",
            EkdromeRegion.PELOPONNESE, listOf(EkdromeTag.MOTO, EkdromeTag.ASPHALT),
            EkdromeDifficulty.EASY, 120, 4.5f,
            "Coastal and mountain loop through the wild Mani peninsula with stone tower villages.",
            "Παράκτια και ορεινή διαδρομή μέσα από την άγρια Μάνη με πύργους από πέτρα."
        ),
        EkdromeRoute(
            3, "Vikos Gorge Route", "Φαράγγι Βίκου",
            EkdromeRegion.EPIRUS, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY),
            EkdromeDifficulty.HARD, 65, 4.9f,
            "Technical mountain roads around the world's deepest gorge with epic views.",
            "Τεχνικός ορεινός δρόμος γύρω από το βαθύτερο φαράγγι του κόσμου."
        ),
        EkdromeRoute(
            4, "Nestos River Valley", "Κοιλάδα Νέστου",
            EkdromeRegion.MACEDONIA, listOf(EkdromeTag.ASPHALT, EkdromeTag.MOTO),
            EkdromeDifficulty.EASY, 95, 4.3f,
            "Scenic river valley road through dense forests and traditional villages.",
            "Γραφικός δρόμος κοιλάδας μέσα από πυκνά δάση και παραδοσιακά χωριά."
        ),
        EkdromeRoute(
            5, "Pantokrator Summit", "Κορυφή Παντοκράτορα",
            EkdromeRegion.EPIRUS, listOf(EkdromeTag.OFFROAD, EkdromeTag.TWISTY),
            EkdromeDifficulty.HARD, 40, 4.4f,
            "Challenging dirt track to a high peak with 360-degree mountain views.",
            "Απαιτητικό χωματόδρομο σε ψηλή κορυφή με πανοραμική θέα 360 μοιρών."
        ),
        EkdromeRoute(
            6, "Parnitha Mountain Loop", "Γύρος Πάρνηθας",
            EkdromeRegion.ATTICA, listOf(EkdromeTag.MOTO, EkdromeTag.ASPHALT),
            EkdromeDifficulty.EASY, 55, 4.0f,
            "Quick escape from Athens through forested mountain roads.",
            "Γρήγορη απόδραση από την Αθήνα μέσα από δασωμένους ορεινούς δρόμους."
        ),
        EkdromeRoute(
            7, "Olympus Foothills Tour", "Πρόποδες Ολύμπου",
            EkdromeRegion.MACEDONIA, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY),
            EkdromeDifficulty.MEDIUM, 110, 4.6f,
            "The roads around Greece's highest mountain through forests and plateaus.",
            "Οι δρόμοι γύρω από το ψηλότερο βουνό της Ελλάδας μέσα από δάση και οροπέδια."
        ),
        EkdromeRoute(
            8, "Taygetos Pass", "Πέρασμα Ταϋγέτου",
            EkdromeRegion.PELOPONNESE, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY, EkdromeTag.OFFROAD),
            EkdromeDifficulty.HARD, 75, 4.8f,
            "Epic pass over the Taygetos mountain range with breathtaking valley views.",
            "Εκπληκτικό πέρασμα πάνω από την οροσειρά Ταϋγέτου με θέα στους κόλπους."
        )
    )

    val filteredRoutes: Flow<List<EkdromeRoute>> = combine(_selectedRegion, _selectedTag) { region, tag ->
        allRoutes.filter { route ->
            (region == EkdromeRegion.ALL || route.region == region) &&
                    (tag == null || tag in route.tags)
        }
    }

    fun selectRegion(region: EkdromeRegion) {
        _selectedRegion.value = region
    }

    fun selectTag(tag: EkdromeTag?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }
}
