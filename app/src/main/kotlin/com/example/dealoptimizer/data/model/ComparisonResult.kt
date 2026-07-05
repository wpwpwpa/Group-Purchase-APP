package com.example.dealoptimizer.data.model

data class ComparisonResult(
    val originalPrice: Double,
    val directDiscountPrice: Double,
    val fillSuggestions: List<FillSuggestion>
)