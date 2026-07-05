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
            val coupon = usage.coupon
            val totalDiscount = when (coupon.type) {
                CouponType.FULL_REDUCTION -> usage.count * coupon.discountValue
                CouponType.DISCOUNT -> originalTotal * (coupon.discountValue / 100) * usage.count
                CouponType.NO_THRESHOLD -> usage.count * coupon.discountValue
            }
            sb.append("${coupon.name} ×${usage.count}: -¥${"%.2f".format(totalDiscount)}\n")
        }
        return sb.toString()
    }
}