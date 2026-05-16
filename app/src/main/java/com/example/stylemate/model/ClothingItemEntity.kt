package com.example.stylemate.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 📦 ClothingItemEntity — Entity Room chính cho module Quản lý tủ đồ.
 *
 * Ánh xạ từ bảng "clothing_items" trong SQLite qua Room.
 * Tuân thủ đúng sơ đồ lớp PlantUML đã thiết kế.
 *
 * @property id Mã định danh duy nhất (UUID string) — dùng String để linh hoạt khi đồng bộ API sau này.
 * @property imageOriginal Đường dẫn (URI/local path) tới ảnh gốc người dùng chụp/tải lên.
 * @property imageNoBg Đường dẫn (URI/local path) tới ảnh đã được tách nền.
 * @property category Danh mục quần áo: "Tops", "Bottoms", "Footwear", ...
 * @property color Màu sắc chính của item.
 * @property name Tên món đồ (vd: "Áo sơ mi trắng", "Quần jeans xanh").
 * @property season Mùa phù hợp: "Spring", "Summer", "Autumn", "Winter".
 * @property occasion Dịp sử dụng: "Casual", "Work", "Sports", "Formal".
 * @property brand Thương hiệu (vd: "Nike", "Uniqlo").
 * @property purchaseDate Ngày mua (timestamp epoch millis) — dùng cho thống kê/lịch sử.
 * @property price Giá tiền (Double) — dùng cho tính năng thống kê chi tiêu.
 * @property createdAt Timestamp (epoch millis) lưu thời điểm tạo item — dùng cho sắp xếp.
 */
@Entity(tableName = "clothing_items")
data class ClothingItemEntity(
    @PrimaryKey
    val id: String,
    val imageOriginal: String,
    val imageNoBg: String,
    val category: String,
    val color: String,
    // ── Các trường mới bổ sung ──────────────────────────────
    val name: String = "",
    val season: String = "",
    val occasion: String = "",
    val brand: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val price: Double = 0.0,
    // ────────────────────────────────────────────────────────
    val createdAt: Long = System.currentTimeMillis()
)
