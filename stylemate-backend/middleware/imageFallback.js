/**
 * 🖼️ Image Fallback Middleware
 * 
 * Handles old image requests with path /uploads/xxx.jpg
 * (without subfolder items/) to fallback and find file
 * in both uploads/root and uploads/items/
 */
const path = require('path');
const fs = require('fs');

const uploadsDir = path.join(__dirname, '..', 'uploads');
const itemsDir = path.join(uploadsDir, 'items');
const tryonDir = path.join(uploadsDir, 'tryon');

/**
 * Middleware to check if image file exists before passing to static.
 * If file doesn't exist in root /uploads/ path,
 * tries to find in /uploads/items/
 */
function imageFallback(req, res, next) {
  // Only process image requests
  const requestPath = req.path; // e.g.: /items/abc.jpg or /abc.jpg
  const requestedFile = requestPath.startsWith('/') ? requestPath.slice(1) : requestPath;
  
  // Check file in root uploads first
  const rootFile = path.join(uploadsDir, requestedFile);
  if (fs.existsSync(rootFile)) {
    // File exists → let static serve handle it
    return next();
  }

  // If request is for /uploads/something.jpg and not in root,
  // try to find in items/ directory
  const itemsFile = path.join(itemsDir, requestedFile);
  if (fs.existsSync(itemsFile)) {
    // Redirect to file in items/
    return res.sendFile(itemsFile);
  }

  // Try to find in tryon/
  const tryonFile = path.join(tryonDir, requestedFile);
  if (fs.existsSync(tryonFile)) {
    return res.sendFile(tryonFile);
  }

  // Not found → let static serve handle it normally (will return 404)
  next();
}

module.exports = imageFallback;