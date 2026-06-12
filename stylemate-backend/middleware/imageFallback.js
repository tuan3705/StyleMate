/**
 * 🖼️ Image Fallback Middleware
 * 
 * Xử lý các request ảnh cũ có path /uploads/xxx.jpg
 * (không có subfolder items/) để fallback tìm file
 * ở cả uploads/root và uploads/items/
 */
const path = require('path');
const fs = require('fs');

const uploadsDir = path.join(__dirname, '..', 'uploads');
const itemsDir = path.join(uploadsDir, 'items');
const tryonDir = path.join(uploadsDir, 'tryon');

/**
 * Middleware kiểm tra file ảnh tồn tại trước khi pass sang static.
 * Nếu file không tồn tại ở đường dẫn gốc /uploads/,
 * thử tìm trong /uploads/items/
 */
function imageFallback(req, res, next) {
  // Chỉ xử lý request ảnh
  const requestPath = req.path; // ví dụ: /items/abc.jpg hoặc /abc.jpg
  const requestedFile = requestPath.startsWith('/') ? requestPath.slice(1) : requestPath;
  
  // Kiểm tra file trong uploads gốc trước
  const rootFile = path.join(uploadsDir, requestedFile);
  if (fs.existsSync(rootFile)) {
    // File tồn tại → để static serve xử lý
    return next();
  }

  // Nếu request đến /uploads/something.jpg và không có trong root,
  // thử tìm trong thư mục items/
  const itemsFile = path.join(itemsDir, requestedFile);
  if (fs.existsSync(itemsFile)) {
    // Redirect đến file trong items/
    return res.sendFile(itemsFile);
  }

  // Thử tìm trong tryon/
  const tryonFile = path.join(tryonDir, requestedFile);
  if (fs.existsSync(tryonFile)) {
    return res.sendFile(tryonFile);
  }

  // Không tìm thấy → để static serve xử lý như bình thường (sẽ trả 404)
  next();
}

module.exports = imageFallback;