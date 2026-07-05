package com.example.dealoptimizer.di

import android.content.Context
import androidx.room.Room
import com.example.dealoptimizer.data.database.AppDatabase
import com.example.dealoptimizer.data.repository.*
import com.example.dealoptimizer.domain.algorithm.DiscountCalculator

object AppContainer {
    lateinit var context: Context
        private set

    private var database: AppDatabase? = null

    val productRepository: ProductRepository by lazy {
        ProductRepository(getDatabase().productDao())
    }

    val couponRepository: CouponRepository by lazy {
        CouponRepository(getDatabase().couponDao())
    }

    val fillProductRepository: FillProductRepository by lazy {
        FillProductRepository(getDatabase().fillProductDao())
    }

    val discountCalculator: DiscountCalculator by lazy {
        DiscountCalculator()
    }

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private fun getDatabase(): AppDatabase {
        if (database == null) {
            database = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "deal_optimizer_db"
            )
                .allowMainThreadQueries()
                .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
        }
        return database!!
    }
}
