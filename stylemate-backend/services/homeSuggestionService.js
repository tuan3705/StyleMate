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
 * Get outfit suggestions for the home page (Only using real UUIDs from DB)
 */
async function getHomeSuggestions(userId, lat, lon) {
  const context = await contextService.buildContext({ userId, lat, lon });
  const weatherText = context.weather?.current?.condition?.text || 'normal';
  const tempC = context.weather?.current?.temp_c || 25;

  console.log(`[HOME SUGGESTION] Request for user: ${userId}, Weather: ${weatherText}, Temp: ${tempC}C`);

  // 1. RAG: Fetch real items and real outfits from this user's closet
  let [relevantItems, relevantOutfits] = await Promise.all([
    closetSearchService.findRelevantItems(userId, `weather ${weatherText}, temperature ${tempC} degrees Celsius`),
    closetSearchService.findRelevantOutfits(userId, `weather ${weatherText}, temperature ${tempC} degrees Celsius`)
  ]);

  console.log(`[RAG LOG] Initial retrieval: ${relevantItems.length} items, ${relevantOutfits.length} outfits`);

  // Fallback: If no matching items found by heuristic, get the latest 20 items
  if (relevantItems.length === 0) {
    console.log(`[RAG LOG] Heuristic returned 0 items, falling back to latest items for user ${userId}`);
    const latestItems = await ClothingItem.find({ userId })
      .sort({ createdAt: -1 })
      .limit(20)
      .lean()
      .exec();

    console.log(`[RAG LOG] Found ${latestItems.length} items in DB directly.`);

    // Map to normalized format
    relevantItems = latestItems.map(item => ({
      id: String(item._id),
      name: item.name,
      category: item.category,
      color: item.color,
      brand: item.brand || '',
      occasion: item.occasion || '',
      season: item.season || ''
    }));
  }

  if (relevantItems.length === 0) {
    console.log(`[HOME SUGGESTION] Closet is empty for user ${userId}`);
    return {
      success: true,
      headline: "Hello!",
      message: "Your closet is empty. Add some clothes and AI will help you create outfits!",
      suggested_outfits: []
    };
  }

  // 2. Prepare context for AI (Add Season/Occasion for smarter selection)
  const itemsPool = relevantItems.map(i => `[${i.id}] Name: ${i.name}, Cat: ${i.category}, Color: ${i.color}, Season: ${i.season}, Occasion: ${i.occasion}`).join('\n');
  const outfitsPool = relevantOutfits.length > 0
    ? relevantOutfits.map(o => `[SAVED_OUTFIT:${o.id}] Name: ${o.name}, Contains: ${o.items.join(', ')}`).join('\n')
    : 'No saved outfits yet.';

  // 3. Enhanced System Prompt - English
  const systemPrompt = `You are StyleMate's Professional Wardrobe Orchestrator.
Target Weather: ${weatherText}, ${tempC}C.

INPUTS:
1. INDIVIDUAL ITEMS: A list of clothes with IDs in [brackets].
2. SAVED OUTFITS: Pre-made combinations with IDs like [SAVED_OUTFIT:uuid].

YOUR MISSION:
1. Suggest exactly 3 outfits for the user.
2. PRIORITY: If a SAVED OUTFIT fits the weather, recommend it! (Set type: "saved").
3. INNOVATION: Create NEW combinations from INDIVIDUAL ITEMS if they are more suitable. (Set type: "new").
4. Provide a 'headline' and 'message' in English. Make it warm, personal, and fashionable.

RULES:
- For 'type': use "saved" for pre-made outfits, "new" for your own combinations.
- For 'item_ids':
    - If type is "saved", provide the items belonging to that saved outfit.
    - If type is "new", provide the IDs of items you picked.
- ONLY use IDs provided in the pools. DO NOT invent IDs.

### INDIVIDUAL ITEMS POOL:
${itemsPool}

### SAVED OUTFITS POOL:
${outfitsPool}

Response must be a clean JSON object.`;

  try {
    const response = await llmClient.generateStructuredResponse({
      message: `Suggest 3 best outfits (prioritize saved ones if appropriate) for ${weatherText}, ${tempC}C.`,
      context: { weather: { condition: weatherText, temp: tempC } },
      systemPrompt,
      options: { provider: 'deepseek', temperature: 0.2 },
      responseSchema: homeSuggestionSchema
    });

    // 4. Hydration & Validation
    // AI might return "outfits" or "suggested_outfits" - handle both
    const outfitsArray = response.suggested_outfits || response.outfits || [];
    const itemIdsInPool = new Set(relevantItems.map(i => i.id));
    const allSuggestedItemIds = [...new Set(outfitsArray.flatMap(o => o.item_ids || []))];

    const itemsInDb = await ClothingItem.find({ _id: { $in: allSuggestedItemIds } }).lean();
    const itemMap = new Map(itemsInDb.map(i => [String(i._id), i]));

    const finalOutfits = outfitsArray.map(outfit => {
      const details = (outfit.item_ids || [])
        .filter(id => itemMap.has(id))
        .map(id => {
          const item = itemMap.get(id);
          return { id, name: item.name, image_url: item.imageNoBg || item.imageOriginal };
        });

      if (details.length === 0) return null;

      return {
        id: outfit.id,
        type: outfit.type || "new",
        item_ids: details.map(d => d.id),
        reason: outfit.reason,
        image_urls: details.reduce((acc, curr, idx) => { acc[`item_${idx}`] = curr.image_url; return acc; }, {}),
        items_detail: details
      };
    }).filter(Boolean);

    // Extract headline and message from the first outfit if not present at top level
    // AI returns these inside each outfit, not at the top-level response
    const firstOutfit = outfitsArray.length > 0 ? outfitsArray[0] : null;
    return {
      success: true,
      headline: response.headline || firstOutfit?.headline || (firstOutfit?.message?.split('.')[0]) || "Personal style",
      message: response.message || firstOutfit?.message || firstOutfit?.reason || "Have a great day looking stylish!",
      suggested_outfits: finalOutfits
    };

  } catch (error) {
    console.error('❌ [HomeSuggestion Fidelity Failure]:', error.message);
    throw error;
  }
}

module.exports = { getHomeSuggestions };