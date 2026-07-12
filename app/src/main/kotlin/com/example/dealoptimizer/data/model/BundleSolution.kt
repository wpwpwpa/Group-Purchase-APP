package com.example.dealoptimizer.data.model

data class BundleSolution(
    val products: List<Product>,
    val couponUsages: List<CouponUsage>,
    val originalTotal: Double,
    val finalPrice: Double,
    val fillProducts: List<FillProduct> = emptyList()
) {
    val totalDiscount: Double get() = originalTotal - finalPrice

    fun getDiscountBreakdown(): String {
        val sb = StringBuilder()
        couponUsages.forEach { usage ->
            val totalDiscount = usage.discount
            sb.append("${usage.coupon.name} ×${usage.count}: -¥${"%.2f".format(totalDiscount)}\n")
        }
        return sb.toString()
    }
}