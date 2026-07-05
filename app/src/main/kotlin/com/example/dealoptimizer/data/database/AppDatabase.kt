package com.example.dealoptimizer.data.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dealoptimizer.data.dao.CouponDao
import com.example.dealoptimizer.data.dao.FillProductDao
import com.example.dealoptimizer.data.dao.ProductDao
import com.example.dealoptimizer.data.model.Coupon
import com.example.dealoptimizer.data.model.FillProduct
import com.example.dealoptimizer.data.model.Product

@Database(
    entities = [Product::class, Coupon::class, FillProduct::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun couponDao(): CouponDao
    abstract fun fillProductDao(): FillProductDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE products_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        originalPrice REAL NOT NULL,
                        isRequired INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO products_new (id, name, originalPrice, isRequired)
                    SELECT id, name, originalPrice, isRequired FROM products
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE products")
                db.execSQL("ALTER TABLE products_new RENAME TO products")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coupons ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
