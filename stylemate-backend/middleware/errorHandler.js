/**
 * 🚨 Error Handler Middleware
 * 
 * Middleware xử lý lỗi tập trung cho toàn bộ ứng dụng.
 * Bắt tất cả lỗi từ controller và trả về JSON thống nhất.
 */

/**
 * Custom AppError class — cho phép gán statusCode và message
 */
class AppError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.statusCode = statusCode;
    this.isOperational = true;
    Error.captureStackTrace(this, this.constructor);
  }
}

/**
 * Middleware xử lý lỗi 404 — route không tồn tại
 */
const notFoundHandler = (req, res, next) => {
  const error = new AppError(
    `❓ Không tìm thấy route: ${req.originalUrl}`,
    404
  );
  next(error);
};

/**
 * Middleware xử lý lỗi tổng quát
 */
const globalErrorHandler = (err, req, res, next) => {
  let statusCode = err.statusCode || 500;
  let message = err.message || 'Lỗi máy chủ nội bộ';

  // Xử lý lỗi Mongoose Validation Error
  if (err.name === 'ValidationError') {
    statusCode = 400;
    const messages = Object.values(err.errors).map((e) => e.message);
    message = `Lỗi validation: ${messages.join(', ')}`;
  }

  // Xử lý lỗi Mongoose Duplicate Key (unique constraint)
  if (err.code === 11000) {
    statusCode = 409;
    const field = Object.keys(err.keyValue).join(', ');
    message = `Dữ liệu bị trùng lặp: ${field} đã tồn tại`;
  }

  // Xử lý lỗi Mongoose CastError (ID không hợp lệ)
  if (err.name === 'CastError') {
    statusCode = 400;
    message = `Giá trị không hợp lệ cho field ${err.path}: ${err.value}`;
  }

  if (process.env.NODE_ENV !== 'production') {
    console.error('❌ Lỗi chi tiết:', {
      message: err.message,
      stack: err.stack,
      statusCode
    });
  }

  res.status(statusCode).json({
    success: false,
    message,
    ...(process.env.NODE_ENV !== 'production' && { stack: err.stack })
  });
};

module.exports = {
  AppError,
  notFoundHandler,
  globalErrorHandler
};
