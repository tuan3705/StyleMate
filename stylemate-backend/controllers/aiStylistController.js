/**
 * ═══════════════════════════════════════════════════════════════
 * 🎨 AI STYLIST CONTROLLER — Hybrid RAG Edition
 * ═══════════════════════════════════════════════════════════════
 */

const ClothingItem = require('../models/ClothingItem');
const Outfit = require('../models/Outfit');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');
const llmClient = require('../services/llmClient');
const contextService = require('../services/contextService');
const chatSessionService = require('../services/chatSessionService');
const closetSearchService = require('../services/closetSearchService');
const homeSuggestionService = require('../services/homeSuggestionService');

const styleAssessSchema = require('../schemas/style-assess.json');

// ───────────────────────────────────────────────────────────────
// 🛠️ HELPERS
// ───────────────────────────────────────────────────────────────

function toArray(val) {
  if (!val) return [];
  if (Array.isArray(val)) return val.filter(Boolean);
  try { return JSON.parse(val); } catch(e) { return String(val).split(',').map(s => s.trim()).filter(Boolean); }
}

function buildItemPayload(item) {
  return {
    id: String(item._id || item.id),
    name: item.name,
    category: item.category,
    color: item.color,
    brand: item.brand || '',
    image_url: item.imageNoBg || item.imageOriginal || ''
  };
}

async function hydrateOutfits(rawOutfits = [], userId) {
  const allItemIds = [...new Set(rawOutfits.flatMap(o => o.item_ids || []))];
  const itemsInDb = await ClothingItem.find({ _id: { $in: allItemIds } }).lean();
  const itemMap = new Map(itemsInDb.map(i => [String(i._id), i]));

  return rawOutfits.map(outfit => {
    const detail = (outfit.item_ids || []).map(id => {
      const dbItem = itemMap.get(String(id));
      return dbItem ? buildItemPayload(dbItem) : null;
    }).filter(Boolean);

    return {
      ...outfit,
      image_urls: detail.map(d => d.image_url),
      items_detail: detail
    };
  });
}

// ───────────────────────────────────────────────────────────────
// 🚀 ENDPOINTS
// ───────────────────────────────────────────────────────────────

/**
 * 💬 POST /api/ai-stylist/chat
 */
const postChat = asyncHandler(async (req, res) => {
  const { userId, message, sessionId, lat, lon } = req.body;
  if (!userId || !message) throw new AppError('Thiếu userId hoặc tin nhắn', 400);

  const relevantItems = await closetSearchService.findRelevantItems(userId, message);
  const baseContext = await contextService.buildContext({ userId, lat, lon });
  const context = { ...baseContext, closet: { items: relevantItems } };

  let session = sessionId ? chatSessionService.getSession(sessionId) : null;
  if (!session || session.userId !== userId) session = chatSessionService.createSession(userId, context);

  chatSessionService.addUserMessage(session.sessionId, message);

  const llmResponse = await llmClient.generateChatResponse({
    userId,
    message,
    context: { ...context, history: chatSessionService.getChatHistory(session.sessionId)?.messages || [] }
  });

  const suggestedOutfits = await hydrateOutfits(llmResponse.suggested_outfits, userId);
  chatSessionService.addAssistantMessage(session.sessionId, llmResponse.message, { suggestedOutfits });

  res.json({
    success: true,
    sessionId: session.sessionId,
    message: llmResponse.message,
    suggested_outfits: suggestedOutfits,
    followups: llmResponse.followups || []
  });
});

/**
 * ⚖️ POST /api/ai-stylist/style-assess
 */
const postStyleAssess = asyncHandler(async (req, res) => {
  const { userId, message, userPhoto } = req.body;
  const selectedOutfitIds = toArray(req.body.selectedOutfitIds);
  if (!userId) throw new AppError('Thiếu userId', 400);

  const outfits = await Outfit.find({ _id: { $in: selectedOutfitIds } }).lean();
  const context = await contextService.buildContext({ userId });

  const options = { mediaParts: [] };
  if (userPhoto) {
    if (userPhoto.startsWith('data:')) {
      const [header, data] = userPhoto.split(';base64,');
      options.mediaParts.push({ inlineData: { mimeType: header.split(':')[1], data } });
    }
    options.provider = 'gemini';
  }

  const response = await llmClient.generateStructuredResponse({
    message: message || "Đánh giá phong cách của tôi.",
    context: { ...context, selected_outfits: outfits },
    systemPrompt: `You are StyleMate critic. Evaluate fit and style. JSON only.`,
    options,
    validator: (data) => data.score !== undefined && Array.isArray(data.recommendations)
  });

  if (response.suggested_outfits) {
    response.suggested_outfits = await hydrateOutfits(response.suggested_outfits, userId);
  }
  res.json({ success: true, result: response });
});

/**
 * 🎨 POST /api/ai-stylist/color-analyze
 */
const postColorAnalyze = asyncHandler(async (req, res) => {
    const { userId, lat, lon } = req.body;
    if (!userId) throw new AppError('Thiếu userId', 400);

    const context = await contextService.buildContext({ userId, lat, lon });
    const mediaParts = await buildMediaPartsFromRequest(req);

    const response = await llmClient.generateStructuredResponse({
      message: "Phân tích màu sắc cá nhân của tôi.",
      context,
      systemPrompt: `Identify seasonal color palette based on photo or profile.`,
      options: { mediaParts, provider: 'gemini' }, // Luôn dùng Gemini cho vision precision
      validator: (data) => data.season_tone && Array.isArray(data.palette)
    });

    res.status(200).json({ success: true, result: response });
  });

/**
 * 🏠 GET /api/ai-stylist/home-suggestions
 */
const getHomeSuggestions = asyncHandler(async (req, res) => {
  const { userId, lat, lon } = req.query;
  if (!userId) throw new AppError('Thiếu userId', 400);

  const result = await homeSuggestionService.getHomeSuggestions(userId, lat, lon);
  res.json(result);
});

/**
 * 🏠 POST /api/ai-stylist/home-suggestions/refresh
 */
const refreshHomeSuggestions = asyncHandler(async (req, res) => {
  const { userId, lat, lon } = req.body;
  if (!userId) throw new AppError('Thiếu userId', 400);

  const result = await homeSuggestionService.refreshHomeSuggestions(userId, lat, lon);
  res.json(result);
});

/**
 * 📂 QUẢN LÝ CLOSET & SESSION
 */
const getClosetItems = asyncHandler(async (req, res) => {
  const items = await ClothingItem.find({ userId: req.query.userId }).sort({ createdAt: -1 }).lean();
  res.json({ success: true, items: items.map(buildItemPayload) });
});

const searchClosetItems = asyncHandler(async (req, res) => {
  const items = await closetSearchService.findRelevantItems(req.body.userId, req.body.query);
  res.json({ success: true, items: items.map(buildItemPayload) });
});

const getSession = asyncHandler(async (req, res) => {
  const history = chatSessionService.getChatHistory(req.params.sessionId);
  if (!history) throw new AppError('Session không tồn tại', 404);
  res.json({ success: true, ...history });
});

const deleteSession = asyncHandler(async (req, res) => {
  chatSessionService.deleteSession(req.params.sessionId);
  res.json({ success: true, message: 'Đã xóa session' });
});

const postHomeSuggestionAction = asyncHandler(async (req, res) => {
  res.json({ success: true, message: 'Action processed' });
});

const postStyleChat = asyncHandler(async (req, res) => {
  const response = await llmClient.generateStructuredResponse({
    message: req.body.message || 'Hello',
    context: { userId: req.body.userId },
    options: { provider: 'deepseek' }
  });
  res.json({ success: true, data: response });
});

module.exports = {
  postChat, postStyleAssess, postColorAnalyze, getHomeSuggestions, refreshHomeSuggestions,
  getClosetItems, searchClosetItems, getSession, deleteSession,
  postHomeSuggestionAction, postStyleChat
};
