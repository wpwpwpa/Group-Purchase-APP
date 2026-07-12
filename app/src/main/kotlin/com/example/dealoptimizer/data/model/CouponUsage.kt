package com.example.dealoptimizer.data.model

data class CouponUsage(
    val coupon: Coupon,
    val count: Int,
    val productGroup: List<Product> = emptyList(),
    val fillProducts: List<FillProduct> = emptyList(),
    /**
     * 本条用券的实际优惠金额，由 DiscountCalculator 在构造时写入（金额计算的唯一真相源）。
     * 视图层/分摊计算应直接读取本字段，禁止再手抄折扣公式，避免与领域层分叉。
     */
    val discount: Double = 0.0
)