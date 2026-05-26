const express = require('express');
const router = express.Router();

const aiStylistController = require('../controllers/aiStylistController');

// POST /api/ai-stylist/chat
router.post('/chat', aiStylistController.postChat);

module.exports = router;
