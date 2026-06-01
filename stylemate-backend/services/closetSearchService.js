/**
 * services/closetSearchService.js
 *
 * Simple helper to search a user's closet for matching items.
 * Currently supports category/colour/season filters and returns a lightweight
 * representation suitable for attaching to LLM responses.
 */
const ClothingItem = require('../models/ClothingItem');

async function searchCloset({ userId, categories = [], colors = [], season = null, limit = 6 } = {}) {
  try {
    const q = {};

    if (Array.isArray(categories) && categories.length > 0) {
      q.category = { $in: categories };
    }

    if (Array.isArray(colors) && colors.length > 0) {
      q.color = { $in: colors };
    }

    if (season) {
      q.season = season;
    }

    const items = await ClothingItem.find(q).sort({ createdAt: -1 }).limit(limit).lean().exec();

    return items.map(it => ({
      source: 'closet',
      item_id: it._id,
      name: it.name || '',
      image: it.imageNoBg || it.imageOriginal || '',
      category: it.category || '',
      match_score: 0.9
    }));

  } catch (err) {
    console.warn('closetSearchService.searchCloset error:', err.message);
    return [];
  }
}

module.exports = {
  searchCloset
};
