package com.example.dealoptimizer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealoptimizer.data.model.Product
import com.example.dealoptimizer.data.model.User
import com.example.dealoptimizer.data.repository.ProductRepository
import com.example.dealoptimizer.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val allProducts: Flow<List<Product>> = productRepository.allProducts
    val allUsers: Flow<List<User>> = userRepository.allUsers

    // 商品页主体只渲染勾选用户（isSelected=true）的框
    val checkedUsers: Flow<List<User>> = userRepository.allUsers.map { users -> users.filter { it.isSelected } }

    private val _addButtonOffset = MutableStateFlow<Pair<Float, Float>?>(null)
    val addButtonOffset: StateFlow<Pair<Float, Float>?> = _addButtonOffset

    fun updateAddButtonOffset(x: Float, y: Float) {
        _addButtonOffset.value = x to y
    }

    fun insertProduct(name: String, price: Double, isRequired: Boolean = false, ownerId: Long = 1, color: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.insertProduct(
                Product(name = name, originalPrice = price, isRequired = isRequired, ownerId = ownerId, color = color)
            )
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

    // ---- 用户管理（多用户凑单） ----
    fun addUser(nickname: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.insertUser(User(nickname = nickname))
        }
    }

    fun renameUser(user: User, nickname: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.updateUser(user.copy(nickname = nickname))
        }
    }

    fun deleteUser(user: User) {
        if (user.isDefault) return   // 禁止删除默认用户「本人」
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.deleteProductsByOwner(user.id)
            userRepository.deleteUser(user.id)
        }
    }

    fun setUserChecked(user: User, checked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.updateUser(user.copy(isSelected = checked))
        }
    }

    fun clearByOwner(ownerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.deleteProductsByOwner(ownerId)
        }
    }
}
