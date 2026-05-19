/**
 * 🛡️ Auth Middleware
 *
 * Kiểm tra access token và gắn user vào req.
 */
const User = require('../models/User');
const asyncHandler = require('./asyncHandler');
const { AppError } = require('./errorHandler');
const { verifyAccessToken } = require('../utils/tokenUtils');

const requireAuth = asyncHandler(async (req, res, next) => {
  const authHeader = req.headers.authorization || '';
  const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : null;

  if (!token) {
    return next(new AppError('Thiếu access token', 401));
  }

  let payload;
  try {
    payload = verifyAccessToken(token);
  } catch (error) {
    return next(new AppError('Access token không hợp lệ', 401));
  }

  const user = await User.findById(payload.sub).select('+refreshTokenHash');

  if (!user) {
    return next(new AppError('User không tồn tại', 401));
  }

  if (user.tokenVersion !== payload.tokenVersion) {
    return next(new AppError('Token đã bị thu hồi', 401));
  }

  req.user = user;
  next();
});

module.exports = {
  requireAuth
};

