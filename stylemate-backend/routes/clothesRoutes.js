/**
 * 👕 Clothes Routes
 * 
 * Định tuyến cho các API CRUD của ClothingItem.
 * 
 * Base path: /api/clothes
 */
const express = require('express');
const router = express.Router();
const {
  getAllClothes,
  getClothingItemById,
  createClothingItem,
  updateClothingItem,
  deleteClothingItem
} = require('../controllers/clothesController');
const {
  upload,
  uploadImage
} = require('../controllers/uploadController');
const { requireAuth } = require('../middleware/authMiddleware');

// Bảo vệ toàn bộ route quần áo
router.use(requireAuth);

// GET /api/clothes?category=Tops — Lấy danh sách (có lọc)
router.get('/', getAllClothes);

// GET /api/clothes/:id — Lấy chi tiết theo ID
router.get('/:id', getClothingItemById);

// POST /api/clothes/upload — Upload ảnh (ĐẶT TRƯỚC /:id để không bị match nhầm)
router.post('/upload', upload.single('image'), uploadImage);

// POST /api/clothes — Tạo mới
router.post('/', createClothingItem);

// PUT /api/clothes/:id — Cập nhật
router.put('/:id', updateClothingItem);

// DELETE /api/clothes/:id — Xoá
router.delete('/:id', deleteClothingItem);

module.exports = router;
