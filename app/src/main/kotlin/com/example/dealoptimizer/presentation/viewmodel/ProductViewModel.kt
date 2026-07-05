package com.example.dealoptimizer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealoptimizer.data.model.Product
import com.example.dealoptimizer.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    val allProducts: Flow<List<Product>> = productRepository.allProducts

    private val _addButtonOffset = MutableStateFlow<Pair<Float, Float>?>(null)
    val addButtonOffset: StateFlow<Pair<Float, Float>?> = _addButtonOffset

    fun updateAddButtonOffset(x: Float, y: Float) {
        _addButtonOffset.value = x to y
    }

    fun insertProduct(name: String, price: Double, isRequired: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.insertProduct(Product(name = name, originalPrice = price, isRequired = isRequired))
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.updateProduct(product)
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.deleteProduct(id)
        }
    }

    fun deleteAllProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.deleteAllProducts()
        }
    }

    suspend fun getTotalPrice(): Double {
        return productRepository.getTotalPrice()
    }
}
