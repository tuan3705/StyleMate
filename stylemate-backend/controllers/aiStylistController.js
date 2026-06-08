/**
 * controllers/aiStylistController.js
 *
 * AI-only endpoints for chat, closet browsing, style assessment, and color analysis.
 * Core wardrobe/outfit logic remains in existing modules.
 */
const axios = require('axios');
const ClothingItem = require('../models/ClothingItem');
const Outfit = require('../models/Outfit');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');
const llmClient = require('../services/llmClient');
const contextService = require('../services/contextService');
const chatSessionService = require('../services/chatSessionService');

const styleAssessSchema = require('../schemas/style-assess.json');
const colorAnalysisSchema = require('../schemas/color-analysis.json');

function toArray(value) {
  if (value == null) return [];
  if (Array.isArray(value)) return value.filter(Boolean);
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      if (Array.isArray(parsed)) return parsed.filter(Boolean);
    } catch (error) {
      return value.split(',').map((entry) => entry.trim()).filter(Boolean);
    }
    return value.split(',').map((entry) => entry.trim()).filter(Boolean);
  }
  return [value].filter(Boolean);
}

function clampScore(value, min = 0, max = 10) {
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return min;
  return Math.min(max, Math.max(min, numeric));
}

function buildItemPayload(item) {
  return {
    id: item._id,
    name: item.name || '',
    category: item.category || '',
    color: item.color || '',
    brand: item.brand || '',
    season: item.season || '',
    occasion: item.occasion || '',
    image_url: item.imageNoBg || item.imageOriginal || ''
  };
}

function buildRelatedItemPayload(item) {
  return {
    id: item.id,
    name: item.name,
    image_url: item.image_url || ''
  };
}

async function fetchClosetItemsByUser(userId, filters = {}, pagination = {}) {
  const query = {};
  if (userId) {
    query.userId = userId;
  }

  const categories = toArray(filters.category || filters.categories);
  const colors = toArray(filters.color || filters.colors);
  const brands = toArray(filters.brand || filters.brands);
  const sizes = toArray(filters.size || filters.sizes);
  const occasions = toArray(filters.occasion || filters.occasions);
  const seasons = toArray(filters.season || filters.seasons);
  const searchQuery = String(filters.query || filters.search || '').trim();

  if (categories.length > 0) query.category = { $in: categories };
  if (colors.length > 0) query.color = { $in: colors };
  if (brands.length > 0) query.brand = { $in: brands };
  if (sizes.length > 0) query.size = { $in: sizes };
  if (occasions.length > 0) query.occasion = { $in: occasions };
  if (seasons.length > 0) query.season = { $in: seasons };
  if (searchQuery) {
    query.$or = [
      { name: { $regex: searchQuery, $options: 'i' } },
      { brand: { $regex: searchQuery, $options: 'i' } }
    ];
  }

  const page = Math.max(1, Number(pagination.page || 1));
  const pageSize = Math.max(1, Math.min(100, Number(pagination.pageSize || pagination.limit || 20)));
  const skip = (page - 1) * pageSize;

  try {
    const [items, total] = await Promise.all([
      ClothingItem.find(query).sort({ createdAt: -1 }).skip(skip).limit(pageSize).lean().exec(),
      ClothingItem.countDocuments(query)
    ]);

    return {
      items: items.map(buildItemPayload),
      total,
      page,
      pageSize,
      filters: {
        category: categories,
        color: colors,
        brand: brands,
        size: sizes,
        occasion: occasions,
        season: seasons,
        query: searchQuery
      }
    };
  } catch (error) {
    console.warn('[aiStylistController] closet query failed:', error.message);
    return {
      items: [],
      total: 0,
      page,
      pageSize,
      filters: {
        category: categories,
        color: colors,
        brand: brands,
        size: sizes,
        occasion: occasions,
        season: seasons,
        query: searchQuery
      }
    };
  }
}

async function fetchOutfitsByIds(userId, outfitIds = []) {
  const ids = toArray(outfitIds);
  if (ids.length === 0) return [];

  try {
    const outfits = await Outfit.find({ userId, _id: { $in: ids } }).lean().exec();
    const itemIds = outfits.flatMap((outfit) => (outfit.clothingItems || []).map((entry) => entry.clothingItemId));
    const items = itemIds.length > 0
      ? await ClothingItem.find({ _id: { $in: itemIds } }).lean().exec()
      : [];
    const itemMap = new Map(items.map((item) => [item._id, item]));

    return outfits.map((outfit) => {
      const outfitItemIds = (outfit.clothingItems || []).map((entry) => entry.clothingItemId);
      return {
        id: outfit._id,
        name: outfit.name || '',
        item_ids: outfitItemIds,
        image_urls: outfitItemIds.map((itemId) => itemMap.get(itemId)?.imageNoBg || itemMap.get(itemId)?.imageOriginal || '').filter(Boolean),
        items_detail: outfitItemIds.map((itemId) => {
          const item = itemMap.get(itemId);
          return item ? buildItemPayload(item) : { id: itemId, name: '', category: '', color: '', brand: '', image_url: '' };
        })
      };
    });
  } catch (error) {
    console.warn('[aiStylistController] outfit query failed:', error.message);
    return [];
  }
}

function chooseFallbackItems(closetItems = [], selectedItemIds = []) {
  if (selectedItemIds.length > 0) {
    return selectedItemIds;
  }
  return closetItems.slice(0, 3).map((item) => item.id);
}

async function hydrateSuggestedOutfits(rawOutfits = [], closetItems = [], selectedItemIds = []) {
  const closetItemMap = new Map(closetItems.map((item) => [item.id, item]));
  const fallbackIds = chooseFallbackItems(closetItems, selectedItemIds);

  return rawOutfits.map((outfit, index) => {
    const itemIds = toArray(outfit.item_ids || outfit.items || outfit.clothing_item_ids);
    const normalizedItemIds = itemIds.length > 0 ? itemIds : fallbackIds.slice(0, 3);
    const relatedItems = normalizedItemIds.map((itemId) => closetItemMap.get(itemId)).filter(Boolean);

    return {
      id: outfit.id || `outfit_${index + 1}`,
      item_ids: normalizedItemIds,
      reason: outfit.reason || outfit.description || '',
      confidence: Number.isFinite(Number(outfit.confidence)) ? Number(outfit.confidence) : 0.7,
      image_urls: relatedItems.map((item) => item.image_url).filter(Boolean),
      items_detail: relatedItems
    };
  });
}

function buildStyleAssessPrompt(context, outfitDetails, hasPhoto) {
  return [
    'You are StyleMate, a professional style assessor.',
    'Return a single JSON object matching the schema exactly.',
    'Analyze outfit fit, style coherence, color harmony, occasion suitability, and practical styling improvements.',
    hasPhoto ? 'User provided a photo. Consider posture, grooming, confidence, and presentation.' : 'No photo provided. Base the assessment on wardrobe and outfit context only.',
    'Provide at least one concrete recommendation and at least one follow-up question.',
    outfitDetails.length > 0 ? `Selected outfits:\n${JSON.stringify(outfitDetails)}` : 'No explicit outfits selected.',
    context?.summaryText ? `Context summary: ${context.summaryText}` : ''
  ].filter(Boolean).join('\n\n');
}

function buildColorAnalyzePrompt(context, hasPhoto) {
  return [
    'You are StyleMate, a personal color analysis assistant.',
    'Return a single JSON object matching the schema exactly.',
    'Infer the most likely season tone and build a practical palette with confidence scores.',
    hasPhoto ? 'A user photo is available and should influence the tone assessment.' : 'No photo provided; use profile and wardrobe context to infer the best possible palette.',
    context?.summaryText ? `Context summary: ${context.summaryText}` : ''
  ].filter(Boolean).join('\n\n');
}

function buildStyleAssessFallback(context, outfitDetails, selectedItemIds, selectedOutfitIds) {
  const closetItems = (context?.closet?.items || []).map((item) => buildItemPayload({
    _id: item.id,
    ...item,
    imageNoBg: item.image,
    imageOriginal: item.image
  }));

  return {
    message: 'Style assessment temporarily used a fallback summary because the provider is unavailable.',
    score: 7,
    confidence: 0.5,
    outfit_analysis: {
      fit: 'The selected pieces appear balanced.',
      style_coherence: 'The outfit looks coordinated overall.',
      color_harmony: 'The palette is reasonably consistent.',
      occasion_appropriateness: 'Suitable for a casual setting.',
      overall_impression: 'A solid base outfit with room for refinement.'
    },
    photo_assessment: {
      posture: 'Not analyzed in fallback mode.',
      grooming: 'Not analyzed in fallback mode.',
      confidence_level: 'Not analyzed in fallback mode.',
      photography_quality: 'Not analyzed in fallback mode.'
    },
    recommendations: [
      {
        id: 'fallback_rec_1',
        text: 'Add one accessory to sharpen the look.',
        reason: 'A small finishing piece can improve overall polish.',
        category: 'accessory',
        priority: 'medium',
        confidence: 0.5,
        related_items: closetItems.slice(0, 2).map(buildRelatedItemPayload)
      }
    ],
    suggested_outfits: outfitDetails.length > 0
      ? outfitDetails.map((outfit) => ({
          id: outfit.id,
          item_ids: outfit.item_ids,
          reason: outfit.name || 'Existing closet outfit',
          confidence: 0.6,
          image_urls: outfit.image_urls,
          items_detail: outfit.items_detail
        }))
      : [{
          id: 'fallback_outfit_1',
          item_ids: selectedItemIds.length > 0 ? selectedItemIds : closetItems.slice(0, 3).map((item) => item.id),
          reason: 'A simple fallback outfit suggestion from your closet.',
          confidence: 0.6,
          image_urls: closetItems.slice(0, 3).map((item) => item.image_url).filter(Boolean),
          items_detail: closetItems.slice(0, 3)
        }],
    styling_tips: [
      { tip: 'Keep proportions clean', detail: 'Use one standout piece and keep the rest simple.' }
    ],
    closet_optimization: {
      strengths: ['You have some wearable basics.'],
      gaps: ['Add a versatile accessory.'],
      color_palette: ['Neutral tones', 'Dark basics'],
      recommended_additions: ['One accessory', 'One layering piece']
    },
    followup_questions: ['Do you want it more formal?', 'Should I suggest alternative shoes?']
  };
}

function buildColorAnalyzeFallback(context) {
  return {
    season_tone: 'summer_cool',
    season_label: 'Fallback color analysis',
    palette: [
      { name: 'Soft blue', hex: '#7FA6D6', rgb: 'rgb(127, 166, 214)', score: 0.6, role: 'primary', contrast_text_hex: '#FFFFFF' },
      { name: 'Warm gray', hex: '#A0A0A0', rgb: 'rgb(160, 160, 160)', score: 0.5, role: 'neutral', contrast_text_hex: '#FFFFFF' }
    ],
    primary_recommendation: 'Use cool neutrals and soft blue accents as a safe starting point.',
    notes: 'Fallback mode used because the AI provider is temporarily unavailable.',
    followups: ['Which colors should I avoid?', 'Can you suggest outfits for this palette?']
  };
}

async function buildMediaPartsFromRequest(req) {
  const mediaParts = [];
  const candidateImages = [req.body?.userPhoto, req.body?.imageUrl, req.body?.bodyImageBase64].filter(Boolean);

  for (const candidate of candidateImages) {
    if (typeof candidate !== 'string') continue;

    if (candidate.startsWith('data:')) {
      const match = candidate.match(/^data:(.+?);base64,(.+)$/i);
      if (match) {
        mediaParts.push({
          inlineData: {
            mimeType: match[1],
            data: match[2]
          }
        });
      }
      continue;
    }

    const absoluteUrl = candidate.startsWith('http://') || candidate.startsWith('https://')
      ? candidate
      : `${req.protocol}://${req.get('host')}${candidate.startsWith('/') ? candidate : `/${candidate}`}`;

    try {
      const response = await axios.get(absoluteUrl, { responseType: 'arraybuffer', timeout: 10000 });
      const mimeType = response.headers['content-type'] || 'image/jpeg';
      mediaParts.push({
        inlineData: {
          mimeType,
          data: Buffer.from(response.data).toString('base64')
        }
      });
    } catch (error) {
      console.warn('[aiStylistController] Could not load media part:', error.message);
    }
  }

  return mediaParts;
}

/**
 * POST /api/ai-stylist/chat
 */
const postChat = asyncHandler(async (req, res) => {
  const { userId, message } = req.body;
  const selectedItemIds = toArray(req.body.selectedItemIds);
  const sessionIdInput = req.body.sessionId || null;
  const lat = req.body.lat;
  const lon = req.body.lon;

  if (!userId) {
    throw new AppError('Missing `userId` in request body', 400);
  }

  if (!message || typeof message !== 'string') {
    throw new AppError('Missing `message` in request body', 400);
  }

  const injectedContext = await contextService.buildContext({ userId, lat, lon, selectedItemIds });
  const session = sessionIdInput ? chatSessionService.getSession(sessionIdInput) : null;
  const activeSession = session && session.userId === userId
    ? session
    : chatSessionService.createSession(userId, {
        weather: injectedContext.weather,
        location: injectedContext.weather?.location || null,
        userProfile: injectedContext.profile,
        selectedItems: injectedContext.closet?.items || []
      });

  chatSessionService.updateContext(activeSession.sessionId, {
    weather: injectedContext.weather,
    location: injectedContext.weather?.location || null,
    userProfile: injectedContext.profile,
    selectedItems: injectedContext.closet?.items || []
  });
  chatSessionService.addUserMessage(activeSession.sessionId, message, { selectedItemIds });

  const llmResponse = await llmClient.generateChatResponse({
    userId,
    message,
    context: {
      ...injectedContext,
      sessionId: activeSession.sessionId,
      selectedItemIds
    }
  });

  const closetItems = (injectedContext.closet?.items || []).map((item) => ({
    id: item.id,
    name: item.name,
    category: item.category,
    color: item.color,
    brand: item.brand || '',
    image_url: item.image || ''
  }));

  const suggestedOutfits = await hydrateSuggestedOutfits(llmResponse.suggested_outfits || [], closetItems, selectedItemIds);

  chatSessionService.addAssistantMessage(activeSession.sessionId, llmResponse.message, {
    suggestedOutfits
  });

  return res.status(200).json({
    success: true,
    sessionId: activeSession.sessionId,
    message: llmResponse.message,
    suggested_outfits: suggestedOutfits
  });
});

/**
 * GET /api/ai-stylist/closet/items
 */
const getClosetItems = asyncHandler(async (req, res) => {
  const { userId } = req.query;
  if (!userId) {
    throw new AppError('Missing `userId` query parameter', 400);
  }

  const result = await fetchClosetItemsByUser(userId, req.query, req.query);

  return res.status(200).json({
    success: true,
    ...result
  });
});

/**
 * POST /api/ai-stylist/closet/items/search
 */
const searchClosetItems = asyncHandler(async (req, res) => {
  const { userId } = req.body;
  if (!userId) {
    throw new AppError('Missing `userId` in request body', 400);
  }

  const result = await fetchClosetItemsByUser(userId, req.body, req.body);

  return res.status(200).json({
    success: true,
    ...result
  });
});

/**
 * GET /api/ai-stylist/sessions/:sessionId
 */
const getSession = asyncHandler(async (req, res) => {
  const { sessionId } = req.params;
  const session = chatSessionService.getChatHistory(sessionId);

  if (!session) {
    throw new AppError('Session not found or expired', 404);
  }

  return res.status(200).json({
    success: true,
    ...session
  });
});

/**
 * DELETE /api/ai-stylist/sessions/:sessionId
 */
const deleteSession = asyncHandler(async (req, res) => {
  const { sessionId } = req.params;
  const deleted = chatSessionService.deleteSession(sessionId);

  if (!deleted) {
    throw new AppError('Session not found', 404);
  }

  return res.status(200).json({
    success: true,
    message: 'Session deleted'
  });
});

/**
 * POST /api/ai-stylist/style-assess
 */
const postStyleAssess = asyncHandler(async (req, res) => {
  const { userId } = req.body;
  const selectedItemIds = toArray(req.body.selectedItemIds);
  const selectedOutfitIds = toArray(req.body.selectedOutfitIds);
  const message = req.body.message || 'Assess my style';
  const lat = req.body.lat;
  const lon = req.body.lon;

  if (!userId) {
    throw new AppError('Missing `userId` in request body', 400);
  }

  const context = await contextService.buildContext({ userId, lat, lon, selectedItemIds });
  const outfitDetails = await fetchOutfitsByIds(userId, selectedOutfitIds);
  const mediaParts = await buildMediaPartsFromRequest(req);

  const response = await llmClient.generateStructuredResponse({
    message,
    context: {
      ...context,
      selectedItemIds,
      selectedOutfitIds,
      outfitDetails
    },
    options: {
      mediaParts,
      temperature: 0.2,
      maxOutputTokens: 4096,
      provider: 'gemini-rest',
      model: process.env.GEMINI_MODEL || 'gemini-2.5-flash'
    },
    systemPrompt: buildStyleAssessPrompt(context, outfitDetails, mediaParts.length > 0),
    responseSchema: styleAssessSchema,
    validator: (payload) => Boolean(payload && typeof payload.message === 'string' && typeof payload.score === 'number' && Array.isArray(payload.recommendations))
  });

  const closetItems = (context.closet?.items || []).map((item) => buildItemPayload({
    _id: item.id,
    ...item,
    imageNoBg: item.image,
    imageOriginal: item.image
  }));

  const recommendations = Array.isArray(response.recommendations) && response.recommendations.length > 0
    ? response.recommendations.map((item) => ({
        ...item,
        related_items: Array.isArray(item.related_items)
          ? item.related_items.map(buildRelatedItemPayload)
          : item.related_items
      }))
    : [{
        text: 'Add one accessory to improve balance',
        reason: 'A simple accessory can improve the visual finish of the outfit.',
        priority: 'medium',
        confidence: 0.5,
        related_items: closetItems.slice(0, 2).map(buildRelatedItemPayload)
      }];

  const suggestedOutfits = Array.isArray(response.suggested_outfits) && response.suggested_outfits.length > 0
    ? response.suggested_outfits
    : outfitDetails.map((outfit) => ({
        id: outfit.id,
        item_ids: outfit.item_ids,
        reason: outfit.name || 'Existing outfit from your closet',
        confidence: 0.75,
        image_urls: outfit.image_urls,
        items_detail: outfit.items_detail
      }));

  return res.status(200).json({
    success: true,
    result: {
      message: response.message || 'Style assessment completed',
      score: clampScore(response.score ?? 0),
      confidence: Number.isFinite(Number(response.confidence)) ? Number(response.confidence) : 0.7,
      outfit_analysis: response.outfit_analysis || {
        fit: 'Balanced overall fit',
        style_coherence: 'The selected items work together',
        color_harmony: 'The palette is reasonably coordinated',
        occasion_appropriateness: 'Suitable for the chosen occasion',
        overall_impression: 'Solid outfit composition'
      },
      photo_assessment: response.photo_assessment || {
        posture: 'N/A',
        grooming: 'N/A',
        confidence_level: 'N/A',
        photography_quality: 'N/A'
      },
      recommendations,
      suggested_outfits: suggestedOutfits,
      styling_tips: Array.isArray(response.styling_tips) ? response.styling_tips : [],
      closet_optimization: response.closet_optimization || {
        strengths: [],
        gaps: [],
        color_palette: [],
        recommended_additions: []
      },
      followup_questions: Array.isArray(response.followup_questions) ? response.followup_questions : ['Can you make this more formal?', 'What should I add to improve the look?']
    }
  });
});

/**
 * POST /api/ai-stylist/color-analyze
 */
const postColorAnalyze = asyncHandler(async (req, res) => {
  const { userId } = req.body;
  if (!userId) {
    throw new AppError('Missing `userId` in request body', 400);
  }

  const context = await contextService.buildContext({ userId, lat: req.body.lat, lon: req.body.lon, selectedItemIds: toArray(req.body.selectedItemIds) });
  const mediaParts = await buildMediaPartsFromRequest(req);

  const response = await llmClient.generateStructuredResponse({
    message: req.body.message || 'Analyze my color season and palette',
    context,
    options: {
      mediaParts,
      temperature: 0.2,
      maxOutputTokens: 3072,
      provider: 'gemini-rest',
      model: process.env.GEMINI_MODEL || 'gemini-2.5-flash'
    },
    systemPrompt: buildColorAnalyzePrompt(context, mediaParts.length > 0),
    responseSchema: colorAnalysisSchema,
    validator: (payload) => Boolean(payload && typeof payload.season_tone === 'string' && Array.isArray(payload.palette) && typeof payload.primary_recommendation === 'string')
  });

  const palette = Array.isArray(response.palette) && response.palette.length > 0
    ? response.palette
    : [
        { name: 'Neutral', hex: '#808080', rgb: 'rgb(128, 128, 128)', score: 0.5, role: 'neutral', contrast_text_hex: '#FFFFFF' }
      ];

  return res.status(200).json({
    success: true,
    result: {
      season_tone: response.season_tone || 'summer_cool',
      season_label: response.season_label || 'StyleMate suggestion',
      palette,
      primary_recommendation: response.primary_recommendation || 'Use your strongest neutral tones as the base.',
      notes: response.notes || '',
      followups: Array.isArray(response.followups) && response.followups.length > 0 ? response.followups : ['Which colors should I avoid?', 'What colors work best for formal outfits?']
    }
  });
});

const homeSuggestionService = require('../services/homeSuggestionService');
const fitAnalysisService = require('../services/fitAnalysisService');

/**
 * GET /api/ai-stylist/home-suggestions
 */
const getHomeSuggestions = asyncHandler(async (req, res) => {
  const { userId, location } = req.query;
  const result = await homeSuggestionService.getHomeSuggestions(userId, location);
  res.json({ success: true, ...result });
});

/**
 * POST /api/ai-stylist/home-suggestions/refresh
 */
const refreshHomeSuggestions = asyncHandler(async (req, res) => {
  const { userId, location } = req.body;
  const result = await homeSuggestionService.refreshHomeSuggestions(userId, location);
  res.json({ success: true, ...result });
});

/**
 * POST /api/ai-stylist/home-suggestions/:id/action
 */
const postHomeSuggestionAction = asyncHandler(async (req, res) => {
  const { id } = req.params;
  const { action } = req.body;
  // Stub implementation
  res.json({ success: true, message: `Action ${action} initiated for ${id}` });
});

/**
 * POST /api/ai-stylist/style-chat
 */
const postStyleChat = asyncHandler(async (req, res) => {
  const { userId, message } = req.body;
  
  // Call LLM for style chat logic (stub)
  const response = await llmClient.generateStructuredResponse({
    message: message || 'Hello, need deep style advice',
    context: {}, // Ideally contextService.buildContext(...)
    options: { schemaType: 'style-chat' }
  });
  
  res.json({ success: true, data: response });
});

module.exports = {
  postChat,
  getClosetItems,
  searchClosetItems,
  getSession,
  deleteSession,
  postStyleAssess,
  postColorAnalyze,
  getHomeSuggestions,
  refreshHomeSuggestions,
  postHomeSuggestionAction,
  postStyleChat
};