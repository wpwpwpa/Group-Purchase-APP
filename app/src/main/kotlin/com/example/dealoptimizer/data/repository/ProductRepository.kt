package com.example.dealoptimizer.data.repository

import com.example.dealoptimizer.data.dao.ProductDao
import com.example.dealoptimizer.data.model.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(id: Long) {
        productDao.deleteProduct(id)
    }

    suspend fun deleteAllProducts() {
        productDao.deleteAllProducts()
    }

    suspend fun getTotalPrice(): Double {
        return productDao.getTotalPrice() ?: 0.0
    }
}
