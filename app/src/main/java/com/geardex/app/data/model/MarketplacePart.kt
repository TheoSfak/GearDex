package com.geardex.app.data.model

data class MarketplacePart(
    val id: String = "",
    val partName: String = "",
    val category: PartCategory = PartCategory.OTHER,
    val vehicleMake: String = "",
    val vehicleModel: String = "",
    val vehicleYear: Int = 0,
    val price: Double = 0.0,
    val condition: PartCondition = PartCondition.NEW,
    val description: String = "",
    val sellerName: String = "",
    val sellerUid: String = "",
    val contactInfo: String = "",
    val region: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = true
)

enum class PartCategory {
    ENGINE, BRAKES, SUSPENSION, ELECTRICAL, BODY, EXHAUST, TIRES, WHEELS, INTERIOR, ACCESSORIES, OTHER
}

enum class PartCondition {
    NEW, USED_LIKE_NEW, USED_GOOD, USED_FAIR, FOR_PARTS
}
