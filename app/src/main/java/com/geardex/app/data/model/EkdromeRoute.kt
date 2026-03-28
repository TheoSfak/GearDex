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
    val descriptionEl: String
)

enum class EkdromeRegion(val displayEn: String, val displayEl: String) {
    ALL("All Regions", "Όλες"),
    CRETE("Crete", "Κρήτη"),
    PELOPONNESE("Peloponnese", "Πελοπόννησος"),
    MACEDONIA("Macedonia", "Μακεδονία"),
    EPIRUS("Epirus", "Ήπειρος"),
    ATTICA("Attica", "Αττική")
}

enum class EkdromeTag(val displayEn: String, val displayEl: String) {
    MOTO("Moto", "Μοτό"),
    ASPHALT("Asphalt", "Άσφαλτος"),
    OFFROAD("Off-Road", "Εκτός Δρόμου"),
    TWISTY("Twisty", "Στροφές")
}

enum class EkdromeDifficulty(val displayEn: String, val displayEl: String) {
    EASY("Easy", "Εύκολο"),
    MEDIUM("Medium", "Μέτριο"),
    HARD("Hard", "Δύσκολο")
}
