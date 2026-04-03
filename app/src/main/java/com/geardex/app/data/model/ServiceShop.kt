package com.geardex.app.data.model

data class ServiceShop(
    val id: String = "",
    val nameEn: String = "",
    val nameEl: String = "",
    val category: ShopCategory = ShopCategory.GENERAL,
    val region: String = "",
    val address: String = "",
    val phone: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val submittedBy: String = "",
    val submittedByUid: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class ShopCategory {
    GENERAL, ENGINE_SPECIALIST, BODY_SHOP, TIRES, ELECTRICAL, MOTORCYCLE, PERFORMANCE, DETAILING
}

data class ShopReview(
    val id: String = "",
    val shopId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val userName: String = "",
    val userUid: String = "",
    val createdAt: Long = 0L
)
