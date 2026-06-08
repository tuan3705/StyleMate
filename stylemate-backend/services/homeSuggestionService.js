/**
 * ═══════════════════════════════════════════════════════════════
 * 🏠 HOME SUGGESTION SERVICE — DeepSeek Migration
 * ═══════════════════════════════════════════════════════════════
 *
 * Dịch vụ cung cấp gợi ý trang phục hàng ngày trên màn hình chính.
 * Tự động phân tích thời tiết và ngữ cảnh người dùng qua DeepSeek.
 *
 * ───────────────────────────────────────────────────────────────
 */

const llmClient = require('./llmClient');
const contextService = require('./contextService');

// Bộ nhớ đệm đơn giản (Cache) để giảm tần suất gọi AI
const suggestionsCache = new Map();
const CACHE_TTL = 30 * 60 * 1000; // 30 phút

/**
 * Lấy gợi ý trang phục cho trang chủ
 */
async function getHomeSuggestions(userId, location) {
  const cacheKey = `${userId}_${location}`;

  // 1. Kiểm tra Cache
  if (suggestionsCache.has(cacheKey)) {
    const cached = suggestionsCache.get(cacheKey);
    if (Date.now() - cached.timestamp < CACHE_TTL) {
      return cached.data;
    }
  }

  // 2. Xây dựng ngữ cảnh (Thời tiết, Closet)
  const [lat, lon] = (location || '').split(',');
  const context = await contextService.buildContext({ userId, lat, lon });

  // 3. Gọi DeepSeek để tạo gợi ý
  const systemPrompt = `You are the StyleMate Daily Assistant.
Based on the current weather and user closet, suggest 3 appropriate outfits.
Response MUST be a JSON object with a 'suggestions' array.
Each suggestion: { "id", "label", "reason", "items": ["id1", "id2"], "confidence" }`;

  try {
    const response = await llmClient.generateStructuredResponse({
      message: 'Hãy gợi ý trang phục phù hợp cho tôi hôm nay.',
      context,
      systemPrompt,
      options: { temperature: 0.4 }
    });

    const result = {
      suggestions: response.suggestions || [],
      generatedAt: new Date().toISOString(),
      weatherContext: context.weather?.current?.condition?.text || 'Bình thường'
    };

    // 4. Lưu Cache và trả về
    suggestionsCache.set(cacheKey, { timestamp: Date.now(), data: result });
    return result;

  } catch (error) {
    console.error('❌ [HomeSuggestion Error]:', error.message);
    // Trả về dữ liệu trống thay vì lỗi để không làm sập UI trang chủ
    return { suggestions: [], generatedAt: new Date().toISOString(), error: 'AI tạm thời vắng mặt' };
  }
}

/**
 * Làm mới gợi ý (Force Refresh)
 */
async function refreshHomeSuggestions(userId, location) {
  const cacheKey = `${userId}_${location}`;
  suggestionsCache.delete(cacheKey);
  return getHomeSuggestions(userId, location);
}

module.exports = {
  getHomeSuggestions,
  refreshHomeSuggestions
};
