package com.example.dealoptimizer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.dealoptimizer.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY isDefault DESC, id ASC")
    fun getAllUsers(): Flow<List<User>>

    @Insert
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Long)

    @Query("SELECT * FROM users WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultUser(): User?

    // 用户管理配套：清空某用户名下商品（外键未开，手动删）
    @Query("DELETE FROM products WHERE ownerId = :ownerId")
    suspend fun deleteProductsByOwner(ownerId: Long)
}
