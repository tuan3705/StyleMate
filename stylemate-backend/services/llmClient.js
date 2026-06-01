/**
 * services/llmClient.js
 *
 * Tối ưu hóa cho StyleMate - Sử dụng duy nhất SDK chính thức của Google Gemini.
 * Hỗ trợ: Mock mode, Tự động ép kiểu JSON Schema (Structured Output), Retry "Bọc thép".
 */

const { GoogleGenerativeAI } = require('@google/generative-ai');
const { jsonrepair } = require('jsonrepair');

// Khởi tạo SDK
const apiKey = process.env.GEMINI_API_KEY || process.env.GGL_API_KEY;
const genAI = apiKey ? new GoogleGenerativeAI(apiKey) : null;

// Cấu hình Model mặc định (Sử dụng dòng model hiện hành)
const DEFAULT_MODEL = 'gemini-2.5-flash';
const FALLBACK_MODELS = [DEFAULT_MODEL, 'gemini-2.0-flash', 'gemini-1.5-flash'];
const DEFAULT_MOCK_DELAY = 300;
const MAX_RETRIES = Number(process.env.LLM_MAX_RETRIES || 3);
const GEMINI_API_BASE = 'https://generativelanguage.googleapis.com/v1beta';

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Kiểm tra cấu trúc Schema Phase 1 bắt buộc cho StyleMate
 */
function validatePhase1Schema(obj) {
  if (!obj || typeof obj !== 'object') return false;
  if (typeof obj.message !== 'string') return false;
  if (!Array.isArray(obj.suggested_outfits)) return false;
  return true;
}

function normalizeResponseText(responseText) {
  return String(responseText || '')
    .replace(/^```json\s*/i, '')
    .replace(/^```\s*/i, '')
    .replace(/\s*```$/i, '')
    .trim()
    .replace(/[\u0000-\u001F]+/g, (match) => {
      if (match === '\n') return '\\n';
      if (match === '\r') return '\\r';
      if (match === '\t') return '\\t';
      return '';
    });
}

function sanitizeResponseSchema(schema) {
  if (!schema || typeof schema !== 'object') {
    return schema;
  }

  if (Array.isArray(schema)) {
    return schema.map((item) => sanitizeResponseSchema(item));
  }

  const sanitized = {};
  for (const [key, value] of Object.entries(schema)) {
    if (key === '$schema' || key === 'additionalProperties' || key === 'title' || key === 'description' || key === 'examples') {
      continue;
    }

    if (key === 'properties' && value && typeof value === 'object' && !Array.isArray(value)) {
      sanitized.properties = {};
      for (const [propertyName, propertySchema] of Object.entries(value)) {
        sanitized.properties[propertyName] = sanitizeResponseSchema(propertySchema);
      }
      continue;
    }

    if (key === 'items') {
      sanitized.items = sanitizeResponseSchema(value);
      continue;
    }

    sanitized[key] = sanitizeResponseSchema(value);
  }

  return sanitized;
}

function extractJsonCandidate(text) {
  if (!text || typeof text !== 'string') return null;

  const firstBrace = text.indexOf('{');
  const lastBrace = text.lastIndexOf('}');
  if (firstBrace === -1 || lastBrace === -1 || lastBrace <= firstBrace) {
    return null;
  }

  return text.slice(firstBrace, lastBrace + 1);
}

function extractRelaxedJsonCandidate(text) {
  const candidate = extractJsonCandidate(text);
  if (!candidate) return null;

  try {
    return jsonrepair(candidate);
  } catch (error) {
    return candidate;
  }
}

async function callGeminiRest({ modelName, prompt, temperature = 0.3, maxOutputTokens = 2048, mediaParts = [] }) {
  const apiKey = process.env.GEMINI_API_KEY || process.env.GGL_API_KEY;
  if (!apiKey) {
    throw Object.assign(new Error('GEMINI_API_KEY chưa được cấu hình trong file .env'), { statusCode: 500 });
  }

  const body = {
    contents: [
      {
        role: 'user',
        parts: [
          { text: prompt },
          ...mediaParts
        ]
      }
    ],
    generationConfig: {
      temperature,
      maxOutputTokens,
      responseMimeType: 'application/json'
    }
  };

  const response = await fetch(`${GEMINI_API_BASE}/models/${modelName}:generateContent?key=${apiKey}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw Object.assign(new Error(`Gemini REST error ${response.status}: ${errorText}`), { statusCode: response.status });
  }

  return response.json();
}

async function generateStructuredResponse({ message, context = {}, options = {}, systemPrompt = '', responseSchema = null, validator = null, mockResponse = {} }) {
  if (process.env.MOCK_LLM === 'true') {
    await sleep(DEFAULT_MOCK_DELAY);
    return mockResponse;
  }

  if (!genAI) {
    throw Object.assign(new Error('GEMINI_API_KEY chưa được cấu hình trong file .env'), { statusCode: 500 });
  }

  if (options.provider === 'gemini-rest') {
    const modelName = (options.model || process.env.GEMINI_MODEL || DEFAULT_MODEL).replace(/^models\//, '');
    const promptParts = [];
    if (systemPrompt) promptParts.push(systemPrompt);
    if (context?.summaryText) promptParts.push(`Context Summary:\n${context.summaryText}`);
    if (context && Object.keys(context).length > 0) promptParts.push(`Full Context Data:\n${JSON.stringify(context)}`);
    promptParts.push(`User current request:\n${message}`);
    const finalPrompt = promptParts.join('\n\n');

    const result = await callGeminiRest({
      modelName,
      prompt: finalPrompt,
      temperature: options.temperature ?? 0.3,
      maxOutputTokens: options.maxOutputTokens ?? 2048,
      mediaParts: Array.isArray(options.mediaParts) ? options.mediaParts : []
    });

    const text = result?.candidates?.[0]?.content?.parts?.map((part) => part.text || '').join('') || '';
    const cleaned = normalizeResponseText(text);
    try {
      return JSON.parse(cleaned);
    } catch (parseError) {
      const extracted = extractRelaxedJsonCandidate(cleaned);
      if (!extracted) {
        throw parseError;
      }
      return JSON.parse(extracted);
    }
  }

  try {
    const promptParts = [];
    if (systemPrompt) {
      promptParts.push(systemPrompt);
    }
    if (context?.summaryText) {
      promptParts.push(`Context Summary:\n${context.summaryText}`);
    }
    if (context && Object.keys(context).length > 0) {
      promptParts.push(`Full Context Data:\n${JSON.stringify(context)}`);
    }
    promptParts.push(`User current request:\n${message}`);
    const finalPrompt = promptParts.join('\n\n');

    const generationConfig = {
      temperature: options.temperature ?? 0.3,
      maxOutputTokens: options.maxOutputTokens ?? 2048,
      responseMimeType: 'application/json'
    };

    if (responseSchema) {
      generationConfig.responseSchema = sanitizeResponseSchema(responseSchema);
    }

    let parsedData = null;
    let attempt = 0;
    const maxAttempts = Number(options.maxRetries || MAX_RETRIES || 3);

    while (attempt < maxAttempts) {
      try {
        attempt++;
        const configuredModel = (process.env.GEMINI_MODEL || DEFAULT_MODEL).replace(/^models\//, '');
        const modelName = FALLBACK_MODELS[Math.min(attempt - 1, FALLBACK_MODELS.length - 1)] || configuredModel;
        const model = genAI.getGenerativeModel({ model: modelName });

        const userParts = [{ text: finalPrompt }];
        if (Array.isArray(options.mediaParts)) {
          for (const part of options.mediaParts) {
            if (part && typeof part === 'object') {
              userParts.push(part);
            }
          }
        }

        const result = await model.generateContent({
          contents: [{ role: 'user', parts: userParts }],
          generationConfig
        });

        const candidate = result.response.candidates?.[0];
        let isMaxTokensError = false;
        if (candidate && candidate.finishReason !== 'STOP') {
          console.warn(`[LLM Cảnh báo] API ngắt giữa chừng vì lý do: ${candidate.finishReason}`);
          if (candidate.finishReason === 'MAX_TOKENS') {
            isMaxTokensError = true;
          }
        }

        const responseText = normalizeResponseText(result.response.text());

        if (isMaxTokensError) {
          throw new Error('AI trả về kết quả quá dài (MAX_TOKENS) khiến JSON bị rách.');
        }

        try {
          parsedData = JSON.parse(responseText);
        } catch (parseError) {
          const extractedJson = extractRelaxedJsonCandidate(responseText);
          if (!extractedJson) {
            throw parseError;
          }

          parsedData = JSON.parse(extractedJson);
        }
        break;
      } catch (err) {
        console.warn(`❌ [LLM Thất bại Lần ${attempt}]: ${err.message}`);

        if (attempt >= maxAttempts) {
          if (options.fallbackResponse) {
            console.warn('[LLM Fallback] Using schema-valid fallback response after repeated provider failures');
            return options.fallbackResponse;
          }

          throw Object.assign(new Error(`LLM liên tục trả về JSON hỏng hoặc lỗi mạng sau ${maxAttempts} lần thử.`), { statusCode: 502 });
        }

        const delay = Math.pow(2, attempt) * 1000 + Math.floor(Math.random() * 500);
        console.log(`⏳ Đang thử gọi lại AI sau ${delay}ms...`);
        await sleep(delay);
      }
    }

    if (typeof validator === 'function' && !validator(parsedData)) {
      throw Object.assign(new Error('Dữ liệu JSON từ LLM không khớp với Schema yêu cầu'), { statusCode: 502 });
    }

    return parsedData;
  } catch (error) {
    console.error('❌ [LLM Client Error]:', {
      message: error.message,
      status: error.status || error.statusCode || 500,
    });

    throw Object.assign(new Error(`Xử lý AI Stylist thất bại: ${error.message}`), {
      statusCode: error.statusCode || 502
    });
  }
}

/**
 * Hàm xuất chính xử lý logic chat và gợi ý đồ
 */
async function generateChatResponse({ userId, message, context = {}, options = {} }) {
  const systemPrompt = `You are StyleMate, a professional fashion stylist AI. 
You MUST reply with a single JSON object matching the required schema. Do not output any Markdown block code, markdown format, or extra text.
CRITICAL INSTRUCTION: Keep the 'message' and 'reason' fields extremely concise. Maximum 2 sentences per field. Do NOT generate long explanations.`;

  const schema = {
    type: "object",
    properties: {
      message: { type: "string" },
      suggested_outfits: {
        type: "array",
        items: {
          type: "object",
          properties: {
            id: { type: "string" },
            reason: { type: "string" }
          },
          required: ["id", "reason"]
        }
      }
    },
    required: ["message", "suggested_outfits"]
  };

  return generateStructuredResponse({
    message,
    context,
    options,
    systemPrompt,
    responseSchema: schema,
    validator: validatePhase1Schema,
    mockResponse: {
      message: `(Mock) Gợi ý phối đồ cho: "${message}"`,
      suggested_outfits: [
        {
          id: `mock_outfit_1`,
          reason: 'Phù hợp với thời tiết dựa trên dữ liệu giả lập.'
        }
      ]
    }
  });
}

module.exports = {
  generateChatResponse,
  generateStructuredResponse,
  validatePhase1Schema
};