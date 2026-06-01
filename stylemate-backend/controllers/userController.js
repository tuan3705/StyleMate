/**
 * 📱 User Controller
 * 
 * Quản lý thông tin thiết bị người dùng và FCM Token.
 * Dùng cho tính năng Push Notification (do bạn tôi phát triển).
 */
const UserDevice = require('../models/UserDevice');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');

/**
 * 📋 POST /api/user/fcm-token
 * 
 * Lưu hoặc cập nhật FCM Token cho một userId.
 * 
 * Cơ chế upsert:
 *   - Nếu userId đã tồn tại: Cập nhật fcmToken mới.
 *   - Nếu fcmToken đã tồn tại (ở user khác): Cập nhật userId cho token đó.
 *   - Nếu chưa có: Tạo mới.
 * 
 * Body:
 *   - userId: String (bắt buộc) — UUID do Client sinh
 *   - fcmToken: String (bắt buộc) — FCM Token từ Firebase
 *   - latitude: Number (tùy chọn) — Vĩ độ
 *   - longitude: Number (tùy chọn) — Kinh độ
 *
 * Response 200 (update): { success: true, message: "Đã cập nhật", data: {...} }
 * Response 201 (create): { success: true, message: "Đã tạo mới", data: {...} }
 */
const saveOrUpdateFcmToken = asyncHandler(async (req, res) => {
  const { userId, fcmToken, latitude, longitude } = req.body;

  // Validation
  if (!userId) {
    return res.status(400).json({
      success: false,
      message: 'Trường userId là bắt buộc'
    });
  }

  if (!fcmToken || fcmToken.trim() === '') {
    return res.status(400).json({
      success: false,
      message: 'Trường fcmToken là bắt buộc và không được để trống'
    });
  }

  if (latitude !== undefined && Number.isNaN(Number(latitude))) {
    return res.status(400).json({
      success: false,
      message: 'latitude phải là số hợp lệ'
    });
  }

  if (longitude !== undefined && Number.isNaN(Number(longitude))) {
    return res.status(400).json({
      success: false,
      message: 'longitude phải là số hợp lệ'
    });
  }

  const trimmedToken = fcmToken.trim();
  const locationUpdate = {
    ...(latitude !== undefined ? { latitude: Number(latitude) } : {}),
    ...(longitude !== undefined ? { longitude: Number(longitude) } : {})
  };

  // Kiểm tra xem user này đã có record chưa
  const existingByUser = await UserDevice.findOne({ userId });

  if (existingByUser) {
    // User đã tồn tại → Cập nhật token
    existingByUser.fcmToken = trimmedToken;
    if (Object.keys(locationUpdate).length > 0) {
      Object.assign(existingByUser, locationUpdate);
    }
    existingByUser.updatedAt = Date.now();
    await existingByUser.save();

    return res.status(200).json({
      success: true,
      message: 'Đã cập nhật FCM Token cho user',
      data: existingByUser
    });
  }

  // Kiểm tra xem token này đã được đăng ký ở user khác chưa
  const existingByToken = await UserDevice.findOne({ fcmToken: trimmedToken });

  if (existingByToken) {
    // Token đã tồn tại → Cập nhật userId cho token này
    existingByToken.userId = userId;
    if (Object.keys(locationUpdate).length > 0) {
      Object.assign(existingByToken, locationUpdate);
    }
    existingByToken.updatedAt = Date.now();
    await existingByToken.save();

    return res.status(200).json({
      success: true,
      message: 'FCM Token đã tồn tại, cập nhật userId',
      data: existingByToken
    });
  }

  // Chưa có gì → Tạo mới
  const newDevice = await UserDevice.create({
    userId,
    fcmToken: trimmedToken,
    ...locationUpdate,
    createdAt: Date.now(),
    updatedAt: Date.now()
  });

  res.status(201).json({
    success: true,
    message: 'Đã lưu FCM Token thành công',
    data: newDevice
  });
});

/**
 * 📋 GET /api/user/fcm-token/:userId
 * 
 * Lấy FCM Token của một user.
 * Dùng để kiểm tra hoặc debug.
 * 
 * Response: { success: true, data: { ... } }
 * Error 404: { success: false, message: "..." }
 */
const getFcmTokenByUserId = asyncHandler(async (req, res, next) => {
  const { userId } = req.params;

  const device = await UserDevice.findOne({ userId });

  if (!device) {
    return next(new AppError(`Không tìm thấy FCM Token cho userId: ${userId}`, 404));
  }

  res.status(200).json({
    success: true,
    data: device
  });
});

/**
 * ❌ DELETE /api/user/fcm-token/:userId
 * 
 * Xoá record của một user (khi user đăng xuất).
 * 
 * Response: { success: true, message: "...", data: {} }
 * Error 404: { success: false, message: "..." }
 */
const deleteFcmTokenByUserId = asyncHandler(async (req, res, next) => {
  const { userId } = req.params;

  const deletedDevice = await UserDevice.findOneAndDelete({ userId });

  if (!deletedDevice) {
    return next(new AppError(`Không tìm thấy FCM Token cho userId: ${userId}`, 404));
  }

  res.status(200).json({
    success: true,
    message: `Đã xoá FCM Token cho userId: ${userId}`,
    data: {}
  });
});

module.exports = {
  saveOrUpdateFcmToken,
  getFcmTokenByUserId,
  deleteFcmTokenByUserId
};
