/**
 * ═══════════════════════════════════════════════════════════════
 * 🎨 AI STYLIST CONTROLLER — DeepSeek Migration
 * ═══════════════════════════════════════════════════════════════
 *
 * Điều khiển các tính năng AI: Chat, Đánh giá Style, Phân tích Màu sắc.
 * Đã chuyển đổi từ Gemini sang DeepSeek.
 *
 * ───────────────────────────────────────────────────────────────
 */

const ClothingItem = require('../models/ClothingItem');
const Outfit = require('../models/Outfit');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');
const llmClient = require('../services/llmClient');
const contextService = require('../services/contextService');
const chatSessionService = require('../services/chatSessionService');

// Schemas định dạng phản hồi
const styleAssessSchema = require('../schemas/style-assess.json');
const colorAnalysisSchema = require('../schemas/color-analysis.json');

/**
 * Tiện ích: Chuyển đổi giá trị sang mảng
 */
function toArray(value) {
  if (value == null) return [];
  if (Array.isArray(value)) return value.filter(Boolean);
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      return Array.isArray(parsed) ? parsed.filter(Boolean) : [value];
    } catch (e) {
      return value.split(',').map(s => s.trim()).filter(Boolean);
    }
  }
  return [value];
}

/**
 * Tiện ích: Chuẩn hóa dữ liệu món đồ gửi lên AI
 */
function buildItemPayload(item) {
  return {
    id: item._id,
    name: item.name || 'Unnamed',
    category: item.category || 'Other',
    color: item.color || 'Unknown',
    brand: item.brand || '',
    season: item.season || '',
    occasion: item.occasion || '',
    image_url: item.imageNoBg || item.imageOriginal || ''
  };
}

/**
 * Hydrate: Làm giàu dữ liệu gợi ý từ AI bằng thông tin thực từ Database
 */
async function hydrateSuggestedOutfits(rawOutfits = [], closetItems = [], selectedItemIds = []) {
  const itemMap = new Map(closetItems.map(i => [String(i.id), i]));

  return rawOutfits.map((outfit, idx) => {
    const itemIds = toArray(outfit.item_ids || outfit.items || []);
    const details = itemIds.map(id => itemMap.get(String(id))).filter(Boolean);

    return {
      id: outfit.id || `suggested_${idx}`,
      item_ids: itemIds,
      reason: outfit.reason || '',
      confidence: outfit.confidence || 0.8,
      image_urls: details.map(d => d.image_url),
      items_detail: details
    };
  });
}

/**
 * 💬 POST /api/ai-stylist/chat
 * Chat với AI Stylist
 */
const postChat = asyncHandler(async (req, res) => {
  const { userId, message, sessionId, lat, lon } = req.body;
  const selectedItemIds = toArray(req.body.selectedItemIds);

  if (!userId || !message) {
    throw new AppError('Thiếu userId hoặc tin nhắn', 400);
  }

  // 1. Xây dựng ngữ cảnh (Thời tiết, Hồ sơ, Tủ đồ)
  const context = await contextService.buildContext({ userId, lat, lon, selectedItemIds });

  // 2. Quản lý Session
  let session = sessionId ? chatSessionService.getSession(sessionId) : null;
  if (!session || session.userId !== userId) {
    session = chatSessionService.createSession(userId, context);
  } else {
    chatSessionService.updateContext(session.sessionId, context);
  }

  // 3. Ghi nhận tin nhắn người dùng
  chatSessionService.addUserMessage(session.sessionId, message, { selectedItemIds });

  // 4. Gọi DeepSeek
  const llmResponse = await llmClient.generateChatResponse({
    userId,
    message,
    context: {
      ...context,
      history: chatSessionService.getChatHistory(session.sessionId)?.messages || []
    }
  });

  // 5. Chuẩn hóa kết quả
  const closetItems = (context.closet?.items || []).map(buildItemPayload);
  const suggestedOutfits = await hydrateSuggestedOutfits(
    llmResponse.suggested_outfits,
    closetItems,
    selectedItemIds
  );

  // 6. Lưu phản hồi AI vào session
  chatSessionService.addAssistantMessage(session.sessionId, llmResponse.message, { suggestedOutfits });

  res.status(200).json({
    success: true,
    sessionId: session.sessionId,
    message: llmResponse.message,
    suggested_outfits: suggestedOutfits,
    followups: llmResponse.followups || []
  });
});

/**
 * ⚖️ POST /api/ai-stylist/style-assess
 * Đánh giá phong cách
 */
const postStyleAssess = asyncHandler(async (req, res) => {
  const { userId, message, lat, lon } = req.body;
  const selectedOutfitIds = toArray(req.body.selectedOutfitIds);

  if (!userId) throw new AppError('Thiếu userId', 400);

  const context = await contextService.buildContext({ userId, lat, lon });

  // Lấy chi tiết bộ đồ cần đánh giá
  const outfitDetails = await Outfit.find({ _id: { $in: selectedOutfitIds } }).lean();

  const systemPrompt = `You are StyleMate, a professional fashion critic.
Analyze the user's outfit and provide a detailed assessment in JSON.
Schema: ${JSON.stringify(styleAssessSchema)}`;

  const response = await llmClient.generateStructuredResponse({
    message: message || "Hãy đánh giá bộ đồ này của tôi.",
    context: { ...context, outfits: outfitDetails },
    systemPrompt,
    validator: (data) => data.score !== undefined && data.recommendations
  });

  res.status(200).json({ success: true, result: response });
});

/**
 * 🎨 POST /api/ai-stylist/color-analyze
 * Phân tích màu sắc cá nhân
 */
const postColorAnalyze = asyncHandler(async (req, res) => {
  const { userId, lat, lon } = req.body;
  if (!userId) throw new AppError('Thiếu userId', 400);

  const context = await contextService.buildContext({ userId, lat, lon });

  const systemPrompt = `You are a Personal Color Analysis expert.
Identify the user's seasonal color palette based on their profile and closet.
Return JSON matching: ${JSON.stringify(colorAnalysisSchema)}`;

  const response = await llmClient.generateStructuredResponse({
    message: "Phân tích màu sắc cá nhân của tôi.",
    context,
    systemPrompt,
    validator: (data) => data.season_tone && data.palette
  });

  res.status(200).json({ success: true, result: response });
});

/**
 * 🏠 GET /api/ai-stylist/home-suggestions
 * Gợi ý mặc gì hôm nay (Trang chủ)
 */
const getHomeSuggestions = asyncHandler(async (req, res) => {
  const { userId, lat, lon } = req.query;
  const context = await contextService.buildContext({ userId, lat, lon });

  const response = await llmClient.generateStructuredResponse({
    message: "Gợi ý 3 bộ đồ phù hợp cho thời tiết và lịch trình hôm nay.",
    context,
    systemPrompt: "Respond with a JSON object containing a 'suggestions' array.",
    mockResponse: { suggestions: [] } // Fallback
  });

  res.json({ success: true, suggestions: response.suggestions || [] });
});

/**
 * 📂 Các hàm bổ trợ quản lý Closet & Session (Giữ nguyên logic nghiệp vụ)
 */
const getClosetItems = asyncHandler(async (req, res) => {
  const { userId } = req.query;
  const items = await ClothingItem.find({ userId }).sort({ createdAt: -1 }).lean();
  res.json({ success: true, items: items.map(buildItemPayload) });
});

const getSession = asyncHandler(async (req, res) => {
  const { sessionId } = req.params;
  const history = chatSessionService.getChatHistory(sessionId);
  if (!history) throw new AppError('Session không tồn tại', 404);
  res.json({ success: true, ...history });
});

const deleteSession = asyncHandler(async (req, res) => {
  chatSessionService.deleteSession(req.params.sessionId);
  res.json({ success: true, message: 'Đã xóa session' });
});

module.exports = {
  postChat,
  postStyleAssess,
  postColorAnalyze,
  getHomeSuggestions,
  getClosetItems,
  getSession,
  deleteSession
};
