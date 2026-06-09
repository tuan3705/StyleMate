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

  // Phân tích bối cảnh (Heuristic mapping)
  const contextMap = {
    'tiệc': ['Formal', 'Party', 'Evening'],
    'party': ['Formal', 'Party', 'Evening'],
    'đi chơi': ['Casual', 'Streetwear'],
    'dạo phố': ['Casual', 'Streetwear'],
    'hẹn hò': ['Casual', 'Elegant'],
    'đi làm': ['Work', 'Business'],
    'office': ['Work', 'Business'],
    'thể thao': ['Sports', 'Active'],
    'mùa hè': ['Summer'],
    'mùa đông': ['Winter'],
    'mùa thu': ['Autumn', 'Fall'],
    'mùa xuân': ['Spring']
  };

  const foundOccasions = [];
  const foundSeasons = [];

  for (const [key, values] of Object.entries(contextMap)) {
    if (text.includes(key)) {
      if (['mùa hè', 'mùa đông', 'mùa thu', 'mùa xuân'].includes(key)) {
        foundSeasons.push(...values);
      } else {
        foundOccasions.push(...values);
      }
    }
  }

  const conditions = [];
  if (foundOccasions.length > 0) conditions.push({ occasion: { $in: foundOccasions } });
  if (foundSeasons.length > 0) conditions.push({ season: { $in: foundSeasons } });

  if (conditions.length > 0) {
    searchFilter.$or = conditions;
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

module.exports = { findRelevantItems };
