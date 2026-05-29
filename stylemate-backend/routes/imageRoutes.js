const express = require('express');
const router = express.Router();
const { upload, removeBgFromImage } = require('../controllers/removeBgController');

// POST /api/images/remove-bg — remove background using remove.bg
router.post('/remove-bg', upload.single('image'), removeBgFromImage);

module.exports = router;

