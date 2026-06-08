/**
 * ═══════════════════════════════════════════════════════════════
 * 🧠 LLM CLIENT SERVICE — DeepSeek Migration
 * ═══════════════════════════════════════════════════════════════
 *
 * Chuyển đổi từ Google Gemini sang DeepSeek (OpenAI Compatible API).
 * Hỗ trợ: Structured Output (JSON), Retry Mechanism, Mock Mode.
 *
 * ───────────────────────────────────────────────────────────────
 */

const axios = require('axios');
const { jsonrepair } = require('jsonrepair');

// ⚙️ Cấu hình API
const DEEPSEEK_API_KEY = process.env.DEEPSEEK_API_KEY;
const DEEPSEEK_BASE_URL = process.env.DEEPSEEK_API_BASE || 'https://api.deepseek.com';
const DEFAULT_MODEL = process.env.DEEPSEEK_MODEL || 'deepseek-chat';

const MAX_RETRIES = Number(process.env.LLM_MAX_RETRIES || 3);
const DEFAULT_MOCK_DELAY = 300;

/**
 * Tiện ích tạm dừng (Sleep)
 */
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Kiểm tra cấu trúc Schema bắt buộc cho StyleMate
 */
function validatePhase1Schema(obj) {
  if (!obj || typeof obj !== 'object') return false;
  if (typeof obj.message !== 'string') return false;
  if (!Array.isArray(obj.suggested_outfits)) return false;
  return true;
}

/**
 * Làm sạch văn bản phản hồi để chuẩn bị parse JSON
 */
function normalizeResponseText(text) {
  return String(text || '')
    .replace(/^```json\s*/i, '')
    .replace(/^```\s*/i, '')
    .replace(/\s*```$/i, '')
    .trim();
}

/**
 * Trích xuất JSON từ chuỗi văn bản hỗn hợp
 */
function extractJsonCandidate(text) {
  if (!text) return null;
  const firstBrace = text.indexOf('{');
  const lastBrace = text.lastIndexOf('}');
  if (firstBrace === -1 || lastBrace === -1 || lastBrace <= firstBrace) return null;

  const candidate = text.slice(firstBrace, lastBrace + 1);
  try {
    return JSON.parse(candidate);
  } catch (e) {
    try {
      return JSON.parse(jsonrepair(candidate));
    } catch (err) {
      return null;
    }
  }
}

/**
 * Gửi yêu cầu đến DeepSeek API
 */
async function callDeepSeek({ messages, temperature = 0.3, responseFormat = { type: 'json_object' } }) {
  if (!DEEPSEEK_API_KEY) {
    throw Object.assign(new Error('DEEPSEEK_API_KEY chưa được cấu hình trong .env'), { statusCode: 500 });
  }

  try {
    const response = await axios.post(`${DEEPSEEK_BASE_URL}/chat/completions`, {
      model: DEFAULT_MODEL,
      messages,
      temperature,
      response_format: responseFormat,
      max_tokens: 4096
    }, {
      headers: {
        'Authorization': `Bearer ${DEEPSEEK_API_KEY}`,
        'Content-Type': 'application/json'
      },
      timeout: 60000 // 60s timeout cho AI phản hồi
    });

    return response.data;
  } catch (error) {
    const status = error.response?.status || 500;
    const errorData = error.response?.data?.error?.message || error.message;
    throw Object.assign(new Error(`DeepSeek API Error (${status}): ${errorData}`), { statusCode: status });
  }
}

/**
 * Hàm lõi tạo phản hồi cấu trúc (JSON) với cơ chế Retry
 */
async function generateStructuredResponse({
  message,
  context = {},
  systemPrompt = '',
  validator = null,
  mockResponse = {},
  options = {}
}) {
  // 1. Chế độ giả lập (Mock Mode)
  if (process.env.MOCK_LLM === 'true') {
    await sleep(DEFAULT_MOCK_DELAY);
    return mockResponse;
  }

  // 2. Chuẩn bị Messages cho DeepSeek
  const messages = [];

  // System Prompt (Rất quan trọng để ép kiểu JSON)
  const finalSystemPrompt = systemPrompt || 'You are a helpful assistant. You MUST always respond with a valid JSON object.';
  messages.push({ role: 'system', content: finalSystemPrompt });

  // Thêm Context (nếu có)
  if (context && Object.keys(context).length > 0) {
    messages.push({
      role: 'user',
      content: `Context for the request:\n${JSON.stringify(context, null, 2)}`
    });
  }

  // User Message chính
  messages.push({ role: 'user', content: message });

  // 3. Thực thi với Retry "Bọc thép"
  let attempt = 0;
  while (attempt < MAX_RETRIES) {
    attempt++;
    try {
      const result = await callDeepSeek({
        messages,
        temperature: options.temperature || 0.3
      });

      const content = result.choices?.[0]?.message?.content;
      if (!content) throw new Error('AI không trả về nội dung phản hồi.');

      // Parse JSON từ phản hồi
      let parsedData = null;
      try {
        parsedData = JSON.parse(normalizeResponseText(content));
      } catch (e) {
        parsedData = extractJsonCandidate(content);
        if (!parsedData) throw new Error('Không thể parse JSON từ kết quả của AI.');
      }

      // Kiểm tra tính hợp lệ của dữ liệu (Validation)
      if (typeof validator === 'function' && !validator(parsedData)) {
        throw new Error('Dữ liệu AI trả về không khớp với cấu trúc yêu cầu (Validation failed).');
      }

      return parsedData;

    } catch (error) {
      console.warn(`⚠️ [DeepSeek Retry ${attempt}/${MAX_RETRIES}]: ${error.message}`);

      if (attempt >= MAX_RETRIES) {
        // Nếu thất bại hoàn toàn, trả về fallback nếu có
        if (options.fallbackResponse) return options.fallbackResponse;
        throw Object.assign(new Error(`Thất bại sau ${MAX_RETRIES} lần thử gọi DeepSeek: ${error.message}`), { statusCode: 502 });
      }

      // Đợi trước khi thử lại (Exponential Backoff)
      const delay = Math.pow(2, attempt) * 1000 + Math.floor(Math.random() * 500);
      await sleep(delay);
    }
  }
}

/**
 * 🎨 CHAT API — Chức năng chính cho AI Stylist Chat
 */
async function generateChatResponse({ userId, message, context = {} }) {
  const systemPrompt = `You are StyleMate, a professional fashion stylist AI. 
You MUST respond with a SINGLE JSON object matching the following schema:
{
  "message": "Your styling advice (max 2 sentences)",
  "suggested_outfits": [
    { "id": "closet_item_id", "reason": "Why this matches (max 2 sentences)" }
  ],
  "followups": ["Short follow-up question 1", "Short follow-up question 2"]
}
DO NOT include any markdown code blocks (like \`\`\`json) or extra text outside the JSON.`;

  return generateStructuredResponse({
    message,
    context,
    systemPrompt,
    validator: validatePhase1Schema,
    mockResponse: {
      message: `(Mock) Đây là gợi ý phối đồ cho bạn!`,
      suggested_outfits: [{ id: "mock_1", reason: "Phù hợp với sở thích của bạn." }],
      followups: ["Bạn có muốn đổi phong cách không?", "Thêm phụ kiện nhé?"]
    }
  });
}

module.exports = {
  generateChatResponse,
  generateStructuredResponse,
  validatePhase1Schema
};
