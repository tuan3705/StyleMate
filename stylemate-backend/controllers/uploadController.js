/**
 * 🖼️ Upload Controller
 *
 * Handles image file upload to the server.
 * Files are saved to the uploads/ directory with a unique name (timestamp).
 *
 * Endpoint: POST /api/clothes/upload
 *
 * Request: multipart/form-data with field "image" containing the image file
 * Response: { success: true, url: "/uploads/file_name.jpg" }
 */
const path = require('path');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');

/**
 * Multer configuration — File upload handling
 */
const multer = require('multer');

// Configure storage location and file naming
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        const uploadDir = path.join(__dirname, '..', 'uploads', 'items');
        // Auto-create directory if not exists
        const fs = require('fs');
        if (!fs.existsSync(uploadDir)) {
            fs.mkdirSync(uploadDir, { recursive: true });
        }
        cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
        // Get the original file extension (e.g. .jpg, .png)
        const ext = path.extname(file.originalname).toLowerCase();
        // Create unique name: timestamp + random 4 digits + original extension
        const uniqueName = `${Date.now()}-${Math.round(Math.random() * 10000)}${ext}`;
        cb(null, uniqueName);
    }
});

// Only accept image files
const fileFilter = (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|gif|webp|bmp/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);

    if (extname && mimetype) {
        cb(null, true);
    } else {
        cb(new AppError('Only image files are accepted (jpg, png, gif, webp, bmp)', 400), false);
    }
};

// Limit file size to 10MB
const upload = multer({
    storage: storage,
    limits: { fileSize: 10 * 1024 * 1024 },
    fileFilter: fileFilter
});

/**
 * POST /api/clothes/upload
 * Upload an image file, returns a relative URL.
 *
 * @param {string} req.file - The uploaded image file (field name: "image")
 * @returns {JSON} { success: true, url: "/uploads/abc.jpg" }
 */
const uploadImage = asyncHandler(async (req, res) => {
    if (!req.file) {
        return res.status(400).json({
            success: false,
        message: 'Please select an image file to upload'
        });
    }

    // Return relative URL - saved in items/ subfolder
    const url = `/uploads/items/${req.file.filename}`;

    console.log(`📤 Image uploaded: ${req.file.filename} (${(req.file.size / 1024).toFixed(1)} KB)`);

    res.status(200).json({
        success: true,
        url: url
    });
});

module.exports = {
    upload,
    uploadImage
};
