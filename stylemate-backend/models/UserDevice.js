/**
 * 📱 UserDevice Model
 * 
 * Lưu thông tin thiết bị người dùng và FCM Token.
 * Dùng cho tính năng Push Notification sau này.
 * 
 * - userId: do Client gửi lên (có thể là UUID do Client sinh ra).
 * - fcmToken: Token FCM của thiết bị (UNIQUE — một token chỉ thuộc 1 user).
 */
const mongoose = require('mongoose');

const userDeviceSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: [true, 'userId là bắt buộc'],
    index: true
  },
  fcmToken: {
    type: String,
    required: [true, 'FCM token là bắt buộc'],
    unique: true, // Mỗi token là duy nhất
    trim: true
  },
  createdAt: {
    type: Number,
    default: () => Date.now()
  },
  updatedAt: {
    type: Number,
    default: () => Date.now()
  }
}, {
  timestamps: false
});

// Cập nhật updatedAt mỗi khi document thay đổi
userDeviceSchema.pre('findOneAndUpdate', function (next) {
  this.set({ updatedAt: Date.now() });
  next();
});

userDeviceSchema.index({ userId: 1 });

module.exports = mongoose.model('UserDevice', userDeviceSchema);
