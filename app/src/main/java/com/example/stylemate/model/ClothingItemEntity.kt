package com.example.stylemate.model

/**
 * 📦 ClothingItemEntity — Data class đại diện cho một món quần áo.
 *
 * Trước đây là Entity Room, nay là POJO thuần (dùng chung cho ViewModel + API).
 *
 * @property id Mã định danh duy nhất (UUID string).
 * @property imageOriginal Đường dẫn (URI/local path) tới ảnh gốc.
 * @property imageNoBg Đường dẫn (URI/local path) tới ảnh đã được tách nền.
 * @property category Danh mục quần áo: "Tops", "Bottoms", "Footwear", ...
 * @property color Màu sắc chính của item.
 * @property name Tên món đồ.
 * @property season Mùa phù hợp: "Spring", "Summer", "Autumn", "Winter".
 * @property occasion Dịp sử dụng: "Casual", "Work", "Sports", "Formal".
 * @property brand Thương hiệu.
 * @property purchaseDate Ngày mua (timestamp epoch millis).
 * @property price Giá tiền.
 * @property createdAt Timestamp (epoch millis) thời điểm tạo.
 */
data class ClothingItemEntity(
    val id: String,
    val imageOriginal: String,
    val imageNoBg: String,
    val category: String,
    val color: String,
    val name: String = "",
    val season: String = "",
    val occasion: String = "",
    val brand: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val price: Double = 0.0,
    val canvasPosX: Float = 0.5f,
    val canvasPosY: Float = 0.5f,
    val canvasScale: Float = 1f,
    val createdAt: Long = System.currentTimeMillis()
)
