/**
 * 👕 Clothes Controller
 * 
 * Xử lý tất cả các logic CRUD cho ClothingItem.
 * Mỗi hàm là một async handler, được wrap bởi asyncHandler
 * để tự động bắt lỗi.
 */
const ClothingItem = require('../models/ClothingItem');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');

/**
 * 📋 GET /api/clothes
 * 
 * Lấy danh sách tất cả ClothingItems.
 * Hỗ trợ query params:
 *   - category: lọc theo danh mục (vd: ?category=Tops)
 *   - sort: sắp xếp (vd: ?sort=-createdAt)
 * 
 * Response: { success: true, count: Number, data: [...] }
 */
const getAllClothes = asyncHandler(async (req, res) => {
  const { category, sort } = req.query;

  // Xây dựng filter
  const filter = {};
  if (category && category !== 'All') {
    filter.category = category;
  }

  // Xây dựng sort — mặc định mới nhất trước
  let sortOption = { createdAt: -1 };
  if (sort) {
    const sortFields = sort.split(',').reduce((acc, field) => {
      if (field.startsWith('-')) {
        acc[field.substring(1)] = -1;
      } else {
        acc[field] = 1;
      }
      return acc;
    }, {});
    sortOption = sortFields;
  }

  const items = await ClothingItem.find(filter).sort(sortOption);

  res.status(200).json({
    success: true,
    count: items.length,
    data: items
  });
});

/**
 * 🔍 GET /api/clothes/:id
 * 
 * Lấy chi tiết một ClothingItem theo ID (UUID string).
 * 
 * Response: { success: true, data: { ... } }
 * Error 404: { success: false, message: "..." }
 */
const getClothingItemById = asyncHandler(async (req, res, next) => {
  const { id } = req.params;

  const item = await ClothingItem.findById(id);

  if (!item) {
    return next(new AppError(`Không tìm thấy ClothingItem với ID: ${id}`, 404));
  }

  res.status(200).json({
    success: true,
    data: item
  });
});

/**
 * ➕ POST /api/clothes
 * 
 * Tạo một ClothingItem mới.
 * Body nhận toàn bộ fields của ClothingItemEntity (kể cả _id).
 * 
 * Response 201: { success: true, data: { ... } }
 * Error 400: { success: false, message: "Lỗi validation: ..." }
 */
const createClothingItem = asyncHandler(async (req, res) => {
  const {
    _id,
    imageOriginal,
    imageNoBg,
    category,
    color,
    name,
    season,
    occasion,
    brand,
    purchaseDate,
    price,
    canvasPosX,
    canvasPosY,
    createdAt
  } = req.body;

  // Kiểm tra ID bắt buộc
  if (!_id) {
    return res.status(400).json({
      success: false,
      message: 'Trường _id (UUID) là bắt buộc'
    });
  }

  // Kiểm tra trùng ID
  const existingItem = await ClothingItem.findById(_id);
  if (existingItem) {
    // Nếu đã tồn tại → upsert (REPLACE)
    const updatedItem = await ClothingItem.findByIdAndUpdate(
      _id,
      {
        imageOriginal: imageOriginal || existingItem.imageOriginal,
        imageNoBg: imageNoBg || existingItem.imageNoBg,
        category: category || existingItem.category,
        color: color || existingItem.color,
        name: name !== undefined ? name : existingItem.name,
        season: season !== undefined ? season : existingItem.season,
        occasion: occasion !== undefined ? occasion : existingItem.occasion,
        brand: brand !== undefined ? brand : existingItem.brand,
        purchaseDate: purchaseDate !== undefined ? purchaseDate : existingItem.purchaseDate,
        price: price !== undefined ? price : existingItem.price,
        canvasPosX: canvasPosX !== undefined ? canvasPosX : existingItem.canvasPosX,
        canvasPosY: canvasPosY !== undefined ? canvasPosY : existingItem.canvasPosY,
        createdAt: createdAt || existingItem.createdAt
      },
      { new: true, runValidators: true }
    );

    return res.status(200).json({
      success: true,
      message: 'Item đã tồn tại, đã cập nhật (upsert)',
      data: updatedItem
    });
  }

  // Tạo mới
  const newItem = await ClothingItem.create({
    _id,
    imageOriginal: imageOriginal || '',
    imageNoBg: imageNoBg || '',
    category: category || 'Tops',
    color: color || '',
    name: name || '',
    season: season || '',
    occasion: occasion || '',
    brand: brand || '',
    purchaseDate: purchaseDate || 0,
    price: price || 0.0,
    canvasPosX: canvasPosX !== undefined ? canvasPosX : 0.5,
    canvasPosY: canvasPosY !== undefined ? canvasPosY : 0.5,
    createdAt: createdAt || Date.now()
  });

  res.status(201).json({
    success: true,
    data: newItem
  });
});

/**
 * ✏️ PUT /api/clothes/:id
 * 
 * Cập nhật một ClothingItem theo ID.
 * Chỉ cập nhật các field được gửi lên (partial update).
 * 
 * Response: { success: true, data: { ... } }
 * Error 404: { success: false, message: "..." }
 */
const updateClothingItem = asyncHandler(async (req, res, next) => {
  const { id } = req.params;

  const updateData = {};
  const allowedFields = [
    'imageOriginal', 'imageNoBg', 'category', 'color', 'name',
    'season', 'occasion', 'brand', 'purchaseDate', 'price',
    'canvasPosX', 'canvasPosY'
  ];

  // Chỉ lấy những field được phép từ req.body
  allowedFields.forEach((field) => {
    if (req.body[field] !== undefined) {
      updateData[field] = req.body[field];
    }
  });

  const updatedItem = await ClothingItem.findByIdAndUpdate(id, updateData, {
    new: true,           // Trả về document đã cập nhật
    runValidators: true  // Kiểm tra validation
  });

  if (!updatedItem) {
    return next(new AppError(`Không tìm thấy ClothingItem với ID: ${id}`, 404));
  }

  res.status(200).json({
    success: true,
    data: updatedItem
  });
});

/**
 * ❌ DELETE /api/clothes/:id
 * 
 * Xoá một ClothingItem theo ID.
 * 
 * Response: { success: true, data: {} }
 * Error 404: { success: false, message: "..." }
 */
const deleteClothingItem = asyncHandler(async (req, res, next) => {
  const { id } = req.params;

  const deletedItem = await ClothingItem.findByIdAndDelete(id);

  if (!deletedItem) {
    return next(new AppError(`Không tìm thấy ClothingItem với ID: ${id}`, 404));
  }

  res.status(200).json({
    success: true,
    message: `Đã xoá ClothingItem: ${id}`,
    data: {}
  });
});

module.exports = {
  getAllClothes,
  getClothingItemById,
  createClothingItem,
  updateClothingItem,
  deleteClothingItem
};
