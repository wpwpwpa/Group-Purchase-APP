package com.example.dealoptimizer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val originalPrice: Double,
    val isRequired: Boolean = false,
    val ownerId: Long = 1,   // 归属用户，默认 1（本人）
    val color: String = ""   // 颜色（单选，拼接进商品名称，如「白短袖」）
)
