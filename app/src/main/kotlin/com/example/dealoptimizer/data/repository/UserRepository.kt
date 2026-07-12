package com.example.dealoptimizer.data.repository

import com.example.dealoptimizer.data.dao.UserDao
import com.example.dealoptimizer.data.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    suspend fun deleteUser(id: Long) = userDao.deleteUser(id)
    suspend fun getDefaultUser(): User? = userDao.getDefaultUser()
    suspend fun deleteProductsByOwner(ownerId: Long) = userDao.deleteProductsByOwner(ownerId)
}
