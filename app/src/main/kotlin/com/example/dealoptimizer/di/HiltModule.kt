package com.example.dealoptimizer.di

import android.content.Context
import androidx.room.Room
import com.example.dealoptimizer.data.database.AppDatabase
import com.example.dealoptimizer.data.repository.*
import com.example.dealoptimizer.domain.algorithm.DiscountCalculator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "deal_optimizer_db"
        )
            .allowMainThreadQueries()
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideProductRepository(database: AppDatabase): ProductRepository {
        return ProductRepository(database.productDao())
    }

    @Provides
    @Singleton
    fun provideCouponRepository(database: AppDatabase): CouponRepository {
        return CouponRepository(database.couponDao())
    }

    @Provides
    @Singleton
    fun provideFillProductRepository(database: AppDatabase): FillProductRepository {
        return FillProductRepository(database.fillProductDao())
    }

    @Provides
    @Singleton
    fun provideDiscountCalculator(): DiscountCalculator {
        return DiscountCalculator()
    }
}
