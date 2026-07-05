package com.example.dealoptimizer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.dealoptimizer.data.model.Coupon
import kotlinx.coroutines.flow.Flow

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons ORDER BY id DESC")
    fun getAllCoupons(): Flow<List<Coupon>>

    @Insert
    suspend fun insertCoupon(coupon: Coupon)

    @Update
    suspend fun updateCoupon(coupon: Coupon)

    @Query("DELETE FROM coupons WHERE id = :id")
    suspend fun deleteCoupon(id: Long)

    @Query("DELETE FROM coupons")
    suspend fun deleteAllCoupons()
}
