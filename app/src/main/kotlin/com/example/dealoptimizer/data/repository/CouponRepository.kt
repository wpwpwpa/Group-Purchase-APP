package com.example.dealoptimizer.data.repository

import com.example.dealoptimizer.data.dao.CouponDao
import com.example.dealoptimizer.data.model.Coupon
import kotlinx.coroutines.flow.Flow

class CouponRepository(private val couponDao: CouponDao) {
    val allCoupons: Flow<List<Coupon>> = couponDao.getAllCoupons()

    suspend fun insertCoupon(coupon: Coupon) {
        couponDao.insertCoupon(coupon)
    }

    suspend fun updateCoupon(coupon: Coupon) {
        couponDao.updateCoupon(coupon)
    }

    suspend fun deleteCoupon(id: Long) {
        couponDao.deleteCoupon(id)
    }

    suspend fun deleteAllCoupons() {
        couponDao.deleteAllCoupons()
    }
}
