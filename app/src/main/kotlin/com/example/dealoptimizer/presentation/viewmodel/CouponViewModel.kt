package com.example.dealoptimizer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealoptimizer.data.model.Coupon
import com.example.dealoptimizer.data.model.CouponType
import com.example.dealoptimizer.data.repository.CouponModeRepository
import com.example.dealoptimizer.data.repository.CouponRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CouponViewModel @Inject constructor(
    private val couponRepository: CouponRepository,
    private val couponModeRepository: CouponModeRepository
) : ViewModel() {

    val allCoupons: Flow<List<Coupon>> = couponRepository.allCoupons

    val isStackableMode: StateFlow<Boolean> = couponModeRepository.isStackableMode

    fun setCouponMode(isStackable: Boolean) {
        couponModeRepository.setStackableMode(isStackable)
    }

    fun insertCoupon(
        name: String,
        type: CouponType,
        threshold: Double,
        purchasePrice: Double = 0.0,
        discountValue: Double,
        maxUsages: Int = Int.MAX_VALUE,
        isStackable: Boolean = false,
        isSingleUse: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            couponRepository.insertCoupon(
                Coupon(
                    name = name,
                    type = type,
                    threshold = threshold,
                    purchasePrice = purchasePrice,
                    discountValue = discountValue,
                    maxUsages = maxUsages,
                    isStackable = isStackable,
                    isSingleUse = isSingleUse
                )
            )
        }
    }

    fun updateCoupon(coupon: Coupon) {
        viewModelScope.launch(Dispatchers.IO) {
            couponRepository.updateCoupon(coupon)
        }
    }

    fun deleteCoupon(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            couponRepository.deleteCoupon(id)
        }
    }

    fun deleteAllCoupons() {
        viewModelScope.launch(Dispatchers.IO) {
            couponRepository.deleteAllCoupons()
        }
    }
}
