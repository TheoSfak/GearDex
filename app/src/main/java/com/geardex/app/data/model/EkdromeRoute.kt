package com.geardex.app.data.model

data class EkdromeRoute(
    val id: Int,
    val nameEn: String,
    val nameEl: String,
    val region: EkdromeRegion,
    val tags: List<EkdromeTag>,
    val difficulty: EkdromeDifficulty,
    val distanceKm: Int,
    val rating: Float,
    val descriptionEn: String,
    val descriptionEl: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val startLocation: String = "",
    val endLocation: String = "",
    val waypoints: List<String> = emptyList(),
    val firestoreId: String = "",
    val reviewCount: Int = 0
)

enum class EkdromeRegion(val displayEn: String, val displayEl: String) {
    ALL("All Regions", "Όλες"),
    CRETE("Crete", "Κρήτη"),
    PELOPONNESE("Peloponnese", "Πελοπόννησος"),
    MACEDONIA("Macedonia", "Μακεδονία"),
    EPIRUS("Epirus", "Ήπειρος"),
    ATTICA("Attica", "Αττική"),
    THESSALY("Thessaly", "Θεσσαλία"),
    DODECANESE("Dodecanese", "Δωδεκάνησα")
}

enum class EkdromeTag(val displayEn: String, val displayEl: String) {
    MOTO("Moto", "Μοτό"),
    ASPHALT("Asphalt", "Άσφαλτος"),
    OFFROAD("Off-Road", "Εκτός Δρόμου"),
    TWISTY("Twisty", "Στροφές"),
    SCENIC("Scenic", "Πανοραμική")
}

enum class EkdromeDifficulty(val displayEn: String, val displayEl: String) {
    EASY("Easy", "Εύκολο"),
    MEDIUM("Medium", "Μέτριο"),
    HARD("Hard", "Δύσκολο")
}

data class RouteReview(
    val id: String = "",
    val routeId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val userName: String = "",
    val userUid: String = "",
    val createdAt: Long = 0L
)
