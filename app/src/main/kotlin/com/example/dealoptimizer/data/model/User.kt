package com.example.dealoptimizer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val isDefault: Boolean = false,   // true = 本人
    val isSelected: Boolean = true    // 勾选状态长期留存
)
