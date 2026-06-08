const llmClient = require('./llmClient');
const contextService = require('./contextService');

/**
 * Service for Home Suggestions
 */

// Cache mock
const suggestionsCache = new Map();

async function getHomeSuggestions(userId, location) {
  const cacheKey = `${userId}_${location}`;
  if (suggestionsCache.has(cacheKey)) {
    const cached = suggestionsCache.get(cacheKey);
    if (Date.now() - cached.timestamp < 15 * 60 * 1000) { // 15 mins
      return cached.data;
    }
  }

  // Compose context
  const context = await contextService.buildContext({ userId, lat: location?.split(',')[0], lon: location?.split(',')[1] });

  // Call LLM for generation
  const response = await llmClient.generateStructuredResponse({
    message: 'Generate home suggestions for today based on context.',
    context,
    options: {
      schemaType: 'home_suggestions' // Pseudo schema
    }
  });

  // Parse result (mock structure or actual schema map)
  const result = {
    suggestions: response.suggestions || [
      {
        id: `s_${Date.now()}`,
        label: "Năng động cho ngày mới",
        items: [],
        thumbnailUrl: null,
        reason: "Gợi ý mặc định",
        confidence: 0.8
      }
    ],
    generatedAt: new Date().toISOString()
  };

  suggestionsCache.set(cacheKey, { timestamp: Date.now(), data: result });
  return result;
}

async function refreshHomeSuggestions(userId, location) {
  const cacheKey = `${userId}_${location}`;
  suggestionsCache.delete(cacheKey);
  return getHomeSuggestions(userId, location);
}

module.exports = {
  getHomeSuggestions,
  refreshHomeSuggestions
};