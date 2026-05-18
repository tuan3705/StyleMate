/**
 * 📅 CalendarEvent Model
 * 
 * Lưu sự kiện gán Outfit vào một ngày cụ thể.
 * - date là epoch midnight (00:00 UTC) — UNIQUE: mỗi ngày chỉ 1 sự kiện.
 * - outfitId tham chiếu tới Outfit.
 * 
 * Dùng String _id để Client Android tự sinh UUID và gửi lên.
 */
const mongoose = require('mongoose');

const calendarEventSchema = new mongoose.Schema({
  _id: {
    type: String,
    required: [true, 'ID là bắt buộc (UUID do Client sinh)']
  },
  date: {
    type: Number,
    required: [true, 'Ngày (epoch midnight) là bắt buộc'],
    unique: true // Mỗi ngày chỉ tối đa 1 sự kiện
  },
  outfitId: {
    type: String,
    required: [true, 'outfitId là bắt buộc']
  },
  createdAt: {
    type: Number,
    default: () => Date.now()
  }
}, {
  _id: false,
  timestamps: false
});

// Indexes
calendarEventSchema.index({ date: 1 }, { unique: true });
calendarEventSchema.index({ outfitId: 1 });

module.exports = mongoose.model('CalendarEvent', calendarEventSchema);
