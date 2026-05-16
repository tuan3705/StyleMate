package com.example.stylemate.repository

import com.example.stylemate.model.ClothingDao
import com.example.stylemate.model.ClothingItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 🏪 ClothingRepository — Repository Pattern cho module ClothingItem.
 *
 * Lớp trung gian duy nhất giữa ViewModel và Data Source (Room).
 * - ViewModel KHÔNG bao giờ gọi trực tiếp DAO, mà luôn qua Repository.
 * - Tất cả các thao tác IO đều được chuyển qua [Dispatchers.IO] để tối ưu hiệu suất.
 * - Flow từ DAO được giữ nguyên để giữ tính reactive (UI tự cập nhật).
 */
class ClothingRepository(private val clothingDao: ClothingDao) {

    /**
     * 📋 Lấy danh sách tất cả items dưới dạng Flow.
     *
     * ⚡ Reactive: Khi có bất kỳ thay đổi nào trong DB, Flow tự động emit dữ liệu mới.
     * ViewModel sẽ collect Flow này, UI sẽ tự động cập nhật.
     */
    fun getAllItems(): Flow<List<ClothingItemEntity>> {
        return clothingDao.getAllItems()
    }

    /**
     * 📋 Lấy danh sách items theo category dưới dạng Flow.
     * Dùng cho chức năng lọc theo danh mục trên giao diện tủ đồ.
     */
    fun getItemsByCategory(category: String): Flow<List<ClothingItemEntity>> {
        return clothingDao.getItemsByCategory(category)
    }

    /**
     * ➕ Thêm một item mới.
     *
     * Sử dụng [withContext(Dispatchers.IO)] để đảm bảo thao tác ghi DB
     * không làm block Main Thread — giữ UI luôn mượt.
     *
     * @param item Entity cần lưu vào database.
     */
    suspend fun insertItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        clothingDao.insertItem(item)
    }

    /**
     * ❌ Xoá một item.
     *
     * Tương tự insert, chạy trên [Dispatchers.IO] để không block Main Thread.
     *
     * @param item Entity cần xoá.
     */
    suspend fun deleteItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        clothingDao.deleteItem(item)
    }

    /**
     * 📊 Lấy số lượng items trong một category dưới dạng Flow.
     * Dùng cho badge đếm trên chip category.
     */
    fun getItemCountByCategory(category: String): Flow<Int> {
        return clothingDao.getItemCountByCategory(category)
    }

    /**
     * 📊 Lấy tổng số lượng items dưới dạng Flow.
     */
    fun getTotalItemCount(): Flow<Int> {
        return clothingDao.getTotalItemCount()
    }

    /**
     * 🔍 Lấy chi tiết một item theo ID.
     *
     * @param itemId UUID của item.
     * @return ClothingItemEntity hoặc null nếu không tìm thấy.
     */
    suspend fun getItemById(itemId: String): ClothingItemEntity? = withContext(Dispatchers.IO) {
        clothingDao.getItemById(itemId)
    }
}
