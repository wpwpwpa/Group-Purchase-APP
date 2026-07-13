package com.example.dealoptimizer.data.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dealoptimizer.data.dao.CouponDao
import com.example.dealoptimizer.data.dao.FillProductDao
import com.example.dealoptimizer.data.dao.ProductDao
import com.example.dealoptimizer.data.dao.UserDao
import com.example.dealoptimizer.data.model.Coupon
import com.example.dealoptimizer.data.model.FillProduct
import com.example.dealoptimizer.data.model.Product
import com.example.dealoptimizer.data.model.User

@Database(
    entities = [Product::class, Coupon::class, FillProduct::class, User::class],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun couponDao(): CouponDao
    abstract fun fillProductDao(): FillProductDao
    abstract fun userDao(): UserDao

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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) 建用户表
                db.execSQL(
                    """
                    CREATE TABLE users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nickname TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        isSelected INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
                // 2) 插默认用户（本人），让自增拿 id=1，避免后续自增冲突
                db.execSQL("INSERT INTO users (nickname, isDefault, isSelected) VALUES ('本人', 1, 1)")
                // 3) 关键：确保 sqlite_sequence 指向 1，否则下一条新用户 INSERT 可能 id 冲突
                db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'users'")
                db.execSQL("INSERT INTO sqlite_sequence (name, seq) VALUES ('users', 1)")
                // 4) 给已有商品补 ownerId，默认归本人(1)
                db.execSQL("ALTER TABLE products ADD COLUMN ownerId INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN color TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 代金券(VOUCHER)需要记录买券人，用于「谁买谁承担」分摊。
                // 新列默认本人(1)，老券自动归属本人，行为不变、数据不丢。
                db.execSQL("ALTER TABLE coupons ADD COLUMN ownerId INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
