const express = require('express');
const router = express.Router();

const aiStylistController = require('../controllers/aiStylistController');

// Debug: confirm this routes file is loaded at server start
console.log('[routes] ai-stylist loaded');

// POST /api/ai-stylist/chat
router.post('/chat', aiStylistController.postChat);

module.exports = router;
