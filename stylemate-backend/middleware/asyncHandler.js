/**
 * 🔄 Async Handler Middleware
 * 
 * Wrapper cho các async route handler để tự động bắt lỗi
 * và chuyển đến error handler mà không cần try-catch thủ công.
 * 
 * @param {Function} fn - Async route handler
 * @returns {Function} Express middleware function
 */
const asyncHandler = (fn) => (req, res, next) => {
  Promise.resolve(fn(req, res, next)).catch(next);
};

module.exports = asyncHandler;
