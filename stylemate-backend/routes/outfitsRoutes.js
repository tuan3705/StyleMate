/**
 * 👔 Outfits Routes
 * 
 * Định tuyến cho các API CRUD của Outfit.
 * 
 * Base path: /api/outfits
 */
const express = require('express');
const router = express.Router();
const {
  getAllOutfits,
  getOutfitById,
  createOutfit,
  updateOutfit,
  deleteOutfit,
  getOutfitsContainingItem
} = require('../controllers/outfitsController');

// GET /api/outfits?populate=true — Lấy danh sách (có thể populate ClothingItem)
router.get('/', getAllOutfits);

// GET /api/outfits/by-item/:clothingItemId — Lấy outfits chứa 1 item
router.get('/by-item/:clothingItemId', getOutfitsContainingItem);

// GET /api/outfits/:id — Lấy chi tiết outfit
router.get('/:id', getOutfitById);

// POST /api/outfits — Tạo outfit mới
router.post('/', createOutfit);

// PUT /api/outfits/:id — Cập nhật outfit
router.put('/:id', updateOutfit);

// DELETE /api/outfits/:id — Xoá outfit (CASCADE: xoá luôn CalendarEvent)
router.delete('/:id', deleteOutfit);

module.exports = router;
