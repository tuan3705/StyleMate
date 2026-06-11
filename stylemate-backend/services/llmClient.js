/**
 * ═══════════════════════════════════════════════════════════════
 * 🧠 HYBRID LLM CLIENT — StyleMate Core (DeepSeek + Gemini)
 * ═══════════════════════════════════════════════════════════════
 */

const { GoogleGenerativeAI } = require('@google/generative-ai');
const axios = require('axios');
const { jsonrepair } = require('jsonrepair');

// ⚙️ Cấu hình API Keys
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || process.env.GGL_API_KEY;
const DEEPSEEK_API_KEY = process.env.DEEPSEEK_API_KEY;

// ⚙️ Khởi tạo SDK
const genAI = GEMINI_API_KEY ? new GoogleGenerativeAI(GEMINI_API_KEY) : null;

// ⚙️ Cấu hình Model
const DEEPSEEK_CONFIG = {
  baseURL: process.env.DEEPSEEK_API_BASE || 'https://api.deepseek.com',
  model: process.env.DEEPSEEK_MODEL || 'deepseek-chat'
};

const GEMINI_CONFIG = {
  model: process.env.GEMINI_MODEL || 'gemini-2.0-flash'
};

const DEFAULT_MOCK_DELAY = 300;
const MAX_RETRIES = Number(process.env.LLM_MAX_RETRIES || 3);

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Kiểm tra cấu trúc Schema Phase 1 bắt buộc
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
function normalizeResponseText(responseText) {
  if (!responseText) return '';
  return String(responseText)
    .replace(/^\uFEFF/, '') // Loại bỏ Byte Order Mark (BOM)
    .replace(/[\u200B-\u200D\uFEFF]/g, '') // Loại bỏ Zero Width Spaces
    .replace(/^```json\s*/i, '')
    .replace(/^```\s*/i, '')
    .replace(/\s*```$/i, '')
    .trim();
}

function extractJsonCandidate(text) {
  if (!text || typeof text !== 'string') return null;
  // Sử dụng Regex để tìm khối { ... } lớn nhất, bỏ qua rác bên ngoài
  const match = text.match(/\{[\s\S]*\}/);
  return match ? match[0] : null;
}

function parseStrictJson(text) {
  // 🔍 LOG RAW RESPONSE FOR DEBUGGING
  console.log('\n--- [AI RAW RESPONSE START] ---');
  console.log(text);
  console.log('--- [AI RAW RESPONSE END] ---\n');

  const cleaned = normalizeResponseText(text);
  if (!cleaned) throw new Error('AI returned an empty response');

  try {
    return JSON.parse(cleaned);
  } catch (e) {
    // Thử trích xuất JSON bằng Regex
    const candidate = extractJsonCandidate(cleaned);
    if (candidate) {
      try {
        return JSON.parse(candidate);
      } catch (e2) {
        // Log chi tiết lỗi vị trí để soi ký tự lạ
        console.error(`❌ [JSON Parse Error]: ${e2.message} at "${candidate.substring(0, 20)}..."`);
        // Thử sửa lỗi JSON bằng jsonrepair
        try {
          return JSON.parse(jsonrepair(candidate));
        } catch (e3) {
          console.error('❌ [JSON Repair Error]: Could not fix the JSON structure.');
          throw new Error('Failed to parse and repair AI JSON response');
        }
      }
    }
    throw e;
  }
}

/**
 * 🧠 Gọi DeepSeek API (REST)
 */
async function callDeepSeek({ messages, temperature = 0.3 }) {
  if (!DEEPSEEK_API_KEY) throw new Error('DEEPSEEK_API_KEY chưa được cấu hình');

  const response = await axios.post(`${DEEPSEEK_CONFIG.baseURL}/chat/completions`, {
    model: DEEPSEEK_CONFIG.model,
    messages,
    temperature,
    response_format: { type: 'json_object' },
    max_tokens: 4096
  }, {
    headers: {
      'Authorization': `Bearer ${DEEPSEEK_API_KEY}`,
      'Content-Type': 'application/json'
    },
    timeout: 60000
  });

  const content = response.data.choices?.[0]?.message?.content;
  if (!content) throw new Error('DeepSeek returned an empty completion choice');
  return content;
}

/**
 * 👁️ Gọi Gemini API (SDK)
 */
async function callGemini({ prompt, mediaParts = [], temperature = 0.3 }) {
  if (!genAI) throw new Error('GEMINI_API_KEY chưa được cấu hình');

  const model = genAI.getGenerativeModel({ model: GEMINI_CONFIG.model });
  const userContent = [{ text: prompt }, ...mediaParts];

  const result = await model.generateContent({
    contents: [{ role: 'user', parts: userContent }],
    generationConfig: {
      temperature,
      maxOutputTokens: 4096,
      responseMimeType: 'application/json'
    }
  });

  return result.response.text();
}

/**
 * 🚀 Cỗ máy điều phối Hybrid thông minh
 */
async function generateStructuredResponse({
  message,
  context = {},
  options = {},
  systemPrompt = '',
  validator = null,
  mockResponse = {}
}) {
  if (process.env.MOCK_LLM === 'true') {
    await sleep(DEFAULT_MOCK_DELAY);
    return mockResponse;
  }

  const mediaParts = Array.isArray(options.mediaParts) ? options.mediaParts : [];
  let provider = options.provider || (mediaParts.length > 0 ? 'gemini' : 'deepseek');

  let attempt = 0;
  while (attempt < MAX_RETRIES) {
    attempt++;
    try {
      let rawResponse = '';

      // Build a clean context for the AI
      const contextSummary = context.closet?.items?.length > 0
        ? `Relevant Clothes available:\n${JSON.stringify(context.closet.items)}\n`
        : 'No relevant clothes found in the search context.\n';

      const promptContext = `${contextSummary}User Input: ${message}`;

      // Hyper-strict system prompt
      const finalSystemPrompt = systemPrompt +
        "\n\nOUTPUT INSTRUCTION: You are a JSON engine. You MUST output a valid JSON object ONLY. " +
        "NO markdown code blocks (NO ```json). NO explanations. NO preamble. " +
        "Start your response directly with '{' and end with '}'.";

      if (provider === 'gemini') {
        try {
          rawResponse = await callGemini({
            prompt: `${finalSystemPrompt}\n\n${promptContext}`,
            mediaParts,
            temperature: options.temperature || 0.3
          });
        } catch (geminiErr) {
          if (geminiErr.message.includes('429') || geminiErr.message.includes('quota')) {
            console.warn('⚠️ Gemini Quota Exceeded. Falling back to DeepSeek (text-only mode)...');
            provider = 'deepseek';
            throw geminiErr;
          }
          throw geminiErr;
        }
      } else {
        const messages = [
          { role: 'system', content: finalSystemPrompt },
          { role: 'user', content: promptContext }
        ];
        rawResponse = await callDeepSeek({ messages, temperature: options.temperature || 0.3 });
      }

      const parsedData = parseStrictJson(rawResponse);

      if (typeof validator === 'function' && !validator(parsedData)) {
        console.error('❌ [Validation Failed] Data:', JSON.stringify(parsedData));
        throw new Error('AI Response validation failed');
      }

      return parsedData;

    } catch (err) {
      console.warn(`❌ [LLM Hybrid Retry ${attempt}/${MAX_RETRIES}]: ${err.message}`);
      if (attempt >= MAX_RETRIES) {
        if (options.fallbackResponse) return options.fallbackResponse;
        throw err;
      }
      await sleep(Math.pow(2, attempt) * 1000);
    }
  }
}

/**
 * 🎨 CHAT API chính — Phối đồ và tư vấn
 */
async function generateChatResponse({ userId, message, context = {}, options = {} }) {
  const systemPrompt = `Bạn là một chuyên gia tư vấn thời trang cá nhân (AI Stylist) thân thiện và tinh tế. Nhiệm vụ của bạn là kết hợp quần áo từ tủ đồ của người dùng, phù hợp với thời tiết hiện tại và dịp sử dụng.

Quy tắc trả lời:
Giọng điệu: Tự nhiên, ấm áp, xưng 'tôi' và gọi người dùng là 'bạn'.

Cấu trúc:
1. Bắt đầu bằng một câu chào thân thiện (Ví dụ: 'Chào bạn, đây là gợi ý...').
2. Giải thích ngắn gọn lý do chọn trang phục (phù hợp thời tiết, thoải mái, v.v.).
3. Nếu thiếu một món đồ quan trọng (ví dụ: giày), hãy ghi chú nhẹ nhàng (Ví dụ: 'Chúng tôi không tìm thấy giày thể thao nào trong tủ đồ của bạn').
4. Kết luận BẮT BUỘC: Luôn luôn kết thúc câu trả lời bằng một câu hỏi mở để khuyến khích người dùng tương tác tiếp, cụ thể là: 'Có vấn đề gì không?' hoặc 'Bạn có muốn thay đổi món đồ nào không?'.

Độ dài: Ngắn gọn, không viết dài dòng lê thê.

Bạn MUST trả về một đối tượng JSON duy nhất:
{
  "message": "Friendly response following the rules above",
  "suggested_outfits": [
    {
      "id": "unique_outfit_id",
      "style_title": "Attractive Style Title",
      "description": "Detailed description of the style and why it works (3-4 sentences)",
      "date": "09 thg 6",
      "location": "Hà Nội",
      "temp": "26 / 22°C",
      "sections": [
        {
          "label": "Category Label",
          "item_description": "Specific item type",
          "matching_item_ids": ["item_id_1", "item_id_2"]
        }
      ]
    }
  ],
  "followups": ["Followup 1", "Followup 2"]
}`;

  return generateStructuredResponse({
    message,
    context,
    options,
    systemPrompt,
    validator: validatePhase1Schema,
    mockResponse: {
      message: `(Mock) Gợi ý cho bạn!`,
      suggested_outfits: [{ id: "mock_1", reason: "Phù hợp với bối cảnh." }]
    }
  });
}

module.exports = {
  generateChatResponse,
  generateStructuredResponse,
  validatePhase1Schema
};
