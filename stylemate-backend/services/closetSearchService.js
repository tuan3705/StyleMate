/**
 * ═══════════════════════════════════════════════════════════════
 * 🔍 CLOSET SEARCH SERVICE (RAG Core)
 * ═══════════════════════════════════════════════════════════════
 */

const ClothingItem = require('../models/ClothingItem');
const mongoose = require('mongoose');

/**
 * Tìm các món đồ liên quan nhất cho yêu cầu (Heuristic RAG)
 */
async function findRelevantItems(userId, queryText, limit = 15) {
  // 1. Kiểm tra ID hợp lệ (Tránh lỗi Cast to ObjectId)
  if (!userId || !mongoose.Types.ObjectId.isValid(userId)) {
    console.warn(`⚠️ [RAG] Invalid userId format: "${userId}". Skipping search.`);
    return [];
  }

  const text = (queryText || '').toLowerCase();
  const searchFilter = { userId: new mongoose.Types.ObjectId(userId) };

  const contextMap = {
    'tiệc': ['Formal', 'Party', 'Evening'],
    'party': ['Formal', 'Party', 'Evening'],
    'formal': ['Formal'],
    'đi chơi': ['Casual', 'Streetwear'],
    'dạo phố': ['Casual', 'Streetwear'],
    'casual': ['Casual'],
    'hẹn hò': ['Casual', 'Elegant'],
    'date': ['Elegant'],
    'đi làm': ['Work', 'Business'],
    'công sở': ['Work', 'Business'],
    'văn phòng': ['Work', 'Business'],
    'office': ['Work', 'Business'],
    'work': ['Work', 'Business'],
    'thể thao': ['Sports', 'Active'],
    'gym': ['Sports', 'Active'],
    'chạy bộ': ['Sports', 'Active'],
    'sports': ['Sports', 'Active'],
    'mùa hè': ['Summer'],
    'nắng': ['Summer'],
    'nóng': ['Summer'],
    'summer': ['Summer'],
    'sunny': ['Summer'],
    'hot': ['Summer'],
    'mùa đông': ['Winter'],
    'lạnh': ['Winter'],
    'mưa': ['Winter', 'Autumn'],
    'winter': ['Winter'],
    'cold': ['Winter'],
    'rain': ['Winter', 'Autumn'],
    'mùa thu': ['Autumn', 'Fall'],
    'mát': ['Autumn', 'Spring'],
    'autumn': ['Autumn', 'Fall'],
    'fall': ['Autumn', 'Fall'],
    'mùa xuân': ['Spring'],
    'spring': ['Spring']
  };

  const foundOccasions = [];
  const foundSeasons = [];

  for (const [key, values] of Object.entries(contextMap)) {
    if (text.includes(key)) {
      if (['mùa hè', 'nắng', 'nóng', 'summer', 'sunny', 'hot', 'mùa đông', 'lạnh', 'mưa', 'winter', 'cold', 'rain', 'mùa thu', 'mát', 'autumn', 'fall', 'mùa xuân', 'spring'].includes(key)) {
        foundSeasons.push(...values);
      } else {
        foundOccasions.push(...values);
      }
    }
  }

  // Temperature heuristics
  const tempMatch = text.match(/(\d+)/);
  if (tempMatch) {
    const temp = parseInt(tempMatch[1]);
    console.log(`[RAG LOG] Temp matched: ${temp}C`);
    if (temp < 18) foundSeasons.push('Winter');
    else if (temp >= 18 && temp < 24) foundSeasons.push('Autumn', 'Spring');
    else if (temp >= 24) foundSeasons.push('Summer');
  }

  const conditions = [];
  if (foundOccasions.length > 0) conditions.push({ occasion: { $in: [...new Set(foundOccasions)] } });
  if (foundSeasons.length > 0) conditions.push({ season: { $in: [...new Set(foundSeasons)] } });

  if (conditions.length > 0) {
    searchFilter.$or = conditions;
    console.log(`[RAG LOG] Filter built: ${JSON.stringify(conditions)}`);
  } else {
    console.log(`[RAG LOG] No specific context matched from text: "${text}"`);
  }

  try {
    const items = await ClothingItem.find(searchFilter)
      .sort({ createdAt: -1 })
      .limit(limit)
      .lean()
      .exec();

    return items.map(item => ({
      id: String(item._id),
      name: item.name,
      category: item.category,
      color: item.color,
      brand: item.brand || '',
      occasion: item.occasion || '',
      season: item.season || ''
    }));
  } catch (error) {
    console.error('❌ [RAG Retrieval Error]:', error.message);
    return [];
  }
}

/**
 * Tìm các bộ đồ (Outfits) liên quan
 */
async function findRelevantOutfits(userId, queryText, limit = 5) {
  if (!userId || !mongoose.Types.ObjectId.isValid(userId)) return [];

  const text = (queryText || '').toLowerCase();
  const filter = { userId: new mongoose.Types.ObjectId(userId) };

  // Simple keyword matching for outfit names
  const keywords = ['summer', 'hè', 'winter', 'đông', 'rain', 'mưa', 'work', 'làm', 'party', 'tiệc'];
  const matchedKeywords = keywords.filter(kw => text.includes(kw));

  if (matchedKeywords.length > 0) {
    filter.name = { $regex: matchedKeywords.join('|'), $options: 'i' };
  }

  try {
    const outfits = await mongoose.model('Outfit').find(filter)
      .sort({ createdAt: -1 })
      .limit(limit)
      .lean()
      .exec();

    // Để AI hiểu nội dung bộ đồ, ta cần lấy thông tin các món đồ bên trong
    const hydratedOutfits = await Promise.all(outfits.map(async (outfit) => {
      const itemIds = outfit.clothingItems.map(ci => ci.clothingItemId);
      const items = await ClothingItem.find({ _id: { $in: itemIds } }).select('name category color').lean();

      return {
        id: String(outfit._id),
        name: outfit.name,
        items: items.map(i => `${i.name} (${i.category}, ${i.color})`)
      };
    }));

    return hydratedOutfits;
  } catch (error) {
    console.error('❌ [RAG Outfit Retrieval Error]:', error.message);
    return [];
  }
}

module.exports = { findRelevantItems, findRelevantOutfits };
