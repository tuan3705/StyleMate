const multer = require('multer');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');
const { tagImage } = require('../services/aiAutoTaggingService');
const { mapAutoTaggingToSuggestion } = require('../services/aiAutoTaggingMapper');

const allowedTypes = /jpeg|jpg|png|gif|webp|bmp/;

const fileFilter = (req, file, cb) => {
  const extname = allowedTypes.test(require('path').extname(file.originalname).toLowerCase());
  const mimetype = allowedTypes.test(file.mimetype);
  const isImageMime = typeof file.mimetype === 'string' && file.mimetype.startsWith('image/');

  if (extname || mimetype || isImageMime) {
    cb(null, true);
  } else {
    cb(new AppError('Chỉ chấp nhận file ảnh (jpg, png, gif, webp, bmp)', 400), false);
  }
};

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter
});

const autoTaggingFromImage = asyncHandler(async (req, res) => {
  if (!req.file) {
    throw new AppError('Vui lòng chọn file ảnh để auto tagging', 400);
  }

  const additionalContext =
    'Return tags that help classify season (Spring/Summer/Autumn/Winter) and occasion (Casual/Work/Sports/Formal) for the clothing item.';

  const response = await tagImage({
    buffer: req.file.buffer,
    filename: req.file.originalname,
    additionalContext
  });

  console.log('🔎 Auto tagging response:', JSON.stringify(response));

  const mapped = mapAutoTaggingToSuggestion(response);

  res.status(200).json({
    success: true,
    data: mapped
  });
});

module.exports = {
  upload,
  autoTaggingFromImage
};

