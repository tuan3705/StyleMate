const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const { requireAuth } = require('../middleware/authMiddleware');
const virtualTryOnController = require('../controllers/virtualTryOnController');
const { saveTryOnToCollection } = require('../controllers/saveTryOnController');

// Ensure upload dir exists
const UPLOAD_DIR = path.join(__dirname, '..', 'uploads', 'tryon');
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

// Kickoff try-on (multipart/form-data)
router.post('/', upload.fields([{ name: 'bodyImage', maxCount: 1 }, { name: 'itemImages', maxCount: 5 }]), virtualTryOnController.kickoffTryOn);

// Kickoff try-on alias used by docs and Android clients
router.post('/create', upload.fields([{ name: 'bodyImage', maxCount: 1 }, { name: 'itemImages', maxCount: 5 }]), virtualTryOnController.kickoffTryOn);

// Status
router.get('/:jobId/status', virtualTryOnController.getStatus);

// Result
router.get('/:jobId/result', virtualTryOnController.getResult);

// Follow-up question
router.post('/:jobId/followup', virtualTryOnController.postFollowup);

// Save try-on result to user's collection (clothing items) - yêu cầu xác thực
router.post('/:jobId/save-to-collection', requireAuth, saveTryOnToCollection);

module.exports = router;
