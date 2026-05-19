/**
 * 👤 User Model
 *
 * Lưu thông tin đăng nhập cho Auth.
 */
const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  email: {
    type: String,
    required: [true, 'Email là bắt buộc'],
    unique: true,
    trim: true,
    lowercase: true,
    index: true
  },
  passwordHash: {
    type: String,
    required: [true, 'Password hash là bắt buộc'],
    select: false
  },
  refreshTokenHash: {
    type: String,
    default: null,
    select: false
  },
  tokenVersion: {
    type: Number,
    default: 0
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

userSchema.pre('save', function (next) {
  this.updatedAt = Date.now();
  next();
});

module.exports = mongoose.model('User', userSchema);

