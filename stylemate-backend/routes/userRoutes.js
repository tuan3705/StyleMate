/**
 * 📱 User Routes
 * 
 * Định tuyến cho API quản lý FCM Token của thiết bị người dùng.
 * 
 * Base path: /api/user
 */
const express = require('express');
const router = express.Router();
const {
  saveOrUpdateFcmToken,
  getFcmTokenByUserId,
  deleteFcmTokenByUserId
} = require('../controllers/userController');

// POST /api/user/fcm-token — Lưu/cập nhật FCM Token
router.post('/fcm-token', saveOrUpdateFcmToken);

// GET /api/user/fcm-token/:userId — Lấy FCM Token theo userId
router.get('/fcm-token/:userId', getFcmTokenByUserId);

// DELETE /api/user/fcm-token/:userId — Xoá FCM Token theo userId
router.delete('/fcm-token/:userId', deleteFcmTokenByUserId);

module.exports = router;
