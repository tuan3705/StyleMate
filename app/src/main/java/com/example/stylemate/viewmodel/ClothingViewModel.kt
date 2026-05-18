package com.example.stylemate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.repository.ClothingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 🧩 ClothingViewModel — ViewModel cho màn hình Quản lý tủ đồ.
 *
 * Tuân thủ kiến trúc MVVM:
 * - ViewModel giao tiếp với Repository (không gọi DAO trực tiếp).
 * - Dùng [StateFlow] để UI observe dữ liệu một cách reactive.
 * - Dùng [viewModelScope] + [Dispatchers.IO] để xử lý bất đồng bộ.
 *
 * 🔄 Luồng dữ liệu:
 *   UI (Compose) ← collect StateFlow ← ViewModel ← Repository ← Room DAO ← SQLite
 */
class ClothingViewModel(
    private val repository: ClothingRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ClothingViewModel"
        // Thời gian giả lập xử lý tách nền (ms)
        private const val MOCK_BG_REMOVAL_DELAY_MS = 2000L
    }

    // ──────────────────────────────────────────────────────────────
    // 🔷 Reactive State: Danh sách items (có hỗ trợ lọc theo category)
    // ──────────────────────────────────────────────────────────────

    /** Category đang được chọn để lọc. "All" = hiển thị tất cả. */
    private val _selectedCategory = MutableStateFlow("All")

    /**
     * 🔄 Trigger để force refresh danh sách items.
     * Mỗi lần thêm/xoá item, ta tăng giá trị này lên → flatMapLatest được kích hoạt lại.
     */
    private val _refreshTrigger = MutableStateFlow(0L)

    /**
     * Category đang được chọn — UI collect để highlight chip tương ứng.
     */
    val selectedCategory: StateFlow<String> = _selectedCategory

    /**
     * Danh sách items dạng [StateFlow], tự động lọc theo [_selectedCategory].
     *
     * Sử dụng [flatMapLatest] để mỗi khi category thay đổi, Flow cũ bị huỷ
     * và Flow mới (từ DAO) được subscribe — tiết kiệm tài nguyên, tránh memory leak.
     *
     * [stateIn] chuyển Flow lạnh thành StateFlow nóng:
     * - `WhileSubscribed(5000)` giữ subscription 5s sau khi không còn collector,
     *   tránh restart không cần thiết khi xoay màn hình.
     * - Giá trị khởi tạo: emptyList().
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<ClothingItemEntity>> = _refreshTrigger
        .flatMapLatest { _ ->
            _selectedCategory.value.let { category ->
            if (category == "All") {
                repository.getAllItems()
            } else {
                repository.getItemsByCategory(category)
            }
        }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ──────────────────────────────────────────────────────────────
    // 🔷 Loading State: Trạng thái đang xử lý
    // ──────────────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)

    /**
     * Trạng thái loading — UI có thể dùng để hiển thị ProgressBar/spinner
     * khi người dùng thêm item mới (đang giả lập tách nền).
     */
    val isLoading: StateFlow<Boolean> = _isLoading

    // ──────────────────────────────────────────────────────────────
    // 🔷 Error State: Trạng thái lỗi
    // ──────────────────────────────────────────────────────────────

    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * Thông báo lỗi (nếu có) — UI có thể hiển thị Snackbar/Toast.
     * Được reset về null sau mỗi lần đọc hoặc sau một thao tác thành công.
     */
    val errorMessage: StateFlow<String?> = _errorMessage

    // ──────────────────────────────────────────────────────────────
    // ➕ Thêm item mới (có giả lập tách nền)
    // ──────────────────────────────────────────────────────────────

    /**
     * Thêm một clothing item mới vào tủ đồ.
     *
     * 📸 Quy trình xử lý:
     * 1. Bật loading state (UI hiển thị spinner).
     * 2. Giả lập xử lý tách nền bằng [delay] — mô phỏng API call hoặc xử lý ảnh.
     * 3. Tạo entity mới với ID (UUID) + đầy đủ thông tin chi tiết.
     * 4. Lưu vào database qua Repository (trên [Dispatchers.IO]).
     * 5. Tắt loading, UI tự động cập nhật nhờ Flow reactive.
     *
     * 🧵 Coroutines: Toàn bộ chạy trong [viewModelScope] — tự động huỷ nếu ViewModel bị clear.
     *
     * @param imageFile File ảnh gốc người dùng chụp/chọn từ thư viện.
     * @param category Danh mục quần áo (vd: "Tops", "Bottoms").
     * @param color Màu sắc chính của item.
     * @param name Tên món đồ (vd: "Áo sơ mi trắng").
     * @param season Mùa phù hợp ("Spring", "Summer", "Autumn", "Winter").
     * @param occasion Dịp sử dụng ("Casual", "Work", "Sports", "Formal").
     * @param brand Thương hiệu (vd: "Nike", "Uniqlo", "Adidas").
     * @param purchaseDate Ngày mua (timestamp epoch millis).
     * @param price Giá tiền.
     */
    fun addClothingItem(
        imageFile: File,
        category: String,
        color: String,
        name: String,
        season: String,
        occasion: String,
        brand: String,
        purchaseDate: Long,
        price: Double
    ) {
        viewModelScope.launch {
            try {
                // ── Bước 1: Bật loading ──────────────────────────
                _isLoading.value = true
                _errorMessage.value = null

                Log.d(TAG, "📸 Bắt đầu xử lý ảnh: ${imageFile.name}")

                // ── Bước 2: Giả lập tách nền (mock API call) ────
                // 💡 Mô phỏng thời gian gọi API dịch vụ tách nền bên thứ ba
                // (remove.bg, Adobe API, hoặc model ML local).
                // Trong thực tế, đây sẽ là network call hoặc xử lý bitmap.
                withContext(Dispatchers.Default) {
                    delay(MOCK_BG_REMOVAL_DELAY_MS)
                }

                // ── Bước 3: Tạo đường dẫn ảnh đã tách nền (fake) ─
                // 💡 Trong thực tế, ảnh không nền sẽ được lưu từ API response.
                // Ở đây ta tạo một đường dẫn fake để minh hoạ luồng xử lý.
                val noBgPath = imageFile.absolutePath.replace(
                    imageFile.name,
                    "no_bg_${imageFile.name}"
                )

                // ── Bước 4: Tạo entity mới với đầy đủ thông tin ───
                val newItem = ClothingItemEntity(
                    id = UUID.randomUUID().toString(),
                    imageOriginal = imageFile.absolutePath,
                    imageNoBg = noBgPath,
                    category = category,
                    color = color,
                    name = name,
                    season = season,
                    occasion = occasion,
                    brand = brand,
                    purchaseDate = purchaseDate,
                    price = price,
                    canvasPosX = 0.5f,
                    canvasPosY = 0.1f,
                    createdAt = System.currentTimeMillis()
                )

                Log.d(TAG, "✅ Tách nền hoàn tất. ID: ${newItem.id}, Tên: ${newItem.name}")

                // ── Bước 5: Lưu vào database qua Repository ──────
                repository.insertItem(newItem)

                // 🔄 Refresh danh sách để UI tự động cập nhật
                _refreshTrigger.value = System.currentTimeMillis()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi thêm item: ${e.message}", e)
                _errorMessage.value = "Không thể thêm item: ${e.message}"
            } finally {
                // ── Bước 6: Tắt loading ──────────────────────────
                _isLoading.value = false
            }
        }
    }

    /**
     * ❌ Xoá một clothing item.
     *
     * @param item Entity cần xoá.
     */
    fun deleteClothingItem(item: ClothingItemEntity) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                // 🔄 Refresh danh sách sau khi xoá
                _refreshTrigger.value = System.currentTimeMillis()
                Log.d(TAG, "🗑️ Đã xoá item: ${item.id}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi xoá item: ${e.message}", e)
                _errorMessage.value = "Không thể xoá item: ${e.message}"
            }
        }
    }

    /**
     * 📊 Lấy số lượng items trong một category cụ thể dưới dạng Flow.
     * UI sẽ collect Flow này để hiển thị badge đếm bên cạnh mỗi chip category.
     *
     * Sử dụng Repository để đảm bảo luồng dữ liệu reactive:
     * khi thêm/xoá item, Room tự động emit lại giá trị mới → UI cập nhật badge.
     */
    fun getItemCountByCategory(category: String): Flow<Int> {
        return if (category == "All") {
            repository.getTotalItemCount()
        } else {
            repository.getItemCountByCategory(category)
        }
    }

    /**
     * 🏷️ Chọn category để lọc danh sách.
     * Gọi từ UI khi người dùng tap vào chip category.
     */
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    /**
     * Xoá thông báo lỗi sau khi UI đã xử lý (vd: đóng Snackbar).
     */
    fun clearError() {
        _errorMessage.value = null
    }

    fun updateItemCanvasPosition(itemId: String, posX: Float, posY: Float) {
        viewModelScope.launch {
            try {
                repository.updateItemCanvasPosition(itemId, posX, posY)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi cập nhật vị trí item: ${e.message}", e)
                _errorMessage.value = "Không thể cập nhật vị trí item: ${e.message}"
            }
        }
    }
}

/**
 * 🏭 ClothingViewModelFactory — Factory pattern để inject [ClothingRepository] vào ViewModel.
 *
 * Tuân thủ [ViewModelProvider.Factory] — cho phép truyền dependency
 * mà không cần Dagger/Hilt (thuần Kotlin, phù hợp project hiện tại).
 *
 * @param repository Repository instance (sẽ được tạo từ AppDatabase trong Activity/Fragment).
 */
class ClothingViewModelFactory(
    private val repository: ClothingRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClothingViewModel::class.java)) {
            return ClothingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
