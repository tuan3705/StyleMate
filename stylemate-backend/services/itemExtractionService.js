const llmClient = require('./llmClient');
const axios = require('axios');
const Ajv = require('ajv');
const path = require('path');

const ajv = new Ajv({ allErrors: true, strict: false });
const schema = require(path.join(__dirname, '..', 'schemas', 'clothingCategorization.schema.json'));
const validate = ajv.compile(schema);

/**
 * Extract clothing metadata using LLM (Gemini) and simple heuristics.
 * imageUrl should be publicly accessible (server /uploads path)
 */
async function extractMetadata({ userId, imageUrl }) {
  // Compose a strict instruction asking for JSON output matching schema
  const instruction = `You are given a clothing item image accessible at this URL: ${imageUrl}. Reply with a single JSON object ONLY (no prose) with the following fields:\n- category: string (e.g., Tops, Bottoms, Dresses, Footwear, Bags, Accessories, Jewelry)\n- subcategory: string (e.g., T-shirt, Jeans, Skirt)\n- colors: array of color names or hex (top 5)\n- season: array (Spring, Summer, Autumn, Winter)\n- occasion: array (Casual, Work, Formal, Sports)\n- brand: string or empty\n- material: string or empty\n- sizeSuggestions: array of strings (e.g., ["S","M"])\n- confidence: object with per-field confidence between 0 and 1\nEnsure the JSON is valid and parsable.`;

  try {
    const parsed = await llmClient.generateChatResponse({ userId, message: instruction, context: { imageUrl }, options: { expectedSchema: 'none' } });

    // parsed is expected to be an object
    if (parsed && typeof parsed === 'object') {
      // Normalize keys to our schema
      const candidate = {
        category: parsed.category || '',
        subcategory: parsed.subcategory || '',
        colors: parsed.colors || parsed.color || [],
        season: parsed.season || [],
        occasion: parsed.occasion || parsed.occasion || [],
        brand: parsed.brand || '',
        material: parsed.material || '',
        sizeSuggestions: parsed.sizeSuggestions || parsed.size_suggestions || [],
        confidence: parsed.confidence || {}
      };

      const ok = validate(candidate);
      if (!ok) {
        console.warn('Clothing categorization validation failed:', validate.errors);
        return null;
      }

      return candidate;
    }

    return null;
  } catch (err) {
    console.warn('itemExtractionService.extractMetadata error:', err.message);
    return null;
  }
}

module.exports = { extractMetadata };
