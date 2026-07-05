package com.example.dealoptimizer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fill_products")
data class FillProduct(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val category: String,
    val isHot: Boolean = false
)