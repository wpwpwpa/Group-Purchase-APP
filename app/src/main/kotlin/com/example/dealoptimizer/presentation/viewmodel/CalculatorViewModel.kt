package com.example.dealoptimizer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealoptimizer.data.model.*
import com.example.dealoptimizer.data.repository.CouponRepository
import com.example.dealoptimizer.data.repository.FillProductRepository
import com.example.dealoptimizer.data.repository.ProductRepository
import com.example.dealoptimizer.domain.algorithm.DiscountCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val couponRepository: CouponRepository,
    private val fillProductRepository: FillProductRepository,
    private val discountCalculator: DiscountCalculator
) : ViewModel() {

    private val _solution = MutableStateFlow<BundleSolution?>(null)
    val solution: StateFlow<BundleSolution?> = _solution

    private val _comparisonResult = MutableStateFlow<ComparisonResult?>(null)
    val comparisonResult: StateFlow<ComparisonResult?> = _comparisonResult

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    private val _allCoupons = MutableStateFlow<List<Coupon>>(emptyList())
    val allCoupons: StateFlow<List<Coupon>> = _allCoupons

    init {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.allProducts.collect { products ->
                _allProducts.value = products
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            couponRepository.allCoupons.collect { coupons ->
                _allCoupons.value = coupons
            }
        }
    }

    fun clearCalculation() {
        _solution.value = null
        _comparisonResult.value = null
    }

    fun calculateBestPrice(useFillProducts: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val products = productRepository.allProducts.first()
            val coupons = couponRepository.allCoupons.first()
            val enabledCoupons = coupons.filter { it.isEnabled }
            val fillProducts = if (useFillProducts) fillProductRepository.allFillProducts.first() else emptyList()
            _allProducts.value = products
            _allCoupons.value = coupons

            if (products.isEmpty() || enabledCoupons.isEmpty()) {
                _solution.value = null
                _comparisonResult.value = null
                return@launch
            }

            val bestSolution = discountCalculator.calculateBestCombination(products, enabledCoupons, fillProducts, useFillProducts)
            _solution.value = bestSolution

            val comparison = discountCalculator.calculateComparisonResult(products, enabledCoupons, fillProducts, useFillProducts)
            _comparisonResult.value = comparison
        }
    }

    fun calculateForProducts(selectedProducts: List<Product>, useFillProducts: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val coupons = _allCoupons.value.ifEmpty { couponRepository.allCoupons.first() }
            val enabledCoupons = coupons.filter { it.isEnabled }
            val fillProducts = if (useFillProducts) fillProductRepository.allFillProducts.first() else emptyList()

            if (selectedProducts.isEmpty() || enabledCoupons.isEmpty()) {
                _solution.value = null
                _comparisonResult.value = null
                return@launch
            }

            _solution.value = discountCalculator.calculateBestCombination(selectedProducts, enabledCoupons, fillProducts, useFillProducts)
            _comparisonResult.value = discountCalculator.calculateComparisonResult(selectedProducts, enabledCoupons, fillProducts, useFillProducts)
        }
    }
}
