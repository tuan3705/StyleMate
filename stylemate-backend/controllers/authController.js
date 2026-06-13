/**
 * 🔐 Auth Controller
 *
 * Đăng nhập/đăng xuất và refresh token.
 */
const bcrypt = require('bcryptjs');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');
const User = require('../models/User');
const {
  signAccessToken,
  signRefreshToken,
  verifyRefreshToken,
  hashToken
} = require('../utils/tokenUtils');

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const normalizeEmail = (email) => email.trim().toLowerCase();
const normalizeName = (name) => (name || '').trim().replace(/\s+/g, ' ');

const validateCredentials = (email, password) => {
  if (!email || !emailRegex.test(email)) {
    throw new AppError('Email không hợp lệ', 400);
  }

  if (!password || password.length < 6) {
    throw new AppError('Mật khẩu tối thiểu 6 ký tự', 400);
  }
};

const buildAuthResponse = (user, accessToken, refreshToken, isNewUser) => {
  return {
    success: true,
    message: isNewUser ? 'Tạo tài khoản và đăng nhập thành công' : 'Đăng nhập thành công',
    data: {
      user: {
        id: user._id,
        email: user.email,
        name: user.name || ''
      },
      accessToken,
      refreshToken,
      isNewUser
    }
  };
};

/**
 * POST /api/auth/login
 * - Nếu email chưa tồn tại thì tự tạo user
 */
const login = asyncHandler(async (req, res) => {
  const { email, password } = req.body;
  validateCredentials(email, password);

  const normalizedEmail = normalizeEmail(email);

  const user = await User.findOne({ email: normalizedEmail }).select('+passwordHash +refreshTokenHash');

  if (!user) {
    throw new AppError('Tài khoản không tồn tại', 404);
  }

  const isValid = await bcrypt.compare(password, user.passwordHash);
  if (!isValid) {
    throw new AppError('Sai email hoặc mật khẩu', 401);
  }

  const accessToken = signAccessToken(user);
  const refreshToken = signRefreshToken(user);

  user.refreshTokenHash = hashToken(refreshToken);
  await user.save();

  res.status(200).json(buildAuthResponse(user, accessToken, refreshToken, false));
});

/**
 * POST /api/auth/refresh
 */
const refresh = asyncHandler(async (req, res) => {
  const { refreshToken } = req.body;

  if (!refreshToken) {
    throw new AppError('Thiếu refresh token', 400);
  }

  let payload;
  try {
    payload = verifyRefreshToken(refreshToken);
  } catch (error) {
    throw new AppError('Refresh token không hợp lệ', 401);
  }

  const user = await User.findById(payload.sub).select('+refreshTokenHash');

  if (!user) {
    throw new AppError('User không tồn tại', 401);
  }

  if (user.tokenVersion !== payload.tokenVersion) {
    throw new AppError('Token đã bị thu hồi', 401);
  }

  if (!user.refreshTokenHash || user.refreshTokenHash !== hashToken(refreshToken)) {
    throw new AppError('Refresh token không hợp lệ', 401);
  }

  const accessToken = signAccessToken(user);

  res.status(200).json({
    success: true,
    message: 'Làm mới access token thành công',
    data: { accessToken }
  });
});

/**
 * POST /api/auth/logout
 */
const logout = asyncHandler(async (req, res) => {
  const user = req.user;

  user.tokenVersion += 1;
  user.refreshTokenHash = null;
  await user.save();

  res.status(200).json({
    success: true,
    message: 'Đăng xuất thành công'
  });
});

/**
 * POST /api/auth/change-password
 */
const changePassword = asyncHandler(async (req, res) => {
  const { currentPassword, newPassword } = req.body;

  if (!currentPassword || currentPassword.length < 6) {
    throw new AppError('Mat khau hien tai toi thieu 6 ky tu', 400);
  }

  if (!newPassword || newPassword.length < 6) {
    throw new AppError('Mat khau moi toi thieu 6 ky tu', 400);
  }

  if (currentPassword === newPassword) {
    throw new AppError('Mat khau moi phai khac mat khau hien tai', 400);
  }

  const user = await User.findById(req.user._id).select('+passwordHash +refreshTokenHash');
  if (!user) {
    throw new AppError('User khong ton tai', 401);
  }

  const isValid = await bcrypt.compare(currentPassword, user.passwordHash);
  if (!isValid) {
    throw new AppError('Mat khau hien tai khong dung', 401);
  }

  user.passwordHash = await bcrypt.hash(newPassword, 10);
  await user.save();

  res.status(200).json({
    success: true,
    message: 'Doi mat khau thanh cong'
  });
});

/**
 * POST /api/auth/register
 */
const register = asyncHandler(async (req, res) => {
  const { email, password, name } = req.body;
  validateCredentials(email, password);

  const normalizedName = normalizeName(name);
  if (!normalizedName) {
    throw new AppError('Tên người dùng là bắt buộc', 400);
  }

  const normalizedEmail = normalizeEmail(email);
  const existingUser = await User.findOne({ email: normalizedEmail });

  if (existingUser) {
    throw new AppError('Email đã tồn tại', 409);
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = await User.create({
    email: normalizedEmail,
    name: normalizedName,
    passwordHash
  });

  const accessToken = signAccessToken(user);
  const refreshToken = signRefreshToken(user);

  user.refreshTokenHash = hashToken(refreshToken);
  await user.save();

  res.status(201).json(buildAuthResponse(user, accessToken, refreshToken, true));
});

module.exports = {
  register,
  login,
  refresh,
  logout,
  changePassword
};
