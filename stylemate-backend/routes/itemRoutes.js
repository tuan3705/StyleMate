const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const itemController = require('../controllers/itemController');

const UPLOAD_DIR = path.join(__dirname, '..', 'uploads', 'items');
if (!fs.existsSync(UPLOAD_DIR)) fs.mkdirSync(UPLOAD_DIR, { recursive: true });

const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, UPLOAD_DIR);
  },
  filename: function (req, file, cb) {
    const filename = `${Date.now().toString(36)}_${file.originalname.replace(/\s+/g, '_')}`;
    cb(null, filename);
  }
});

const upload = multer({ storage });

// POST /api/items/upload
router.post('/upload', upload.single('image'), itemController.uploadItem);

// GET status for upload job
router.get('/upload/:jobId/status', itemController.getUploadStatus);

// DELETE temp assets / cancel job
router.delete('/upload/:jobId', itemController.deleteTempJob);

module.exports = router;
