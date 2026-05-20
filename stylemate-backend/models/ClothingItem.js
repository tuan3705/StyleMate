/**
 * 👕 ClothingItem Model
 * 
 * Schema cho Quần áo trong tủ đồ.
 * Dùng String _id để Client Android tự sinh UUID và gửi lên.
 */
const mongoose = require('mongoose');

const clothingItemSchema = new mongoose.Schema({
  _id: {
    type: String,
    required: [true, 'ID là bắt buộc (UUID do Client sinh)']
  },
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'userId là bắt buộc']
  },
  imageOriginal: {
    type: String,
    default: ''
  },
  imageNoBg: {
    type: String,
    default: ''
  },
  category: {
    type: String,
    required: [true, 'Danh mục là bắt buộc'],
    enum: ['Tops', 'Bottoms', 'Dresses', 'Footwear', 'Bags', 'Accessories', 'Jewelry'],
    default: 'Tops'
  },
  color: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: ''
  },
  season: {
    type: String,
    enum: ['', 'Spring', 'Summer', 'Autumn', 'Winter'],
    default: ''
  },
  occasion: {
    type: String,
    enum: ['', 'Casual', 'Work', 'Sports', 'Formal'],
    default: ''
  },
  brand: {
    type: String,
    default: ''
  },
  purchaseDate: {
    type: Number,
    default: 0
  },
  price: {
    type: Number,
    default: 0.0
  },
  canvasPosX: {
    type: Number,
    default: 0.5
  },
  canvasPosY: {
    type: Number,
    default: 0.5
  },
  createdAt: {
    type: Number,
    default: () => Date.now()
  }
}, {
  _id: false,        // Không dùng ObjectId tự động
  timestamps: false  // Tự quản lý createdAt
});

// Index để lọc theo userId nhanh hơn
clothingItemSchema.index({ userId: 1, category: 1, createdAt: -1 });
clothingItemSchema.index({ userId: 1, createdAt: -1 });

// Index để lọc theo category nhanh hơn
clothingItemSchema.index({ category: 1, createdAt: -1 });
clothingItemSchema.index({ createdAt: -1 });

module.exports = mongoose.model('ClothingItem', clothingItemSchema);
