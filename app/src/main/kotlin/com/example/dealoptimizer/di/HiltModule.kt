package com.example.dealoptimizer.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // 全新安装：建表后补默认用户「本人」（升级场景由 MIGRATION_5_6 负责）
                    db.execSQL("INSERT INTO users (nickname, isDefault, isSelected) VALUES ('本人', 1, 1)")
                    db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'users'")
                    db.execSQL("INSERT INTO sqlite_sequence (name, seq) VALUES ('users', 1)")
                }
            })
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
    fun provideUserRepository(database: AppDatabase): UserRepository {
        return UserRepository(database.userDao())
    }

    @Provides
    @Singleton
    fun provideCouponModeRepository(@ApplicationContext context: Context): CouponModeRepository {
        return CouponModeRepository(context)
    }

    @Provides
    @Singleton
    fun provideDiscountCalculator(): DiscountCalculator {
        return DiscountCalculator()
    }
}
