/**
 * 🖼️ Upload Controller
 *
 * Xử lý upload file ảnh lên server.
 * File được lưu vào thư mục uploads/ với tên unique (timestamp).
 *
 * Endpoint: POST /api/clothes/upload
 *
 * Request: multipart/form-data với field "image" chứa file ảnh
 * Response: { success: true, url: "/uploads/ten_file.jpg" }
 */
const path = require('path');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');

/**
 * Multer configuration — Xử lý upload file
 */
const multer = require('multer');

// Cấu hình nơi lưu file và tên file
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        const uploadDir = path.join(__dirname, '..', 'uploads');
        cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
        // Lấy phần mở rộng của file gốc (vd: .jpg, .png)
        const ext = path.extname(file.originalname).toLowerCase();
        // Tạo tên unique: timestamp + random 4 số + đuôi gốc
        const uniqueName = `${Date.now()}-${Math.round(Math.random() * 10000)}${ext}`;
        cb(null, uniqueName);
    }
});

// Chỉ chấp nhận file ảnh
const fileFilter = (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|gif|webp|bmp/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);

    if (extname && mimetype) {
        cb(null, true);
    } else {
        cb(new AppError('Chỉ chấp nhận file ảnh (jpg, png, gif, webp, bmp)', 400), false);
    }
};

// Giới hạn kích thước 10MB
const upload = multer({
    storage: storage,
    limits: { fileSize: 10 * 1024 * 1024 },
    fileFilter: fileFilter
});

/**
 * POST /api/clothes/upload
 * Upload một file ảnh, trả về URL tương đối.
 *
 * @param {string} req.file - File ảnh được upload (field name: "image")
 * @returns {JSON} { success: true, url: "/uploads/abc.jpg" }
 */
const uploadImage = asyncHandler(async (req, res) => {
    if (!req.file) {
        return res.status(400).json({
            success: false,
            message: 'Vui lòng chọn file ảnh để upload'
        });
    }

    // Trả về đường dẫn tương đối (relative URL)
    const url = `/uploads/${req.file.filename}`;

    console.log(`📤 Ảnh đã upload: ${req.file.filename} (${(req.file.size / 1024).toFixed(1)} KB)`);

    res.status(200).json({
        success: true,
        url: url
    });
});

module.exports = {
    upload,
    uploadImage
};
