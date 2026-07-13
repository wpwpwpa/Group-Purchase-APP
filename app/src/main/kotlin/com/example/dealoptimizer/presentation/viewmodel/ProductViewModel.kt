package com.example.dealoptimizer.presentation.viewmodel

import android.util.Log
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    // 用于 update/deleteAll 等操作前抓取旧数据快照
    private var productsSnapshot: List<Product> = emptyList()

    init {
        viewModelScope.launch {
            allProducts.collect { productsSnapshot = it }
        }
    }

    fun updateAddButtonOffset(x: Float, y: Float) {
        _addButtonOffset.value = x to y
    }

    fun insertProduct(name: String, price: Double, isRequired: Boolean = false, ownerId: Long = 1, color: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val product = Product(
                name = name,
                originalPrice = price,
                isRequired = isRequired,
                ownerId = ownerId,
                color = color
            )
            val newId = productRepository.insertProduct(product)
            pushUndo(DeleteUndo(product.copy(id = newId)))
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldProduct = productsSnapshot.find { it.id == product.id }
            productRepository.updateProduct(product)
            if (oldProduct != null) {
                pushUndo(UpdateUndo(oldProduct))
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.deleteProduct(product.id)
            pushUndo(InsertUndo(product))
        }
    }

    fun deleteAllProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = productsSnapshot.toList()
            if (snapshot.isEmpty()) return@launch
            productRepository.deleteAllProducts()
            pushUndo(ClearUndo(snapshot))
        }
    }

    suspend fun getTotalPrice(): Double {
        return productRepository.getTotalPrice()
    }

    // ---- 撤回（撤销）机制 ----

    private val undoStack = mutableListOf<UndoCommand>()
    private val undoMutex = Mutex()

    private sealed class UndoCommand {
        abstract suspend fun undo(productRepository: ProductRepository)
    }

    // 命名易误读：InsertUndo 在「删除商品」时入栈（撤销删除=重新插入）；
    //          DeleteUndo 在「新增商品」时入栈（撤销新增=删除该商品）。二者 undo 方向相反。
    private data class InsertUndo(val product: Product) : UndoCommand() {
        override suspend fun undo(productRepository: ProductRepository) {
            // 撤销「删除」= 恢复原商品；用全新自增 id 重插，避免显式回插原 id 触发主键/序列冲突
            productRepository.insertProduct(product.copy(id = 0))
        }
    }

    private data class DeleteUndo(val product: Product) : UndoCommand() {
        override suspend fun undo(productRepository: ProductRepository) {
            // 撤销「新增」= 删除刚新增的商品
            productRepository.deleteProduct(product.id)
        }
    }

    private data class UpdateUndo(val oldProduct: Product) : UndoCommand() {
        override suspend fun undo(productRepository: ProductRepository) {
            productRepository.updateProduct(oldProduct)
        }
    }

    private data class ClearUndo(val products: List<Product>) : UndoCommand() {
        override suspend fun undo(productRepository: ProductRepository) {
            // 撤销「清空」= 恢复全部；同样用全新自增 id 规避约束冲突
            products.forEach { productRepository.insertProduct(it.copy(id = 0)) }
        }
    }

    private suspend fun pushUndo(command: UndoCommand) {
        undoMutex.withLock {
            undoStack.add(command)
            if (undoStack.size > MAX_UNDO_COUNT) undoStack.removeAt(0)
            _canUndo.value = undoStack.isNotEmpty()
        }
    }

    fun undo() {
        viewModelScope.launch(Dispatchers.IO) {
            val command = undoMutex.withLock {
                if (undoStack.isEmpty()) return@launch
                undoStack.removeAt(undoStack.size - 1).also {
                    _canUndo.value = undoStack.isNotEmpty()
                }
            }
            try {
                command.undo(productRepository)
            } catch (e: Exception) {
                // 撤销回放失败不得导致应用崩溃，仅记录便于排查
                android.util.Log.e("ProductViewModel", "undo failed", e)
            }
        }
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

    fun clearByOwner(ownerId: Long, products: List<Product>) {
        if (products.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.deleteProductsByOwner(ownerId)
            pushUndo(ClearUndo(products))
        }
    }

    companion object {
        private const val MAX_UNDO_COUNT = 20
    }
}
