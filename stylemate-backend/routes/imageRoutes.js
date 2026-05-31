const express = require('express');
const router = express.Router();
const { upload, removeBgFromImage } = require('../controllers/removeBgController');
const { upload: aiUpload, aiFillFromImage } = require('../controllers/aiFillController');

// POST /api/images/remove-bg — remove background using remove.bg
router.post('/remove-bg', upload.single('image'), removeBgFromImage);

// POST /api/images/ai-fill — AI auto-fill fields from image
router.post('/ai-fill', aiUpload.single('image'), aiFillFromImage);

module.exports = router;

