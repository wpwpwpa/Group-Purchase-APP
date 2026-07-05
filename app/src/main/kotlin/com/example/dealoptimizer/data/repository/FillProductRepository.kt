package com.example.dealoptimizer.data.repository

import com.example.dealoptimizer.data.dao.FillProductDao
import com.example.dealoptimizer.data.model.FillProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FillProductRepository(private val fillProductDao: FillProductDao) {
    val allFillProducts = fillProductDao.getAllFillProducts()

    suspend fun insertFillProduct(fillProduct: FillProduct) {
        fillProductDao.insertFillProduct(fillProduct)
    }

    suspend fun deleteAllFillProducts() {
        fillProductDao.deleteAllFillProducts()
    }

    suspend fun initializeDefaultProducts() {
        withContext(Dispatchers.IO) {
            val count = fillProductDao.getCount()
            if (count == 0) {
                fillProductDao.insertAll(defaultFillProducts)
            }
        }
    }
}