package com.example.stylemate.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE category = :category")
    fun getItemsByCategory(category: String): Flow<List<Item>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Query("SELECT COUNT(*) FROM items")
    fun getItemCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE category = :category")
    fun getItemCountByCategory(category: String): Flow<Int>
}
