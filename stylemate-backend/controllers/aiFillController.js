const multer = require('multer');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');
const { tagImage } = require('../services/lykdatTaggingService');
const { mapLykdatToItemFields } = require('../services/lykdatTaggingMapper');

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

const aiFillFromImage = asyncHandler(async (req, res) => {
  if (!req.file) {
    throw new AppError('Vui lòng chọn file ảnh để nhận gợi ý', 400);
  }

  const response = await tagImage({
    buffer: req.file.buffer,
    filename: req.file.originalname,
    mimetype: req.file.mimetype
  });

  console.log('🔎 Lykdat response:', JSON.stringify(response));

  const mapped = mapLykdatToItemFields(response);

  res.status(200).json({
    success: true,
    data: mapped
  });
});

module.exports = {
  upload,
  aiFillFromImage
};

