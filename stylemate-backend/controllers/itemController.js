const fs = require('fs');
const path = require('path');
const ClothingItem = require('../models/ClothingItem');
const imageSegmentationService = require('../services/imageSegmentationService');
const itemExtractionService = require('../services/itemExtractionService');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');
const ProcessingJob = require('../models/ProcessingJob');
const PUBLIC_BASE_URL = process.env.PUBLIC_BASE_URL || `http://localhost:${process.env.PORT || 3000}`;

const UPLOAD_DIR = path.join(__dirname, '..', 'uploads', 'items');
if (!fs.existsSync(UPLOAD_DIR)) fs.mkdirSync(UPLOAD_DIR, { recursive: true });

function genId() {
  return `item_${Date.now().toString(36)}_${Math.random().toString(36).slice(2,8)}`;
}

async function processItemJob(jobId) {
  const job = await ProcessingJob.findOne({ jobId }).exec();
  if (!job) return;

  try {
    job.status = 'processing'; job.progress = 10; await job.save();

    const originalPath = job.params.originalPath;
    const publicOriginal = job.params.publicOriginal;

    // Attempt background removal
    const nobg = await imageSegmentationService.removeBackground(originalPath);
    job.progress = 30; await job.save();

    const imageNoBgLocal = nobg?.localPath || null;
    const imageNoBgUrl = nobg?.publicUrl || null;

    const imageForExtraction = imageNoBgUrl ? `${PUBLIC_BASE_URL}${imageNoBgUrl}` : `${PUBLIC_BASE_URL}${publicOriginal}`;

    // Extract metadata via LLM
    const metadata = await itemExtractionService.extractMetadata({ userId: job.userId, imageUrl: imageForExtraction });
    job.progress = 70; await job.save();

    // Create ClothingItem doc
    const itemId = genId();
    const doc = new ClothingItem({
      _id: itemId,
      imageOriginal: publicOriginal,
      imageNoBg: imageNoBgUrl || '',
      category: metadata?.category || 'Tops',
      color: (metadata?.colors && metadata.colors[0]) || '',
      name: metadata?.subcategory || '',
      season: (metadata?.season && metadata.season[0]) || '',
      occasion: (metadata?.occasion && metadata.occasion[0]) || '',
      brand: metadata?.brand || ''
    });

    await doc.save();

    job.status = 'completed';
    job.progress = 100;
    job.result = { itemId: doc._id, metadata };
    await job.save();
  } catch (err) {
    console.error('processItemJob error:', err.message);
    job.status = 'failed'; job.result = { message: err.message }; job.progress = 0; await job.save();
  }
}

/**
 * POST /api/items/upload
 * multipart: image (file), userId (field)
 */
const uploadItem = asyncHandler(async (req, res, next) => {
  const userId = req.body.userId || null;

  if (!req.file) return next(new AppError('Missing image file', 400));

  const originalPath = req.file.path;
  const publicOriginal = `/uploads/items/${req.file.filename}`;

  // Create processing job in DB
  const jobId = `itemjob_${Date.now().toString(36)}_${Math.random().toString(36).slice(2,8)}`;
  const ttlMs = Number(process.env.TEMP_ASSET_TTL_MS || 24 * 3600 * 1000);
  const expiresAt = new Date(Date.now() + ttlMs);

  const job = new ProcessingJob({
    jobId,
    type: 'item_upload',
    userId,
    status: 'queued',
    progress: 0,
    params: { originalPath, publicOriginal, filename: req.file.filename },
    expiresAt
  });

  await job.save();

  // Start background processing
  process.nextTick(() => processItemJob(jobId));

  res.status(202).json({ success: true, jobId });
});

const getUploadStatus = asyncHandler(async (req, res, next) => {
  const { jobId } = req.params;
  const job = await ProcessingJob.findOne({ jobId }).lean().exec();
  if (!job) return res.status(404).json({ success: false, message: 'Job not found' });
  res.status(200).json({ success: true, job });
});

const deleteTempJob = asyncHandler(async (req, res, next) => {
  const { jobId } = req.params;
  const job = await ProcessingJob.findOne({ jobId }).exec();
  if (!job) return res.status(404).json({ success: false, message: 'Job not found' });

  // Attempt to remove temp files referenced in params
  try {
    const originalPath = job.params?.originalPath;
    if (originalPath && fs.existsSync(originalPath)) fs.unlinkSync(originalPath);
    const nobg = job.params?.nobgLocalPath;
    if (nobg && fs.existsSync(nobg)) fs.unlinkSync(nobg);
  } catch (err) {
    console.warn('deleteTempJob cleanup error:', err.message);
  }

  job.status = 'cancelled';
  job.expiresAt = new Date();
  await job.save();

  res.status(200).json({ success: true, message: 'Temp assets removed, job cancelled' });
});

module.exports = { uploadItem, getUploadStatus, deleteTempJob };
