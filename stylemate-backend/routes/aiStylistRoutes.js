const express = require('express');
const router = express.Router();

const aiStylistController = require('../controllers/aiStylistController');

// Debug: confirm this routes file is loaded at server start
console.log('[routes] ai-stylist loaded');

// POST /api/ai-stylist/chat
router.post('/chat', aiStylistController.postChat);

// GET /api/ai-stylist/closet/items
router.get('/closet/items', aiStylistController.getClosetItems);

// POST /api/ai-stylist/closet/items/search
router.post('/closet/items/search', aiStylistController.searchClosetItems);

// GET /api/ai-stylist/sessions/:sessionId
router.get('/sessions/:sessionId', aiStylistController.getSession);

// DELETE /api/ai-stylist/sessions/:sessionId
router.delete('/sessions/:sessionId', aiStylistController.deleteSession);

// POST /api/ai-stylist/style-assess
router.post('/style-assess', aiStylistController.postStyleAssess);

// POST /api/ai-stylist/color-analyze
router.post('/color-analyze', aiStylistController.postColorAnalyze);

// POST /api/ai-stylist/style-chat
router.post('/style-chat', aiStylistController.postStyleChat);

// GET /api/ai-stylist/home-suggestions
router.get('/home-suggestions', aiStylistController.getHomeSuggestions);

// POST /api/ai-stylist/home-suggestions/refresh
router.post('/home-suggestions/refresh', aiStylistController.refreshHomeSuggestions);

// POST /api/ai-stylist/home-suggestions/:id/action
router.post('/home-suggestions/:id/action', aiStylistController.postHomeSuggestionAction);

module.exports = router;
