/**
 * 👔 Outfit Model
 * 
 * Schema cho Bộ phối đồ (Outfit).
 * Lưu mảng clothingItems chứa clothingItemId (tham chiếu tới ClothingItem)
 * kèm vị trí posX, posY (cho canvas ảnh).
 * 
 * Dùng String _id để Client Android tự sinh UUID và gửi lên.
 */
const mongoose = require('mongoose');

/**
 * Sub-document cho một item trong Outfit.
 * Chứa clothingItemId để tham chiếu tới ClothingItem,
 * và posX/posY cho vị trí trên canvas ảnh.
 */
const outfitClothingItemSchema = new mongoose.Schema({
  clothingItemId: {
    type: String,
    required: [true, 'clothingItemId là bắt buộc']
  },
  posX: {
    type: Number,
    default: 0.5
  },
  posY: {
    type: Number,
    default: 0.5
  },
  scale: {
    type: Number,
    default: 1.0
  }
}, {
  _id: false // Không cần _id riêng cho sub-document
});

const outfitSchema = new mongoose.Schema({
  _id: {
    type: String,
    required: [true, 'ID là bắt buộc (UUID do Client sinh)']
  },
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'userId là bắt buộc']
  },
  name: {
    type: String,
    required: [true, 'Tên bộ đồ là bắt buộc'],
    trim: true,
    maxlength: [200, 'Tên bộ đồ không được quá 200 ký tự']
  },
  clothingItems: {
    type: [outfitClothingItemSchema],
    default: []
  },
  createdAt: {
    type: Number,
    default: () => Date.now()
  }
}, {
  _id: false,
  timestamps: false
});

outfitSchema.index({ userId: 1, createdAt: -1 });
outfitSchema.index({ createdAt: -1 });

module.exports = mongoose.model('Outfit', outfitSchema);
