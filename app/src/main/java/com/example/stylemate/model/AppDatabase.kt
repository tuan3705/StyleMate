package com.example.stylemate.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 🗄️ AppDatabase — Room Database singleton cho StyleMate.
 *
 * 📌 Danh sách Entity:
 * - [ClothingItemEntity]: Module Quản lý tủ đồ.
 * - [OutfitEntity]: Module Phối đồ (Outfit).
 * - [OutfitClothingCrossRef]: Bảng trung gian quan hệ N-N Outfit ↔ ClothingItem.
 * - [Item]: Entity cũ (giữ lại tương thích).
 *
 * Singleton pattern với Double-Checked Locking (@Volatile + synchronized)
 * đảm bảo thread-safe, chỉ khởi tạo một lần duy nhất trong toàn bộ vòng đời app.
 */
@Database(
    entities = [
        ClothingItemEntity::class,
        OutfitEntity::class,
        OutfitClothingCrossRef::class,
        Item::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // DAO cho ClothingItem
    abstract fun clothingDao(): ClothingDao

    // DAO cho Outfit
    abstract fun outfitDao(): OutfitDao

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
