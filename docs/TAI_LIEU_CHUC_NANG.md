# Tài liệu chức năng — StyleMate (Tủ đồ & Phối đồ)

> Tài liệu mô tả vị trí code (file + dòng), thành phần, luồng hoạt động và các tham số quan trọng của từng chức năng.
> Cập nhật theo nhánh `feature/ai-stylist`.

Quy ước viết tắt:
- **FE** = Android app (Jetpack Compose, Kotlin) trong `app/src/main/java/com/example/stylemate/`
- **BE** = Backend Node.js/Express trong `stylemate-backend/`

---

## 1. Add Item (Thêm món đồ)

Có **2 đường vào** cho cùng một logic thêm món đồ:

| Đường vào | File | Hàm | Dòng |
|---|---|---|---|
| Màn hình đầy đủ | `ui/screens/AddItemScreen.kt` | `AddItemScreen()` | 44–530 |
| Quick Add (Bottom Sheet trong tab Món đồ) | `ui/screens/ClosetScreen.kt` | `NewClothingItemSheet()` | 1672–2135 |

**Thành phần form (giống nhau ở cả 2 nơi):**
- Chọn ảnh: `ImagePickerSection` (camera / gallery / xoá nền) — `AddItemScreen.kt:209`, `ClosetScreen.kt:1796`
- 2 nút AI: Auto Tagging + Fill with AI (xem mục 8)
- Category (dropdown), Color, Item name, Brand, Price, Season (FilterChip), Occasion (FilterChip), Purchase date (DatePicker)
- Nút submit: `AddItemScreen.kt:433` ("Add to closet") / `ClosetScreen.kt:2069` ("Add item")

**Validate trước khi lưu** (`AddItemScreen.kt:435–446`): bắt buộc có ảnh, category, color, itemName. Quick Add chỉ bắt buộc category + color (`ClosetScreen.kt:2071, 2090`).

**Luồng hoạt động:**
1. UI gọi `ClothingViewModel.addClothingItem(...)` — `viewmodel/ClothingViewModel.kt:111–157`.
2. ViewModel tạo `ClothingItemEntity` với `id = UUID.randomUUID()`, set `imageNoBg` nếu đã xoá nền (`bgRemoved`), rồi `repository.insertItem(newItem)`.
3. Sau khi insert → `_refreshTrigger.value = System.currentTimeMillis()` để các flow (`items`, `allItems`) tự refetch.
4. Backend lưu qua `repository/ClothingRepository.kt` → API.

**Tham số quan trọng (`addClothingItem`, `ClothingViewModel.kt:111`):**
- `imageFile: File?` — ảnh gốc; `absolutePath` được gán vào `imageOriginal`.
- `bgRemoved: Boolean = false` — nếu `true` thì `imageNoBg = imagePath`, ảnh trong tủ sẽ ưu tiên hiển thị bản đã xoá nền. ⚠️ Lưu ý: ở `AddItemScreen` không truyền `bgRemoved` (mặc định `false`), chỉ `NewClothingItemSheet` truyền (`ClosetScreen.kt:2074, 2085`).
- `canvasPosX = 0.5f`, `canvasPosY = 0.1f` — vị trí mặc định trên canvas (hard-code tại `ClothingViewModel.kt:142–143`).
- `purchaseDate: Long` — epoch millis; DatePicker chỉ cho chọn ngày ≤ hiện tại (`AddItemScreen.kt:502`).

---

## 2. Add Outfit (Thêm/Tạo bộ đồ)

Có **2 luồng** liên quan đến tạo/chỉnh sửa bộ đồ:

### 2a. Tạo outfit mới (Bottom Sheet)
- File: `ui/screens/ClosetScreen.kt`
- Hàm: `CreateOutfitBottomSheetContent()` — dòng **1204–1353**
- Mở qua FAB khi đang ở tab "Bộ đồ" (`ClosetScreen.kt:253` → `showCreateOutfitSheet = true`).

**Thành phần:**
- TextField tên bộ đồ (`:1243`)
- Badge đếm số item đã chọn (`:1267`)
- Grid 2 cột chọn item (toggle) — `SelectableItemCard` (`:1310`, định nghĩa tại `:1420`)
- Nút "Lưu" — chỉ enable khi `outfitName.isNotBlank() && draftItems.isNotEmpty() && !isLoading` (`:1336`)

**Luồng:**
1. Click item → `outfitVM.addClothingItemToDraft / removeClothingItemFromDraft` (`OutfitViewModel.kt:186, 204`) cập nhật `_draftOutfitItems`.
2. Nút Lưu → `outfitVM.saveOutfit(outfitName)` (`OutfitViewModel.kt:234–292`):
   - Validate tên + draft không rỗng.
   - Tạo `OutfitEntity(id = UUID, name, createdAt)`.
   - Map mỗi draft item → `OutfitClothingCrossRef` với vị trí grid mặc định `defaultGridPosition(index)` (`OutfitViewModel.kt:321–327`).
   - `repository.insertOutfit` + `repository.insertOutfitClothingCrossRefs` → bump `_refreshTrigger`.
3. UI phát hiện `draftItems` rỗng + hết loading → đóng sheet (`ClosetScreen.kt:1220–1224`).

### 2b. Thêm item vào outfit đang chỉnh sửa
- `AddItemsBottomSheet()` — `ClosetScreen.kt:1046–1120`. Confirm → `outfitVM.addItemsToEditing(...)` (`OutfitViewModel.kt:345–355`).

**Tham số quan trọng:**
- `defaultGridPosition(index)`: cột chẵn x=0.1, cột lẻ x=0.55; y tăng 0.25 mỗi 2 item, giới hạn ≤ 0.8 — quyết định vị trí khởi tạo trên canvas.
- `scale = 1f` mặc định cho mỗi item.

---

## 3. Canvas của Outfit

Có **3 biến thể canvas**, tất cả trong `ui/screens/ClosetScreen.kt` (lưu ý: số dòng có thể trôi sau các lần sửa — tra theo tên hàm):

| Biến thể | Hàm | Mục đích |
|---|---|---|
| Preview (chỉ xem) | `OutfitCanvasPreview()` | Thumbnail trong card outfit đã lưu (Box cao tối đa 220dp, canvas con canh giữa) |
| Editor (kéo thả + resize) | `OutfitCanvas()` | Canvas chỉnh sửa item |
| Dialog bao ngoài editor | `OutfitCanvasEditorDialog()` | Khung dialog chứa `OutfitCanvas` + nút Add/Save |

**Hình học dùng chung (đảm bảo editor & preview ĐỒNG DẠNG):** hai hằng số `OUTFIT_CANVAS_ASPECT_RATIO = 0.82f` (width/height) và `OUTFIT_ITEM_SIZE_FRACTION = 0.30f` (item width = 30% bề rộng canvas) — đặt cạnh `defaultOutfitGridPosition`. Cả hai canvas dùng `Modifier.aspectRatio(...)` + item size = `canvasWidth * fraction` nên bố cục chuẩn hoá 0..1 tái hiện y hệt giữa hai nơi (tránh item bị đè ở preview).

**Luồng hoạt động (editor):**
1. Mở từ tab Bộ đồ: click card → `outfitVM.startEditingOutfit(id, name)` (`OutfitViewModel.kt:294`) load items kèm vị trí + scale → `_editingItems`.
2. `OutfitCanvas` render từng item bằng `Modifier.offset` theo `posX/posY` (toạ độ chuẩn hoá 0..1) và `.size(itemSize * localScale)`.
3. **Kéo thả (di chuyển)**: `detectTapGestures` (chọn item) + `detectDragGestures` trên thân item — cập nhật `localPos` realtime; `onDragEnd` → `onPositionChange` → `outfitVM.updateEditingItemPosition` (`OutfitViewModel.kt:329`).
4. **Resize**: xem mục 3a.
5. **Chọn item**: viền vàng + nút xoá (X) đỏ góc trên-phải + handle resize (⇔) góc dưới-phải.
6. Lưu: `onSave` → `outfitVM.saveEditingOutfit()` (`OutfitViewModel.kt:361`) — xoá hết crossref cũ rồi ghi lại theo `posX/posY/scale` mới; set `_editSaveSuccess` → UI đóng dialog.

**Tham số quan trọng:**
- Toạ độ **chuẩn hoá 0..1**, không phải pixel: `offsetX = posX * maxX`, với `maxX = canvasWidth - scaledSizePx` (`scaledSizePx = itemSizePx * scale`). Đổi kích thước canvas vẫn giữ đúng vị trí tương đối.
- `OUTFIT_ITEM_SIZE_FRACTION` quyết định kích thước item gốc (chưa scale); `scale` của từng item nhân lên trên đó.
- Ảnh item lấy từ `rememberItemImageModel` — ưu tiên `imageNoBg`, fallback `imageOriginal`.

### 3a. Resize item trong editor

**Vị trí UI:** handle tròn màu vàng có ký hiệu `⇔` ở `Alignment.BottomEnd` của item, **chỉ hiện khi item được chọn** (trong block `if (isSelected)` của `OutfitCanvas`).

**Luồng:**
1. Kéo handle → `detectDragGestures.onDrag`: `delta = (dragAmount.x + dragAmount.y) / itemSizePx`, `newScale = (localScale + delta).coerceIn(0.4f, 2f)`; giữ nguyên vị trí pixel hiện tại rồi **tái chuẩn hoá `localPos`** theo `maxX/maxY` mới để item không nhảy/tràn khi đổi size.
2. `onDragEnd` → `onScaleChange(item.id, scale)` + `onPositionChange(...)` → `outfitVM.updateEditingItemScale` (`OutfitViewModel.kt:337`) + `updateEditingItemPosition`.
3. Save persist `scale` qua `OutfitClothingCrossRef.scale` → backend; load lại đúng qua `getOutfitItemsWithPosition` → `OutfitItemWithPosition.scale` → `mapWithDefaults`.

**Tham số quan trọng:**
- `minScale = 0.4f`, `maxScale = 2.0f` — giới hạn phóng to/thu nhỏ (khai báo đầu hàm `OutfitCanvas`).
- Scale **theo từng outfit-instance**, không phải thuộc tính toàn cục của item (lưu ở crossref, không ở `ClothingItemEntity` gốc).

**Đường đi của scale tới Preview:** vì `OutfitWithClothingItems.clothingItems` là `List<ClothingItemEntity>` (không mang scale của crossref), scale được "đi nhờ" qua field `ClothingItemEntity.canvasScale` — `OutfitRepository.toFullOutfitWithItems` gán `canvasScale = ref.scale`, rồi `mapClothingItemsToPlacements` đọc `it.canvasScale`, và `OutfitCanvasPreview` áp `placement.scale` vào `.size()` + `maxX/maxY`. Nhờ đồng dạng hình học, kích thước item hiển thị khớp giữa editor và preview.

---

## 4. Nút xoá Item

**Vị trí UI:** Icon thùng rác (`Icons.Default.Delete`) góc trên-phải mỗi `ClothingItemCard`.
- `ClothingItemCard()` — `ui/screens/ClosetScreen.kt:1534`, nút xoá tại **1606–1618**.
- (Một biến thể khác trong `SelectableItemCard` tại `:1510–1524`, chỉ hiện khi truyền `onDelete`.)

**Luồng:**
1. Click → `onDelete` → `ItemsTabContent.onDeleteItem` → `ClosetScreen.kt:343–347`:
   ```
   clothingVM.deleteClothingItem(item) { outfitVM.refreshAfterItemChange() }
   ```
2. `ClothingViewModel.deleteClothingItem` (`ClothingViewModel.kt:196–208`): `repository.deleteItem(item)` → bump `_refreshTrigger` → gọi `onComplete`.
3. Callback `outfitVM.refreshAfterItemChange()` (`OutfitViewModel.kt:105–108`): xoá cache clothes + bump trigger để các outfit/preview chứa item đó cập nhật (tránh hiển thị item đã xoá).

**Tham số quan trọng:**
- `onComplete: () -> Unit` — đảm bảo outfit liên quan được refresh **sau** khi xoá item; nếu bỏ callback này, preview outfit có thể còn item "mồ côi".
- Hiện **không có dialog xác nhận** — xoá ngay khi bấm.

---

## 5. Search bar (trong tab Món đồ)

**Vị trí UI:**
- Composable: `ClosetSearchBar()` — `ui/screens/ClosetScreen.kt:451–487`.
- Hiển thị có điều kiện: `AnimatedVisibility(visible = selectedTab == 0 && showSearchBar)` (`:291–305`).

**Cơ chế hiện/ẩn (quan trọng):** search bar **không phải lúc nào cũng hiện** — nó xuất hiện khi người dùng kéo grid xuống ở vị trí trên cùng:
- `itemsScrollConnection` (`NestedScrollConnection`, `:177–192`): khi đang ở đỉnh list (`isItemsAtTop`) và vuốt xuống (`available.y > 0`) → `showSearchBar = true`; vuốt lên → `false`.
- `isItemsAtTop` = derivedState dựa trên `firstVisibleItemIndex == 0 && offset == 0` (`:171–176`).
- `showSearchBar` lưu bằng `rememberSaveable` (`:161`).

**Luồng tìm kiếm (client-side, có debounce):**
1. Gõ chữ → `clothingVM.updateSearchQuery(query)` (`ClothingViewModel.kt:222`) cập nhật `_searchQuery`.
2. `debouncedSearchQuery`: trim → `debounce(250ms)` → `distinctUntilChanged` (`ClothingViewModel.kt:79–83`).
3. `filteredItems` = `combine(items, debouncedSearchQuery)` lọc bằng `matchesSearchQuery` trên `Dispatchers.Default` (`:85–100`).
4. `matchesSearchQuery` (`:250–273`): tách query thành tokens theo khoảng trắng, ghép `name + color + season + occasion + brand` (lowercase), trả `true` nếu **tất cả** token đều khớp (AND).

**Tham số quan trọng:**
- `debounce(250)` — độ trễ trước khi lọc.
- Tìm kiếm **kết hợp với filter category**: `filteredItems` lọc trên `items` (đã được lọc theo category), nên search chỉ áp dụng trong category đang chọn.
- Nút X (`:470–480`) → `clearSearchQuery()` + clear focus.

---

## 6. Thanh Sort theo Category (trong tab Món đồ)

**Vị trí UI:**
- `LazyRow` các `CategoryChip` — `ui/screens/ClosetScreen.kt:509–523` (trong `ItemsTabContent`).
- Composable chip: `CategoryChip()` — `:2142–2163` (hiển thị tên category + số lượng đếm).
- Danh sách category: `allCategories = listOf(Categories.ALL) + Categories.list` (`:169`).

**Luồng:**
1. Số lượng mỗi chip: `clothingVM.getItemCountByCategory(category)` (`ClothingViewModel.kt:210–216`) — nếu `ALL` đếm tổng, ngược lại đếm theo category.
2. Click chip → `clothingVM.selectCategory(category)` (`:218–220`) cập nhật `_selectedCategory`.
3. `items` flow phản ứng: `combine(_selectedCategory, _refreshTrigger)` → `flatMapLatest` (`ClothingViewModel.kt:58–74`):
   - `ALL` → `repository.getAllItems()`
   - khác → `repository.getItemsByCategory(category)`
4. `filteredItems` (search) chạy tiếp trên kết quả này.

**Tham số quan trọng:**
- Đây là **filter chọn 1** (không phải sort thật sự) — chỉ một category active tại một thời điểm, mặc định `Categories.ALL`.
- Chuyển category dùng `flatMapLatest` → request cũ bị huỷ khi đổi nhanh.

---

## 7. Xoá nền (Remove Background)

**FE — UI & trigger:**
- Nút "Remove BG" nằm trong `ImagePickerSection` (param `onRemoveBgClick`, `canRemoveBg`, `isProcessing`).
  - `AddItemScreen.kt`: handler `onRemoveBackgroundClick` (`:105–113`), gọi `:214`.
  - `NewClothingItemSheet`: handler `:1721–1729`, gọi `:1802`.
- `canRemoveBackground`: chỉ bật khi có ảnh và ảnh hiện tại **chưa** là bản vừa xoá nền (`AddItemScreen.kt:101–103`, `ClosetScreen.kt:1717–1719`) → tránh xoá nền 2 lần.

**FE — ViewModel/Repository:**
- `ImageProcessingViewModel.removeBackground(currentPath)` — `viewmodel/ImageProcessingViewModel.kt:42–53` (state `RemoveBgUiState`: `isProcessing/errorMessage/resultPath`).
- `ImageProcessingRepository.removeBackground(localPath)` — `repository/ImageProcessingRepository.kt:55–101`:
  - `resolveInputFile` (`:26–53`) chuẩn hoá path: `/uploads/...` → ghép `STYLEMATE_BASE_URL`; hỗ trợ `file://`, `content://`, `http(s)://`.
  - Đoán mime theo extension; gửi multipart field **`image`**.
  - `apiService.removeBackgroundImage` → lưu PNG kết quả vào internal storage (prefix `nobg_`), trả `absolutePath`.
- Kết quả → `LaunchedEffect(removeBgState.resultPath)` set lại ảnh trong picker và đánh dấu `lastRemoveBgPath/noBgPath` (`AddItemScreen.kt:136–143`, `ClosetScreen.kt:1731–1738`).

**BE:**
- Route: `POST /api/images/remove-bg` — `routes/imageRoutes.js`, field `image` (`upload.single('image')`).
- Controller: `controllers/removeBgController.js:26–39` — trả thẳng PNG (`Content-Type: image/png`, `Cache-Control: no-store`).
- Service: `services/removeBgService.js:29–58` — gọi **remove.bg API** (`https://api.remove.bg/v1.0/removebg`).

**Tham số quan trọng:**
- `REMOVE_BG_API_KEY` (env) — bắt buộc, thiếu → lỗi 500 (`removeBgService.js:30–33`).
- Form gửi remove.bg: `size: 'auto'`, `format: 'png'` (`:39–40`).
- Giới hạn upload: `fileSize 10MB` (multer, `removeBgController.js:23`); timeout 30s, maxContent 20MB (`removeBgService.js:6, 50–51`).
- Field multipart **phải tên `image`** (cả FE và BE) — đổi tên sẽ làm hỏng upload.

---

## 8. Hai nút "Auto Tagging" và "Fill with AI"

Cả 2 nút nằm cạnh nhau, dùng chung `ImageProcessingViewModel`. Vị trí UI:
- `AddItemScreen.kt:219–276` (Auto Tagging `:223`, Fill with AI `:247`).
- `NewClothingItemSheet` (`ClosetScreen.kt:1821–1875`).

Cả 2 đều yêu cầu đã chọn ảnh (nếu chưa → snackbar lỗi). Khi đang xử lý hiện `CircularProgressIndicator` + đổi nhãn ("Tagging…"/"Filling…").

### 8a. Auto Tagging → gợi ý **Season + Occasion**
**Luồng:**
1. Click → `imageProcessingViewModel.autoTagging(currentPath)` (`AddItemScreen.kt:229`; VM `ImageProcessingViewModel.kt:82–93`, state `AutoTaggingUiState`).
2. `ImageProcessingRepository.autoTagging` (`:144–181`) → multipart field `image` → `apiService.autoTaggingFromImage` (`StylemateApiService.kt:116–120`, `POST api/images/auto-tagging`).
3. Kết quả `AiAutoTaggingSuggestionDto { season, occasion, tags }` (`ApiModels.kt:221–225`).
4. `LaunchedEffect(autoTaggingState.suggestion)` (`AddItemScreen.kt:171–178`) tự điền `selectedSeason`, `selectedOccasion`.

**BE:** `controllers/aiAutoTaggingController.js:27–49` → `services/aiAutoTaggingService.js` (endpoint mặc định `https://aiautotagging.com/api/tag/image`) → mapper `aiAutoTaggingMapper.js`.
- Params service: `options = { maxTags: 12, includeConfidence: true, additionalContext }`; `additionalContext` ép model phân loại theo Spring/Summer/Autumn/Winter & Casual/Work/Sports/Formal (`aiAutoTaggingController.js:32–33`).
- Env: `AI_AUTOTAGGING_ENDPOINT`, `AI_AUTOTAGGING_API_KEY` (key tuỳ chọn).

### 8b. Fill with AI → gợi ý **Category + Color + Name**
**Luồng:**
1. Click → `imageProcessingViewModel.fillWithAi(currentPath)` (`AddItemScreen.kt:253`; VM `:62–73`, state `AiFillUiState`).
2. `ImageProcessingRepository.fillWithAi` (`:103–142`) → field `image` → `apiService.aiFillFromImage` (`POST api/images/ai-fill`).
3. Kết quả `AiFillSuggestionDto { category, color, name, ...confidence, candidates }` (`ApiModels.kt:205–214`).
4. `LaunchedEffect(aiFillState.suggestion)` (`AddItemScreen.kt:153–161`) tự điền `category`, `color`, `itemName`.

**BE:** `controllers/aiFillController.js:27–46` → `services/lykdatTaggingService.js` (gọi **Lykdat** `https://cloudapi.lykdat.com/v1/detection/tags`) → mapper `lykdatTaggingMapper.js`.
- Env: `LYKDAT_TAGGING_API_KEY` (bắt buộc), `LYKDAT_TAGGING_ENDPOINT` (tuỳ chọn). Timeout 15s.

**Tham số / lưu ý quan trọng cho cả 2:**
- Chỉ **điền gợi ý vào form**, KHÔNG tự lưu item — người dùng vẫn phải bấm nút Add.
- Hai nút độc lập (state riêng): Auto Tagging chỉ đụng season/occasion; Fill with AI chỉ đụng category/color/name → có thể chạy cả hai.
- `enabled = !isProcessing` chặn double-tap; mỗi lần xong gọi `clear...Result()` để reset state (`VM:95–100`, `:75–80`).
- Field multipart đều tên **`image`**; backend dùng `upload.single('image')`, giới hạn 10MB.
- Phụ thuộc API bên thứ ba (remove.bg, Lykdat, aiautotagging) → cần API key trong `.env` của backend, lỗi key/timeout sẽ trả message hiển thị qua snackbar.

---

## Phụ lục — Bản đồ file nhanh

| Lớp | File |
|---|---|
| Màn hình thêm item đầy đủ | `app/.../ui/screens/AddItemScreen.kt` |
| Tủ đồ + Phối đồ + Quick Add + Canvas | `app/.../ui/screens/ClosetScreen.kt` |
| Logic item (add/delete/search/category) | `app/.../viewmodel/ClothingViewModel.kt` |
| Logic outfit (draft/save/edit canvas) | `app/.../viewmodel/OutfitViewModel.kt` |
| Logic xoá nền + 2 nút AI | `app/.../viewmodel/ImageProcessingViewModel.kt` + `app/.../repository/ImageProcessingRepository.kt` |
| Khai báo API | `app/.../network/StylemateApiService.kt`, `app/.../network/ApiModels.kt` |
| BE xoá nền | `controllers/removeBgController.js` + `services/removeBgService.js` |
| BE Fill with AI | `controllers/aiFillController.js` + `services/lykdatTaggingService.js` |
| BE Auto Tagging | `controllers/aiAutoTaggingController.js` + `services/aiAutoTaggingService.js` |
| BE routes ảnh | `routes/imageRoutes.js` |
</content>
</invoke>