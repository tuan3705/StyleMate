/**
 * services/llmClient.js
 *
 * Hardened LLM client with support for:
 *  - MOCK mode for local development
 *  - Generic REST providers (GEMINI_API_URL + GEMINI_API_KEY)
 *  - Google Generative Language / Vertex style endpoints using service account OAuth or API key
 *
 * Features:
 *  - Retries with exponential backoff on 5xx/429
 *  - Timeout control
 *  - Robust extraction of text from various provider response shapes
 *  - Strict JSON extraction/validation for Phase 1 schema (message + suggested_outfits)
 */
const util = require('util');
const axios = require('axios');
const { GoogleAuth } = require('google-auth-library');
const DEFAULT_MOCK_DELAY = 300;

const GEMINI_API_URL = process.env.GEMINI_API_URL;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-default';

// Google / Vertex generative API settings (optional). Default to Gemini model.
const GGL_MODEL = process.env.GGL_MODEL || process.env.GENERATIVE_MODEL || process.env.VERTEX_MODEL || 'models/gemini-1.5'; // e.g. models/gemini-1.5
const GGL_API_KEY = process.env.GGL_API_KEY || process.env.GENERATIVE_API_KEY;

const MAX_RETRIES = Number(process.env.LLM_MAX_RETRIES || 3);
const TIMEOUT_MS = Number(process.env.LLM_TIMEOUT_MS || 20000);

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function extractJsonFromString(s) {
  if (!s || typeof s !== 'string') return null;

  const firstBrace = s.indexOf('{');
  const firstBracket = s.indexOf('[');
  let start = -1;
  let openChar = null;

  if (firstBrace === -1 && firstBracket === -1) return null;
  if (firstBrace === -1) { start = firstBracket; openChar = '['; }
  else if (firstBracket === -1) { start = firstBrace; openChar = '{'; }
  else { start = Math.min(firstBrace, firstBracket); openChar = start === firstBrace ? '{' : '['; }

  let depth = 0;
  for (let i = start; i < s.length; i++) {
    const ch = s[i];
    if (ch === openChar) depth++;
    else if ((openChar === '{' && ch === '}') || (openChar === '[' && ch === ']')) depth--;

    if (depth === 0) {
      const candidate = s.slice(start, i + 1);
      try {
        return JSON.parse(candidate);
      } catch (e) {
        return null;
      }
    }
  }

  return null;
}

function validatePhase1Schema(obj) {
  if (!obj || typeof obj !== 'object') return false;
  if (typeof obj.message !== 'string') return false;
  if (!Array.isArray(obj.suggested_outfits)) return false;
  return true;
}

async function axiosPostWithRetries(url, body, config = {}, maxAttempts = MAX_RETRIES) {
  let attempt = 0;
  while (attempt < maxAttempts) {
    try {
      const resp = await axios.post(url, body, config);
      return resp;
    } catch (err) {
      attempt++;
      const status = err.response?.status;
      // If client error, don't retry
      if (status && status >= 400 && status < 500 && status !== 429) {
        throw err;
      }

      // Handle 429 specially if Retry-After present
      if (status === 429) {
        const ra = err.response.headers['retry-after'];
        let wait = 1000 * (Number(ra) || Math.pow(2, attempt) * 500 + Math.floor(Math.random() * 200));
        console.warn(`LLM rate-limited (429). Waiting ${wait}ms before retry (${attempt}/${maxAttempts})`);
        await sleep(wait);
        continue;
      }

      // For 5xx or network errors, exponential backoff
      if (attempt < maxAttempts) {
        const backoff = Math.pow(2, attempt) * 500 + Math.floor(Math.random() * 200);
        console.warn(`LLM request failed (attempt ${attempt}/${maxAttempts}). Backing off ${backoff}ms. Error: ${err.message}`);
        await sleep(backoff);
        continue;
      }

      throw err;
    }
  }
  throw new Error('LLM request retries exhausted');
}

/**
 * Generate a chat response from an LLM.
 * Supports mock mode, Google Generative Language (service account or API key), and generic REST endpoints.
 */
/**
 * generateChatResponse
 * @param {{userId?:string,message:string,context?:object,options?:{expectedSchema?:'phase1'|'none'|Function}}} param0
 */
async function generateChatResponse({ userId, message, context = {}, options = {} }) {
  // Mock short-circuit
  const mockMode = process.env.MOCK_LLM === 'true';
  if (mockMode) {
    await sleep(DEFAULT_MOCK_DELAY);
    return {
      message: `(Mock) Gợi ý cho: "${message}"`,
      suggested_outfits: [
        {
          id: `mock_outfit_1`,
          top_id: 'mock_top_1',
          bottom_id: 'mock_bottom_1',
          shoes_id: 'mock_shoes_1',
          image_urls: {
            top: '/uploads/mock_top_1.jpg',
            bottom: '/uploads/mock_bottom_1.jpg',
            shoes: '/uploads/mock_shoes_1.jpg'
          },
          reason: 'Mock suggestion: phù hợp với thời tiết và phong cách bạn chọn.'
        }
      ]
    };
  }

  // Build a strict system prompt and include RAG-style context if available
  const systemPrompt = `You are an assistant that MUST reply with a single JSON object only. Do not include explanation or markdown.`;

  const promptParts = [systemPrompt];
  if (context && typeof context.summaryText === 'string' && context.summaryText.trim().length > 0) {
    promptParts.push(`Context Summary:\n${context.summaryText}`);
  }

  // Also include the full context JSON for reference (providers may ignore if too long)
  try {
    promptParts.push(`Full Context (JSON):\n${JSON.stringify(context, null, 2)}`);
  } catch (e) {
    // fallback to util.inspect if JSON stringify fails
    promptParts.push(`Full Context (INSPECT):\n${util.inspect(context, { depth: 2 })}`);
  }

  promptParts.push(`User message:\n${message}`);
  const prompt = promptParts.join('\n\n');

  const expectedSchema = (options && options.expectedSchema) || 'phase1';

  // Prefer Google Generative Language if configured
  const canUseGoogle = Boolean(GGL_MODEL || process.env.GOOGLE_APPLICATION_CREDENTIALS || GGL_API_KEY);
  if (canUseGoogle) {
    const modelPath = GGL_MODEL?.startsWith('models/') ? GGL_MODEL : `models/${GGL_MODEL || 'text-bison-001'}`;
    let url = `https://generativelanguage.googleapis.com/v1beta2/${modelPath}:generateText`;
    if (GGL_API_KEY) url += `?key=${GGL_API_KEY}`;

    const headers = { 'Content-Type': 'application/json' };
    if (!GGL_API_KEY) {
      // Use service account / ADC to obtain access token
      const auth = new GoogleAuth({ scopes: ['https://www.googleapis.com/auth/cloud-platform'] });
      const client = await auth.getClient();
      const accessToken = await client.getAccessToken();
      const token = accessToken?.token || accessToken;
      if (!token) throw Object.assign(new Error('Failed to acquire Google access token for Generative API'), { statusCode: 500 });
      headers.Authorization = `Bearer ${token}`;
    }

    const body = {
      prompt: { text: prompt },
      temperature: 0.2,
      maxOutputTokens: 800
    };

    try {
      const resp = await axiosPostWithRetries(url, body, { headers, timeout: TIMEOUT_MS }, MAX_RETRIES);
      const data = resp.data;

      const rawText = data?.candidates?.[0]?.content || data?.candidates?.[0]?.output || data?.output || data?.responses?.[0]?.content || JSON.stringify(data);

      // Extract JSON
      const parsed = extractJsonFromString(String(rawText));
      if (!parsed) {
        const err = new Error('LLM returned non-JSON response');
        err.statusCode = 502;
        throw err;
      }

      // Validate according to expectedSchema option
      if (expectedSchema === 'phase1') {
        if (!validatePhase1Schema(parsed)) {
          const err = new Error('LLM JSON does not match required schema (message + suggested_outfits)');
          err.statusCode = 502;
          throw err;
        }
      } else if (typeof expectedSchema === 'function') {
        const ok = expectedSchema(parsed);
        if (!ok) {
          const err = new Error('LLM JSON does not match custom validation function');
          err.statusCode = 502;
          throw err;
        }
      }

      return parsed;
    } catch (err) {
      console.error('LLM (Google) request error:', err.message);
      if (!err.statusCode) err.statusCode = 502;
      throw err;
    }
  }

  // Fallback: Generic GEMINI-like REST provider
  if (GEMINI_API_URL && GEMINI_API_KEY) {
    const url = GEMINI_API_URL;
    const headers = {
      Authorization: `Bearer ${GEMINI_API_KEY}`,
      'Content-Type': 'application/json'
    };
    const body = {
      model: GEMINI_MODEL,
      prompt,
      max_tokens: 800
    };

    try {
      const resp = await axiosPostWithRetries(url, body, { headers, timeout: TIMEOUT_MS }, MAX_RETRIES);
      const data = resp.data;
      const rawText = data.output || data.text || (data?.choices && data.choices[0]?.text) || JSON.stringify(data);

      const parsed = extractJsonFromString(String(rawText));
      if (!parsed) {
        const err = new Error('LLM returned non-JSON response');
        err.statusCode = 502;
        throw err;
      }

      if (!validatePhase1Schema(parsed)) {
        const err = new Error('LLM JSON does not match required schema (message + suggested_outfits)');
        err.statusCode = 502;
        throw err;
      }

      return parsed;
    } catch (err) {
      console.error('LLM (Generic) request error:', err.message);
      if (!err.statusCode) err.statusCode = 502;
      throw err;
    }
  }

  const err = new Error('LLM provider not configured. Set MOCK_LLM=true for local dev, or configure Google or GEMINI provider env vars.');
  err.statusCode = 500;
  throw err;
}

module.exports = {
  generateChatResponse
};
