/**
 * 📅 CalendarEvent Model
 * 
 * Lưu sự kiện gán Outfit vào một ngày cụ thể.
 * - date là epoch midnight (00:00 UTC)
 * - outfitId tham chiếu tới Outfit.
 * - Mỗi user chỉ tối đa 1 sự kiện cho mỗi ngày (unique theo userId + date).
 *
 * Dùng String _id để Client Android tự sinh UUID và gửi lên.
 */
const mongoose = require('mongoose');

const calendarEventSchema = new mongoose.Schema({
  _id: {
    type: String,
    required: [true, 'ID là bắt buộc (UUID do Client sinh)']
  },
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'userId là bắt buộc']
  },
  date: {
    type: Number,
    required: [true, 'Ngày (epoch midnight) là bắt buộc']
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
calendarEventSchema.index({ userId: 1, date: 1 }, { unique: true });
calendarEventSchema.index({ outfitId: 1, userId: 1 });

module.exports = mongoose.model('CalendarEvent', calendarEventSchema);
