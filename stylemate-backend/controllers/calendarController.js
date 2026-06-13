/**
 * 📅 Calendar Controller
 * 
 * Xử lý tất cả logic CRUD cho CalendarEvent.
 * 
 * ⚠️ date phải là epoch midnight (00:00 UTC) do Client gửi lên.
 * UNIQUE constraint theo userId + date → mỗi ngày chỉ 1 outfit cho mỗi user.
 * Dùng upsert để REPLACE nếu đã tồn tại sự kiện cho ngày đó.
 */
const CalendarEvent = require('../models/CalendarEvent');
const Outfit = require('../models/Outfit');
const ClothingItem = require('../models/ClothingItem');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');

/**
 * 📋 GET /api/calendar
 * 
 * Lấy danh sách sự kiện lịch.
 * Hỗ trợ query params:
 *   - date: Lấy sự kiện của 1 ngày cụ thể (epoch midnight)
 *   - from, to: Lấy sự kiện trong khoảng thời gian
 *   - populate: nếu "true" → trả về thêm thông tin outfit + clothing items
 *
 * Nếu không có params, trả về tất cả sự kiện.
 * 
 * Response: { success: true, count: Number, data: [...] }
 */
const getCalendarEvents = asyncHandler(async (req, res) => {
  const { date, from, to, populate } = req.query;
  const currentUserId = req.user._id;

  let filter = { userId: currentUserId };

  if (date) {
    const dateNum = Number(date);
    if (isNaN(dateNum)) {
      return res.status(400).json({
        success: false,
        message: 'Tham số date phải là số (epoch millis)'
      });
    }
    filter.date = dateNum;
  } else if (from && to) {
    const fromNum = Number(from);
    const toNum = Number(to);
    if (isNaN(fromNum) || isNaN(toNum)) {
      return res.status(400).json({
        success: false,
        message: 'Tham số from và to phải là số (epoch millis)'
      });
    }
    filter.date = { $gte: fromNum, $lte: toNum };
  }

  let events = await CalendarEvent.find(filter).sort({ date: 1 });

  if (populate === 'true' && events.length > 0) {
    const outfitIds = [...new Set(events.map(e => e.outfitId))];
    const outfits = await Outfit.find({ _id: { $in: outfitIds } }).lean();
    const allItemIds = [
      ...new Set(outfits.flatMap(o => o.clothingItems.map(ci => ci.clothingItemId)))
    ];
    const clothingItems = await ClothingItem.find({ _id: { $in: allItemIds } }).lean();
    const clothingMap = {};
    clothingItems.forEach(item => { clothingMap[item._id] = item; });
    const outfitMap = {};
    outfits.forEach(o => {
      o.clothingItems = o.clothingItems.map(ci => ({
        ...ci,
        clothingItem: clothingMap[ci.clothingItemId] || null
      }));
      outfitMap[o._id] = o;
    });
    events = events.map(event => ({
      ...event.toObject(),
      outfit: outfitMap[event.outfitId] || null
    }));
  }
  res.status(200).json({
    success: true,
    count: events.length,
    data: events
  });
});

/**
 * 🔍 GET /api/calendar/:id
 */
const getCalendarEventById = asyncHandler(async (req, res, next) => {
  const { id } = req.params;
  const currentUserId = req.user._id;
  const event = await CalendarEvent.findOne({ _id: id, userId: currentUserId });
  if (!event) {
    return next(new AppError(`Không tìm thấy CalendarEvent với ID: ${id}`, 404));
  }
  res.status(200).json({ success: true, data: event });
});

/**
 * ➕ POST /api/calendar
 *
 * Gán (hoặc thay thế) một Outfit vào một ngày.
 *
 * ⚠️ Dùng findOneAndUpdate với upsert=true để tránh lỗi E11000 duplicate key.
 * Index cũ { date: 1 } (unique) có thể còn tồn tại trong DB, upsert sẽ bypass.
 *
 * Body:
 *   - _id: String (UUID do Client sinh)
 *   - date: Number (epoch midnight)
 *   - outfitId: String (UUID của Outfit)
 */
const createOrReplaceCalendarEvent = asyncHandler(async (req, res) => {
  const { _id, date, outfitId } = req.body;
  const currentUserId = req.user._id;

  // Validation
  if (!_id) {
    return res.status(400).json({ success: false, message: 'Trường _id (UUID) là bắt buộc' });
  }
  if (date === undefined || date === null) {
    return res.status(400).json({ success: false, message: 'Trường date (epoch midnight) là bắt buộc' });
  }
  if (!outfitId) {
    return res.status(400).json({ success: false, message: 'Trường outfitId là bắt buộc' });
  }

  const dateNum = Number(date);
  if (isNaN(dateNum)) {
    return res.status(400).json({ success: false, message: 'date phải là số (epoch millis)' });
  }

  const ownedOutfit = await Outfit.findOne({ _id: outfitId, userId: currentUserId });
  if (!ownedOutfit) {
    return res.status(404).json({
      success: false,
      message: 'Outfit không tồn tại hoặc không thuộc về người dùng'
    });
  }

  // ⚡ UPSERT: tìm theo userId+date → nếu chưa có thì tạo, nếu có thì cập nhật outfitId
  // Dùng upsert thay vì find + create để tránh E11000 duplicate key (index cũ date_1)
  // QUAN TRỌNG: Không được $set _id vì _id là immutable khi đã tồn tại
  const updatedEvent = await CalendarEvent.findOneAndUpdate(
    { userId: currentUserId, date: dateNum },
    { $set: { outfitId }, $setOnInsert: { _id } },
    { upsert: true, new: true, setDefaultsOnInsert: true }
  );

  res.status(200).json({
    success: true,
    message: 'Đã gán outfit thành công',
    data: updatedEvent
  });
});

/**
 * ✏️ PUT /api/calendar/:id
 */
const updateCalendarEvent = asyncHandler(async (req, res, next) => {
  const { id } = req.params;
  const { outfitId, date } = req.body;
  const currentUserId = req.user._id;

  const updateData = {};
  if (outfitId !== undefined) updateData.outfitId = outfitId;
  if (date !== undefined) {
    const dateNum = Number(date);
    if (isNaN(dateNum)) {
      return res.status(400).json({ success: false, message: 'date phải là số (epoch millis)' });
    }
    updateData.date = dateNum;
  }

  const updatedEvent = await CalendarEvent.findOneAndUpdate(
    { _id: id, userId: currentUserId },
    updateData,
    { new: true, runValidators: true }
  );

  if (!updatedEvent) {
    return next(new AppError(`Không tìm thấy CalendarEvent với ID: ${id}`, 404));
  }
  res.status(200).json({ success: true, data: updatedEvent });
});

/**
 * ❌ DELETE /api/calendar/:id
 */
const deleteCalendarEvent = asyncHandler(async (req, res, next) => {
  const { id } = req.params;
  const currentUserId = req.user._id;
  const deletedEvent = await CalendarEvent.findOneAndDelete({ _id: id, userId: currentUserId });
  if (!deletedEvent) {
    return next(new AppError(`Không tìm thấy CalendarEvent với ID: ${id}`, 404));
  }
  res.status(200).json({ success: true, message: `Đã xoá CalendarEvent: ${id}`, data: {} });
});

/**
 * ❌ DELETE /api/calendar/by-date/:date
 */
const deleteCalendarEventByDate = asyncHandler(async (req, res, next) => {
  const { date } = req.params;
  const currentUserId = req.user._id;
  const dateNum = Number(date);
  if (isNaN(dateNum)) {
    return res.status(400).json({ success: false, message: 'date phải là số (epoch millis)' });
  }
  const deletedEvent = await CalendarEvent.findOneAndDelete({ userId: currentUserId, date: dateNum });
  if (!deletedEvent) {
    return next(new AppError(`Không tìm thấy sự kiện cho ngày: ${dateNum}`, 404));
  }
  res.status(200).json({ success: true, message: `Đã xoá sự kiện cho ngày ${dateNum}`, data: deletedEvent });
});

/**
 * 🗑️ DELETE /api/calendar/bulk
 */
const bulkDeleteCalendarEvents = asyncHandler(async (req, res) => {
  const currentUserId = req.user._id;
  const result = await CalendarEvent.deleteMany({ userId: currentUserId });
  res.status(200).json({
    success: true,
    message: `Đã xoá ${result.deletedCount} sự kiện lịch`,
    deletedCount: result.deletedCount
  });
});

module.exports = {
  getCalendarEvents,
  getCalendarEventById,
  createOrReplaceCalendarEvent,
  updateCalendarEvent,
  deleteCalendarEvent,
  deleteCalendarEventByDate,
  bulkDeleteCalendarEvents
};