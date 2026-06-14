/**
 * 🖼️ Save Try-On Result to Collection Controller
 *
 * Allows user to save the result image from virtual try-on
 * into their ClothingItem collection (closet).
 *
 * Requirement: User must be authenticated (requireAuth).
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
 * Save try-on result image to user's closet as a ClothingItem.
 * Only the job owner is allowed to save.
 */
const saveTryOnToCollection = asyncHandler(async (req, res, next) => {
  const { jobId } = req.params;
  const currentUserId = req.user._id;

  // 1. Check if job exists and is completed
  const job = await ProcessingJob.findOne({ jobId });
  if (!job) {
    return next(new AppError('Try-on job not found', 404));
  }
  if (job.status !== 'completed') {
    return next(new AppError('Try-on job not completed yet, cannot save', 400));
  }
  
  // 2. Check permissions: only job owner can save
  if (job.userId && job.userId.toString() !== currentUserId.toString()) {
    return next(new AppError('You do not have permission to save this try-on result', 403));
  }

  // 3. Check if result image exists
  const result = job.result || {};
  const generatedImageUrl = result.generatedImageUrl;
  if (!generatedImageUrl) {
    return next(new AppError('Try-on result image not found', 404));
  }

  // 4. Copy image from uploads/tryon/ to uploads/items/
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
    console.log(`📋 Copied try-on image to items/: ${newFileName}`);
  } else {
    return next(new AppError('Result image file does not exist on server', 404));
  }

  // 5. Get info from body (if provided)
  const {
    name = 'Try-On Result',
    category = 'Tops',
    color = '',
    season = '',
    occasion = '',
    brand = '',
    price = 0
  } = req.body;

  // 6. Create new ClothingItem with copied image
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

  console.log(`✅ Saved try-on result to closet: ${newItem._id}`);

  res.status(201).json({
    success: true,
    message: 'Try-on result saved to collection successfully',
    data: newItem
  });
});

module.exports = {
  saveTryOnToCollection
};