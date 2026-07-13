package com.example.dealoptimizer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CouponType {
    FULL_REDUCTION, DISCOUNT, NO_THRESHOLD, VOUCHER
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
    val isEnabled: Boolean = true,
    // 买券人：代金券(VOUCHER)的购买成本(purchasePrice)归属此人，用于「谁买谁承担」分摊。默认本人(1)。
    val ownerId: Long = 1
)
