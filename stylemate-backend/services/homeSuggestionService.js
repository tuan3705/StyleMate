/**
 * ═══════════════════════════════════════════════════════════════
 * 🏠 HOME SUGGESTION SERVICE — Truth-Only RAG
 * ═══════════════════════════════════════════════════════════════
 */

const llmClient = require('./llmClient');
const contextService = require('./contextService');
const closetSearchService = require('./closetSearchService');
const ClothingItem = require('../models/ClothingItem');

const homeSuggestionSchema = require('../schemas/home-suggestion.json');

/**
 * Lấy gợi ý trang phục cho trang chủ (Chỉ dùng UUID thật từ DB)
 */
async function getHomeSuggestions(userId, lat, lon) {
  const context = await contextService.buildContext({ userId, lat, lon });
  const weatherText = context.weather?.current?.condition?.text || 'bình thường';
  const tempC = context.weather?.current?.temp_c || 25;

  // 1. RAG: Lấy đồ THẬT từ tủ đồ của chính user này
  const relevantItems = await closetSearchService.findRelevantItems(
    userId,
    `thời tiết ${weatherText}, nhiệt độ ${tempC} độ C`
  );

  console.log(`[RAG LOG] Found ${relevantItems.length} real items for user ${userId}`);

  if (relevantItems.length === 0) {
    return {
      success: true,
      headline: "Chào ngày mới!",
      message: "Tủ đồ của bạn đang trống, hãy thêm đồ để AI giúp bạn phối đồ nhé!",
      suggested_outfits: []
    };
  }

  // 2. Chuẩn bị danh sách UUID và mô tả để AI chọn
  const itemsPool = relevantItems.map(i => `[${i.id}] Category: ${i.category}, Color: ${i.color}, Name: ${i.name}`).join('\n');

  // 3. Strictest System Prompt: Cấm AI sáng tác ID
  const systemPrompt = `You are StyleMate's Professional Wardrobe Orchestrator.
I will give you a list of REAL clothes from the user's database with their EXACT IDs in brackets [like-this].

YOUR ONLY TASKS:
1. Select 1-3 combinations of clothes from the provided list to form outfits suitable for: ${weatherText}, ${tempC}C.
2. Provide a 'headline' and 'message' in Vietnamese about the day's style.
3. For 'suggested_outfits', ONLY use the IDs found in the list below.

DANGER: DO NOT generate names like 'ao-thun' or 'quan-jean'.
DANGER: YOU MUST ONLY RETURN THE IDs IN BRACKETS.
DANGER: If an ID is not in the list, DO NOT use it.

### REAL CLOSET POOL:
${itemsPool}

Response must be a clean JSON object.`;

  try {
    const response = await llmClient.generateStructuredResponse({
      message: `Create 1-3 outfit suggestions using ONLY the real items listed above.`,
      context: { weather: { condition: weatherText, temp: tempC } },
      systemPrompt,
      options: { provider: 'deepseek', temperature: 0.1 }, // Logic cực kỳ chặt chẽ
      responseSchema: homeSuggestionSchema
    });

    // 4. Hydration & Strict Matching: Chỉ lấy những gì AI chọn mà có trong Database
    const poolIds = new Set(relevantItems.map(i => i.id));
    const allItemIds = [...new Set((response.suggested_outfits || []).flatMap(o => o.item_ids))];

    // Lọc lại một lần nữa ở server để đảm bảo không có ID lạ lọt qua
    const validatedIds = allItemIds.filter(id => poolIds.has(id));
    const itemsInDb = await ClothingItem.find({ _id: { $in: validatedIds } }).lean();
    const itemMap = new Map(itemsInDb.map(i => [String(i._id), i]));

    const finalOutfits = (response.suggested_outfits || []).map(outfit => {
      const details = (outfit.item_ids || [])
        .filter(id => itemMap.has(id)) // Chỉ lấy ID có trong DB
        .map(id => {
          const item = itemMap.get(id);
          return { id, name: item.name, image_url: item.imageNoBg || item.imageOriginal };
        });

      if (details.length === 0) return null;

      return {
        id: outfit.id,
        item_ids: details.map(d => d.id),
        reason: outfit.reason,
        image_urls: details.reduce((acc, curr, idx) => { acc[`item_${idx}`] = curr.image_url; return acc; }, {}),
        items_detail: details
      };
    }).filter(Boolean);

    return {
      success: true,
      headline: response.headline || "Phong cách cá nhân",
      message: response.message || "Chúc bạn một ngày mặc đẹp!",
      suggested_outfits: finalOutfits
    };

  } catch (error) {
    console.error('❌ [HomeSuggestion Fidelity Failure]:', error.message);
    throw error;
  }
}

module.exports = { getHomeSuggestions };
