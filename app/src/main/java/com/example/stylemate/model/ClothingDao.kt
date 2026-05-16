package com.example.stylemate.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 🗄️ ClothingDao — Data Access Object cho ClothingItemEntity.
 *
 * Sử dụng Coroutines (suspend) để thao tác ghi không block Main Thread.
 * Sử dụng Flow<List<T>> để UI tự động cập nhật khi dữ liệu thay đổi (reactive).
 */
@Dao
interface ClothingDao {

    /**
     * Lấy toàn bộ danh sách clothing items, trả về [Flow] để UI observe.
     * Room tự động emit lại danh sách mới mỗi khi có insert/delete/update.
     */
    @Query("SELECT * FROM clothing_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ClothingItemEntity>>

    /**
     * Thêm một item mới vào database.
     * Dùng [OnConflictStrategy.REPLACE] để nếu trùng id thì ghi đè.
     * Hàm [suspend] — phải gọi từ Coroutine scope.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClothingItemEntity)

    /**
     * Xoá một item khỏi database dựa trên entity.
     * Room tự động map theo PrimaryKey.
     * Hàm [suspend] — phải gọi từ Coroutine scope.
     */
    @Delete
    suspend fun deleteItem(item: ClothingItemEntity)

    /**
     * Lấy danh sách items theo category, trả về [Flow] để UI observe reactive.
     * Dùng cho chức năng lọc theo danh mục trên màn hình tủ đồ.
     */
    @Query("SELECT * FROM clothing_items WHERE category = :category ORDER BY createdAt DESC")
    fun getItemsByCategory(category: String): Flow<List<ClothingItemEntity>>

    /**
     * Đếm số lượng items trong một category cụ thể, trả về [Flow] để UI observe.
     * Dùng để hiển thị số lượng bên cạnh chip category trên màn hình tủ đồ.
     */
    @Query("SELECT COUNT(*) FROM clothing_items WHERE category = :category")
    fun getItemCountByCategory(category: String): Flow<Int>

    /**
     * Đếm tổng số lượng items, trả về [Flow] để UI observe.
     */
    @Query("SELECT COUNT(*) FROM clothing_items")
    fun getTotalItemCount(): Flow<Int>

    /**
     * Lấy một item theo ID.
     * Dùng khi cần hiển thị chi tiết hoặc chỉnh sửa.
     * Trả về nullable vì item có thể không tồn tại.
     */
    @Query("SELECT * FROM clothing_items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): ClothingItemEntity?
}
