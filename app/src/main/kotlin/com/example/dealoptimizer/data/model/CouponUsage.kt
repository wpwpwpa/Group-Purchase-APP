package com.example.dealoptimizer.data.model

data class CouponUsage(
    val coupon: Coupon,
    val count: Int,
    val productGroup: List<Product> = emptyList(),
    val fillProducts: List<FillProduct> = emptyList()
)