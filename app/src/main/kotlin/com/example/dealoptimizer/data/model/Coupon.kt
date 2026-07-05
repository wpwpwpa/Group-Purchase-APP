package com.example.dealoptimizer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CouponType {
    FULL_REDUCTION, DISCOUNT, NO_THRESHOLD
}

@Entity(tableName = "coupons")
data class Coupon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: CouponType,
    val threshold: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val discountValue: Double,
    val maxUsages: Int = Int.MAX_VALUE,
    val isStackable: Boolean = false,
    val isSingleUse: Boolean = false,
    val isEnabled: Boolean = true
)
