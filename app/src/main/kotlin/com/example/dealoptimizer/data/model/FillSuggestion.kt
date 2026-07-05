package com.example.dealoptimizer.data.model

data class FillSuggestion(
    val fillProducts: List<FillProduct>,
    val originalTotal: Double,
    val newTotal: Double,
    val discount: Double,
    val finalPrice: Double,
    val saving: Double
)