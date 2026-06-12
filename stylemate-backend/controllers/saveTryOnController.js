/**
 * 🖼️ Save Try-On Result to Collection Controller
 *
 * Cho phép người dùng lưu ảnh kết quả từ virtual try-on
 * vào bộ sưu tập ClothingItem của họ (tủ đồ).
 *
 * Yêu cầu: User phải được xác thực (requireAuth).
 *
 * POST /api/ai-stylist/virtual-tryon/:jobId/save-to-collection
 * Body: { name, category?, color?, season?, occasion?, brand?, price? }
 *
 * Response: { success: true, data: { clothingItem } }
 */

const path = require('path');
const fs = require('fs');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');
const ProcessingJob = require('../models/ProcessingJob');
const ClothingItem = require('../models/ClothingItem');
const crypto = require('crypto');

/**
 * POST /api/ai-stylist/virtual-tryon/:jobId/save-to-collection
 * 
 * Lưu ảnh kết quả try-on vào tủ đồ của user như một ClothingItem.
 * Chỉ cho phép chủ sở hữu của job mới được lưu.
 */
const saveTryOnToCollection = asyncHandler(async (req, res, next) => {
  const { jobId } = req.params;
  const currentUserId = req.user._id;

  // 1. Kiểm tra job có tồn tại và đã hoàn thành không
  const job = await ProcessingJob.findOne({ jobId });
  if (!job) {
    return next(new AppError('Không tìm thấy job try-on này', 404));
  }
  if (job.status !== 'completed') {
    return next(new AppError('Job try-on chưa hoàn thành, không thể lưu', 400));
  }
  
  // 2. Kiểm tra quyền: chỉ user sở hữu job mới được lưu
  if (job.userId && job.userId.toString() !== currentUserId.toString()) {
    return next(new AppError('Bạn không có quyền lưu kết quả try-on này', 403));
  }

  // 3. Kiểm tra ảnh kết quả có tồn tại
  const result = job.result || {};
  const generatedImageUrl = result.generatedImageUrl;
  if (!generatedImageUrl) {
    return next(new AppError('Không tìm thấy ảnh kết quả try-on', 404));
  }

  // 4. Copy ảnh từ uploads/tryon/ sang uploads/items/
  const sourcePath = path.join(__dirname, '..', generatedImageUrl);
  const itemsDir = path.join(__dirname, '..', 'uploads', 'items');
  if (!fs.existsSync(itemsDir)) {
    fs.mkdirSync(itemsDir, { recursive: true });
  }

  const ext = path.extname(generatedImageUrl) || '.png';
  const newFileName = `tryon_saved_${Date.now()}_${crypto.randomBytes(4).toString('hex')}${ext}`;
  const destPath = path.join(itemsDir, newFileName);

  if (fs.existsSync(sourcePath)) {
    fs.copyFileSync(sourcePath, destPath);
    console.log(`📋 Đã copy ảnh try-on vào items/: ${newFileName}`);
  } else {
    return next(new AppError('File ảnh kết quả không tồn tại trên server', 404));
  }

  // 5. Lấy thông tin từ body (nếu có)
  const {
    name = 'Try-On Result',
    category = 'Tops',
    color = '',
    season = '',
    occasion = '',
    brand = '',
    price = 0
  } = req.body;

  // 6. Tạo ClothingItem mới với ảnh đã copy
  const newItem = await ClothingItem.create({
    _id: `tryon_${jobId}_${Date.now()}`,
    userId: currentUserId,
    imageOriginal: `/uploads/items/${newFileName}`,
    imageNoBg: '',
    category: category || 'Tops',
    color: color || '',
    name: name || 'Try-On Result',
    season: season || '',
    occasion: occasion || '',
    brand: brand || '',
    purchaseDate: Date.now(),
    price: price || 0,
    canvasPosX: 0.5,
    canvasPosY: 0.5,
    createdAt: Date.now()
  });

  console.log(`✅ Đã lưu try-on result vào tủ đồ: ${newItem._id}`);

  res.status(201).json({
    success: true,
    message: 'Đã lưu kết quả try-on vào bộ sưu tập thành công',
    data: newItem
  });
});

module.exports = {
  saveTryOnToCollection
};