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
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');

/**
 * 📋 GET /api/calendar
 * 
 * Lấy danh sách sự kiện lịch.
 * Hỗ trợ query params:
 *   - date: Lấy sự kiện của 1 ngày cụ thể (epoch midnight)
 *   - from, to: Lấy sự kiện trong khoảng thời gian
 * 
 * Nếu không có params, trả về tất cả sự kiện.
 * 
 * Response: { success: true, count: Number, data: [...] }
 */
const getCalendarEvents = asyncHandler(async (req, res) => {
  const { date, from, to } = req.query;
  const currentUserId = req.user._id;

  let filter = { userId: currentUserId };

  if (date) {
    // Lấy sự kiện của 1 ngày cụ thể
    const dateNum = Number(date);
    if (isNaN(dateNum)) {
      return res.status(400).json({
        success: false,
        message: 'Tham số date phải là số (epoch millis)'
      });
    }
    filter.date = dateNum;
  } else if (from && to) {
    // Lấy sự kiện trong khoảng
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

  const events = await CalendarEvent.find(filter).sort({ date: 1 });

  res.status(200).json({
    success: true,
    count: events.length,
    data: events
  });
});

/**
 * 🔍 GET /api/calendar/:id
 * 
 * Lấy chi tiết một sự kiện lịch theo ID.
 * 
 * Response: { success: true, data: { ... } }
 * Error 404: { success: false, message: "..." }
 */
const getCalendarEventById = asyncHandler(async (req, res, next) => {
  const { id } = req.params;
  const currentUserId = req.user._id;

  const event = await CalendarEvent.findOne({ _id: id, userId: currentUserId });

  if (!event) {
    return next(new AppError(`Không tìm thấy CalendarEvent với ID: ${id}`, 404));
  }

  res.status(200).json({
    success: true,
    data: event
  });
});

/**
 * ➕ POST /api/calendar
 * 
 * Gán (hoặc thay thế) một Outfit vào một ngày.
 * 
 * ⚠️ Nếu đã có sự kiện cho ngày đó, nó sẽ bị ghi đè (upsert/replace).
 * Cơ chế: Dùng findOneAndUpdate với upsert = true để:
 *   - Nếu chưa có: Tạo mới
 *   - Nếu đã có: Cập nhật outfitId (thay outfit cũ bằng outfit mới)
 * 
 * Body:
 *   - _id: String (UUID do Client sinh) — bắt buộc
 *   - date: Number (epoch midnight) — bắt buộc
 *   - outfitId: String (UUID của Outfit) — bắt buộc
 * 
 * Response 200 (nếu update): { success: true, message: "Đã cập nhật", data: {...} }
 * Response 201 (nếu create): { success: true, message: "Đã tạo mới", data: {...} }
 */
const createOrReplaceCalendarEvent = asyncHandler(async (req, res) => {
  const { _id, date, outfitId } = req.body;
  const currentUserId = req.user._id;

  // Validation
  if (!_id) {
    return res.status(400).json({
      success: false,
      message: 'Trường _id (UUID) là bắt buộc'
    });
  }
  if (date === undefined || date === null) {
    return res.status(400).json({
      success: false,
      message: 'Trường date (epoch midnight) là bắt buộc'
    });
  }
  if (!outfitId) {
    return res.status(400).json({
      success: false,
      message: 'Trường outfitId là bắt buộc'
    });
  }

  const dateNum = Number(date);
  if (isNaN(dateNum)) {
    return res.status(400).json({
      success: false,
      message: 'date phải là số (epoch millis)'
    });
  }

  const ownedOutfit = await Outfit.findOne({ _id: outfitId, userId: currentUserId });
  if (!ownedOutfit) {
    return res.status(404).json({
      success: false,
      message: 'Outfit không tồn tại hoặc không thuộc về người dùng'
    });
  }

  // Kiểm tra ngày đó đã có sự kiện chưa
  const existingEvent = await CalendarEvent.findOne({ userId: currentUserId, date: dateNum });

  if (existingEvent) {
    // Đã có sự kiện → UPDATE/Cập nhật outfitId
    const updatedEvent = await CalendarEvent.findByIdAndUpdate(
      existingEvent._id,
      { outfitId, _id: existingEvent._id }, // Giữ nguyên _id cũ, chỉ thay outfitId
      { new: true }
    );

    return res.status(200).json({
      success: true,
      message: 'Đã cập nhật outfit cho ngày này (ghi đè)',
      data: updatedEvent
    });
  }

  // Chưa có → Tạo mới
  const newEvent = await CalendarEvent.create({
    _id,
    userId: currentUserId,
    date: dateNum,
    outfitId,
    createdAt: Date.now()
  });

  res.status(201).json({
    success: true,
    message: 'Đã gán outfit thành công',
    data: newEvent
  });
});

/**
 * ✏️ PUT /api/calendar/:id
 * 
 * Cập nhật một sự kiện lịch (thay outfitId).
 * 
 * Response: { success: true, data: { ... } }
 * Error 404: { success: false, message: "..." }
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
      return res.status(400).json({
        success: false,
        message: 'date phải là số (epoch millis)'
      });
    }
    updateData.date = dateNum;
  }

  const updatedEvent = await CalendarEvent.findOneAndUpdate(
    { _id: id, userId: currentUserId },
    updateData,
    {
      new: true,
      runValidators: true
    }
  );

  if (!updatedEvent) {
    return next(new AppError(`Không tìm thấy CalendarEvent với ID: ${id}`, 404));
  }

  res.status(200).json({
    success: true,
    data: updatedEvent
  });
});

/**
 * ❌ DELETE /api/calendar/:id
 * 
 * Xoá một sự kiện lịch theo ID.
 * 
 * Response: { success: true, message: "...", data: {} }
 * Error 404: { success: false, message: "..." }
 */
const deleteCalendarEvent = asyncHandler(async (req, res, next) => {
  const { id } = req.params;
  const currentUserId = req.user._id;

  const deletedEvent = await CalendarEvent.findOneAndDelete({ _id: id, userId: currentUserId });

  if (!deletedEvent) {
    return next(new AppError(`Không tìm thấy CalendarEvent với ID: ${id}`, 404));
  }

  res.status(200).json({
    success: true,
    message: `Đã xoá CalendarEvent: ${id}`,
    data: {}
  });
});

/**
 * ❌ DELETE /api/calendar/by-date/:date
 * 
 * Xoá sự kiện theo ngày (epoch midnight).
 * 
 * Response: { success: true, message: "...", data: {} }
 * Error 404: { success: false, message: "..." }
 */
const deleteCalendarEventByDate = asyncHandler(async (req, res, next) => {
  const { date } = req.params;
  const currentUserId = req.user._id;
  const dateNum = Number(date);

  if (isNaN(dateNum)) {
    return res.status(400).json({
      success: false,
      message: 'date phải là số (epoch millis)'
    });
  }

  const deletedEvent = await CalendarEvent.findOneAndDelete({ userId: currentUserId, date: dateNum });

  if (!deletedEvent) {
    return next(new AppError(`Không tìm thấy sự kiện cho ngày: ${dateNum}`, 404));
  }

  res.status(200).json({
    success: true,
    message: `Đã xoá sự kiện cho ngày ${dateNum}`,
    data: deletedEvent
  });
});

module.exports = {
  getCalendarEvents,
  getCalendarEventById,
  createOrReplaceCalendarEvent,
  updateCalendarEvent,
  deleteCalendarEvent,
  deleteCalendarEventByDate
};
