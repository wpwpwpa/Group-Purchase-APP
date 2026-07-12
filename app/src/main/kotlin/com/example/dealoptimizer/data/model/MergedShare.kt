package com.example.dealoptimizer.data.model

data class MergedShare(
    val ownOriginal: Double,
    val remaining: Double = 0.0,
    val discount: Double,
    val payable: Double
)
