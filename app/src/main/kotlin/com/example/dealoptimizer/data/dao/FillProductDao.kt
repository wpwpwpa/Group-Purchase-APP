package com.example.dealoptimizer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.dealoptimizer.data.model.FillProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface FillProductDao {
    @Query("SELECT * FROM fill_products ORDER BY category, isHot DESC, price")
    fun getAllFillProducts(): Flow<List<FillProduct>>

    @Query("SELECT * FROM fill_products WHERE category = :category ORDER BY isHot DESC, price")
    fun getFillProductsByCategory(category: String): Flow<List<FillProduct>>

    @Insert
    suspend fun insertFillProduct(fillProduct: FillProduct)

    @Insert
    suspend fun insertAll(fillProducts: List<FillProduct>)

    @Query("SELECT COUNT(*) FROM fill_products")
    suspend fun getCount(): Int

    @Query("DELETE FROM fill_products")
    suspend fun deleteAllFillProducts()
}