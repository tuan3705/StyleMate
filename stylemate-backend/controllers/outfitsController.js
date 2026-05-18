/**
 * 👔 Outfits Controller
 * 
 * Xử lý tất cả logic CRUD cho Outfit.
 * 
 * ⚠️ QUAN TRỌNG: Khi DELETE outfit, tự động xoá các CalendarEvent
 * có outfitId tương ứng (CASCADE logic).
 */
const Outfit = require('../models/Outfit');
const CalendarEvent = require('../models/CalendarEvent');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');

/**
 * 📋 GET /api/outfits
 * 
 * Lấy danh sách tất cả Outfits.
 * Mỗi Outfit kèm mảng clothingItems chứa { clothingItemId, posX, posY }.
 * 
 * Query params:
 *   - populate=true: Nếu true, thay clothingItemId bằng dữ liệu đầy đủ của ClothingItem
 * 
 * Response: { success: true, count: Number, data: [...] }
 */
const getAllOutfits = asyncHandler(async (req, res) => {
  const { populate } = req.query;

  let outfits;

  if (populate === 'true') {
    // Aggregate để join với ClothingItem collection
    outfits = await Outfit.aggregate([
      { $sort: { createdAt: -1 } },
      {
        $lookup: {
          from: 'clothingitems', // Tên collection MongoDB (tự động lowercase + plural)
          localField: 'clothingItems.clothingItemId',
          foreignField: '_id',
          as: 'populatedClothingItems'
        }
      },
      {
        $addFields: {
          clothingItems: {
            $map: {
              input: '$clothingItems',
              as: 'ci',
              in: {
                $mergeObjects: [
                  '$$ci',
                  {
                    clothingItem: {
                      $arrayElemAt: [
                        {
                          $filter: {
                            input: '$populatedClothingItems',
                            cond: { $eq: ['$$this._id', '$$ci.clothingItemId'] }
                          }
                        },
                        0
                      ]
                    }
                  }
                ]
              }
            }
          }
        }
      },
      { $project: { populatedClothingItems: 0 } }
    ]);
  } else {
    outfits = await Outfit.find().sort({ createdAt: -1 });
  }

  res.status(200).json({
    success: true,
    count: outfits.length,
    data: outfits
  });
});

/**
 * 🔍 GET /api/outfits/:id
 * 
 * Lấy chi tiết một Outfit theo ID.
 * 
 * Response: { success: true, data: { ... } }
 * Error 404: { success: false, message: "..." }
 */
const getOutfitById = asyncHandler(async (req, res, next) => {
  const { id } = req.params;

  const outfit = await Outfit.findById(id);

  if (!outfit) {
    return next(new AppError(`Không tìm thấy Outfit với ID: ${id}`, 404));
  }

  res.status(200).json({
    success: true,
    data: outfit
  });
});

/**
 * ➕ POST /api/outfits
 * 
 * Tạo một Outfit mới.
 * Body nhận:
 *   - _id: String (UUID do Client sinh)
 *   - name: String (bắt buộc)
 *   - clothingItems: Mảng [{ clothingItemId: String, posX: Number, posY: Number }]
 *   - createdAt: Number (optional)
 * 
 * Response 201: { success: true, data: { ... } }
 * Error 400: { success: false, message: "..." }
 */
const createOutfit = asyncHandler(async (req, res) => {
  const { _id, name, clothingItems, createdAt } = req.body;

  // Validation
  if (!_id) {
    return res.status(400).json({
      success: false,
      message: 'Trường _id (UUID) là bắt buộc'
    });
  }

  if (!name || name.trim() === '') {
    return res.status(400).json({
      success: false,
      message: 'Trường name (tên bộ đồ) là bắt buộc'
    });
  }

  // Chuẩn hoá clothingItems: gán posX, posY mặc định nếu thiếu
  const normalizedItems = Array.isArray(clothingItems)
    ? clothingItems.map((item, index) => ({
        clothingItemId: item.clothingItemId,
        posX: item.posX !== undefined ? item.posX : (index % 2 === 0 ? 0.1 : 0.55),
        posY: item.posY !== undefined ? item.posY : 0.1 + Math.floor(index / 2) * 0.25
      }))
    : [];

  const newOutfit = await Outfit.create({
    _id,
    name: name.trim(),
    clothingItems: normalizedItems,
    createdAt: createdAt || Date.now()
  });

  res.status(201).json({
    success: true,
    data: newOutfit
  });
});

/**
 * ✏️ PUT /api/outfits/:id
 * 
 * Cập nhật một Outfit.
 * Cho phép cập nhật name và/hoặc thay thế toàn bộ clothingItems.
 * 
 * Response: { success: true, data: { ... } }
 * Error 404: { success: false, message: "..." }
 */
const updateOutfit = asyncHandler(async (req, res, next) => {
  const { id } = req.params;

  const updateData = {};

  if (req.body.name !== undefined) {
    if (req.body.name.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'Tên bộ đồ không được để trống'
      });
    }
    updateData.name = req.body.name.trim();
  }

  if (req.body.clothingItems !== undefined) {
    if (!Array.isArray(req.body.clothingItems)) {
      return res.status(400).json({
        success: false,
        message: 'clothingItems phải là một mảng'
      });
    }
    updateData.clothingItems = req.body.clothingItems.map((item, index) => ({
      clothingItemId: item.clothingItemId,
      posX: item.posX !== undefined ? item.posX : (index % 2 === 0 ? 0.1 : 0.55),
      posY: item.posY !== undefined ? item.posY : 0.1 + Math.floor(index / 2) * 0.25
    }));
  }

  const updatedOutfit = await Outfit.findByIdAndUpdate(id, updateData, {
    new: true,
    runValidators: true
  });

  if (!updatedOutfit) {
    return next(new AppError(`Không tìm thấy Outfit với ID: ${id}`, 404));
  }

  res.status(200).json({
    success: true,
    data: updatedOutfit
  });
});

/**
 * ❌ DELETE /api/outfits/:id
 * 
 * Xoá một Outfit.
 * ⚠️ Tự động xoá các CalendarEvent có outfitId = id (CASCADE logic).
 * 
 * Response: { success: true, message: "...", deletedEvents: Number }
 * Error 404: { success: false, message: "..." }
 */
const deleteOutfit = asyncHandler(async (req, res, next) => {
  const { id } = req.params;

  const deletedOutfit = await Outfit.findByIdAndDelete(id);

  if (!deletedOutfit) {
    return next(new AppError(`Không tìm thấy Outfit với ID: ${id}`, 404));
  }

  // 🗑️ CASCADE: Xoá tất cả CalendarEvent liên quan đến outfit này
  const deleteResult = await CalendarEvent.deleteMany({ outfitId: id });

  res.status(200).json({
    success: true,
    message: `Đã xoá Outfit: ${id}`,
    data: {},
    cascadeDeletedCalendarEvents: deleteResult.deletedCount
  });
});

/**
 * 📋 GET /api/outfits/by-item/:clothingItemId
 * 
 * Lấy danh sách các Outfit có chứa một ClothingItem cụ thể.
 * 
 * Response: { success: true, count: Number, data: [...] }
 */
const getOutfitsContainingItem = asyncHandler(async (req, res) => {
  const { clothingItemId } = req.params;

  const outfits = await Outfit.find({
    'clothingItems.clothingItemId': clothingItemId
  }).sort({ createdAt: -1 });

  res.status(200).json({
    success: true,
    count: outfits.length,
    data: outfits
  });
});

module.exports = {
  getAllOutfits,
  getOutfitById,
  createOutfit,
  updateOutfit,
  deleteOutfit,
  getOutfitsContainingItem
};
