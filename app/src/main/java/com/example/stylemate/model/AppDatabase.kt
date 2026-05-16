package com.example.stylemate.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 🗄️ AppDatabase — Room Database singleton cho StyleMate.
 *
 * - [ClothingItemEntity]: Entity chính cho module Quản lý tủ đồ.
 * - [Item]: Entity cũ (giữ lại tương thích với UI hiện tại, sẽ migrate sau).
 *
 * Singleton pattern với Double-Checked Locking (@Volatile + synchronized)
 * đảm bảo thread-safe, chỉ khởi tạo một lần duy nhất trong toàn bộ vòng đời app.
 */
@Database(
    entities = [ClothingItemEntity::class, Item::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // DAO mới cho ClothingItemEntity
    abstract fun clothingDao(): ClothingDao

    // DAO cũ (giữ lại tương thích)
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        /**
         * Lấy singleton database instance.
         * Thread-safe, chỉ khởi tạo một lần.
         */
        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stylemate_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
