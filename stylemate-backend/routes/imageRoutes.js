const express = require('express');
const router = express.Router();
const { upload, removeBgFromImage } = require('../controllers/removeBgController');
const { upload: aiUpload, aiFillFromImage } = require('../controllers/aiFillController');
const { upload: autoTagUpload, autoTaggingFromImage } = require('../controllers/aiAutoTaggingController');

// POST /api/images/remove-bg — remove background using remove.bg
router.post('/remove-bg', upload.single('image'), removeBgFromImage);

// POST /api/images/ai-fill — AI auto-fill fields from image
router.post('/ai-fill', aiUpload.single('image'), aiFillFromImage);

// POST /api/images/auto-tagging — experimental season/occasion suggestion
router.post('/auto-tagging', autoTagUpload.single('image'), autoTaggingFromImage);

module.exports = router;

