const multer = require('multer');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');
const { removeBackground } = require('../services/removeBgService');

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

const removeBgFromImage = asyncHandler(async (req, res) => {
  if (!req.file) {
    throw new AppError('Vui lòng chọn file ảnh để tách nền', 400);
  }

  const outputBuffer = await removeBackground({
    buffer: req.file.buffer,
    filename: req.file.originalname
  });

  res.setHeader('Content-Type', 'image/png');
  res.setHeader('Cache-Control', 'no-store');
  res.status(200).send(outputBuffer);
});

module.exports = {
  upload,
  removeBgFromImage
};
