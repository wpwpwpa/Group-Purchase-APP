package com.example.dealoptimizer.data.model

data class MergedShare(
    val ownOriginal: Double,
    val remaining: Double = 0.0,
    val discount: Double,
    val payable: Double,
    // 自购代金券的购买成本合计（仅 VOUCHER 类型、按 ownerId 归属）。
    // 谁买谁承担：这笔钱是买券人真实现金支出，不进货品优惠，单独挂账。
    // 真实应付 = payable + couponCost；真实净省 = discount - couponCost。
    val couponCost: Double = 0.0
)
