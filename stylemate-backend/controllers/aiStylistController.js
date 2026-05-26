/**
 * controllers/aiStylistController.js
 *
 * Phase 1 controller: LLM integration and structured JSON output.
 */
const llmClient = require('../services/llmClient');
const contextService = require('../services/contextService');

/**
 * POST /api/ai-stylist/chat
 */
async function postChat(req, res, next) {
  try {
    const { userId, message, selectedItemIds = [] } = req.body;

    if (!message || typeof message !== 'string') {
      return res.status(400).json({ success: false, error: 'Missing `message` in request body' });
    }

    // Build richer context: weather + closet summary (stubs for Phase 2)
    const { lat, lon } = req.body;
    const injectedContext = await contextService.buildContext({ userId, lat, lon, selectedItemIds });

    const context = {
      userId,
      selectedItemIds,
      injected: injectedContext
    };

    const llmResponse = await llmClient.generateChatResponse({ userId, message, context });

    // Basic validation: ensure object has message and suggested_outfits array
    if (!llmResponse || typeof llmResponse !== 'object' || !llmResponse.message) {
      return res.status(502).json({ success: false, error: 'Invalid response from LLM' });
    }

    return res.status(200).json({ success: true, ...llmResponse });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  postChat
};
