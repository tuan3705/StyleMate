/**
 * 📅 Calendar Routes
 * 
 * Định tuyến cho các API của CalendarEvent.
 * 
 * Base path: /api/calendar
 */
const express = require('express');
const router = express.Router();
const {
  getCalendarEvents,
  getCalendarEventById,
  createOrReplaceCalendarEvent,
  updateCalendarEvent,
  deleteCalendarEvent,
  deleteCalendarEventByDate
} = require('../controllers/calendarController');
const { requireAuth } = require('../middleware/authMiddleware');

// Bảo vệ toàn bộ route lịch
router.use(requireAuth);

// GET /api/calendar?date=...&from=...&to=... — Lấy sự kiện (theo ngày hoặc khoảng)
router.get('/', getCalendarEvents);

// GET /api/calendar/:id — Lấy chi tiết sự kiện theo ID
router.get('/:id', getCalendarEventById);

// POST /api/calendar — Gán outfit vào ngày (upsert: tạo mới hoặc ghi đè)
router.post('/', createOrReplaceCalendarEvent);

// PUT /api/calendar/:id — Cập nhật sự kiện
router.put('/:id', updateCalendarEvent);

// DELETE /api/calendar/by-date/:date — Xoá sự kiện theo ngày
router.delete('/by-date/:date', deleteCalendarEventByDate);

// DELETE /api/calendar/:id — Xoá sự kiện theo ID
router.delete('/:id', deleteCalendarEvent);

module.exports = router;
