/**
 * services/llmClient.js
 *
 * Tối ưu hóa cho StyleMate - Sử dụng duy nhất SDK chính thức của Google Gemini.
 * Hỗ trợ: Mock mode, Tự động ép kiểu JSON Schema (Structured Output), Retry "Bọc thép".
 */

const { GoogleGenerativeAI } = require('@google/generative-ai');

// Khởi tạo SDK
const apiKey = process.env.GEMINI_API_KEY || process.env.GGL_API_KEY;
const genAI = apiKey ? new GoogleGenerativeAI(apiKey) : null;

// Cấu hình Model mặc định (Sử dụng dòng model hiện hành)
const DEFAULT_MODEL = 'gemini-2.5-flash';
const DEFAULT_MOCK_DELAY = 300;
const MAX_RETRIES = Number(process.env.LLM_MAX_RETRIES || 3);

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

/**
 * Hàm xuất chính xử lý logic chat và gợi ý đồ
 */
async function generateChatResponse({ userId, message, context = {}, options = {} }) {
  // 1. Chế độ MOCK dành cho Dev Local
  if (process.env.MOCK_LLM === 'true') {
    await sleep(DEFAULT_MOCK_DELAY);
    return {
      message: `(Mock) Gợi ý phối đồ cho: "${message}"`,
      suggested_outfits: [
        {
          id: `mock_outfit_1`,
          reason: 'Phù hợp với thời tiết dựa trên dữ liệu giả lập.'
        }
      ]
    };
  }

  // Kiểm tra cấu hình khóa API
  if (!genAI) {
    throw Object.assign(new Error('GEMINI_API_KEY chưa được cấu hình trong file .env'), { statusCode: 500 });
  }

  // Bọc toàn bộ luồng xử lý trong Try...Catch tổng
  try {
    // 2. Chuẩn bị Prompt & Ngữ cảnh (RAG)
    const systemPrompt = `You are StyleMate, a professional fashion stylist AI. 
You MUST reply with a single JSON object matching the required schema. Do not output any Markdown block code, markdown format, or extra text.
CRITICAL INSTRUCTION: Keep the 'message' and 'reason' fields extremely concise. Maximum 2 sentences per field. Do NOT generate long explanations.`;

    const promptParts = [systemPrompt];
    if (context?.summaryText) {
      promptParts.push(`Context Summary:\n${context.summaryText}`);
    }
    if (context && Object.keys(context).length > 0) {
      promptParts.push(`Full Context Data:\n${JSON.stringify(context)}`);
    }
    promptParts.push(`User current request:\n${message}`);
    const finalPrompt = promptParts.join('\n\n');

    // 3. Cấu hình generationConfig với Schema Phase 1
    // Việc định nghĩa responseSchema ở đây giúp loại bỏ hoàn toàn các hàm normalize phức tạp.
    const generationConfig = {
      temperature: 0.3,
      maxOutputTokens: 2048,
      responseMimeType: 'application/json',
      responseSchema: {
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
      }
    };

    // 4. VÒNG LẶP RETRY "BỌC THÉP" (Xử lý cả lỗi mạng lẫn lỗi JSON rách/cắt cụt)
    let parsedData = null;
    let attempt = 0;
    const maxAttempts = MAX_RETRIES || 3;

    while (attempt < maxAttempts) {
      try {
        attempt++;
        let modelName = process.env.GEMINI_MODEL || DEFAULT_MODEL;
        // Chuẩn hóa tên model (xóa tiền tố "models/" nếu bị dư)
        modelName = modelName.replace(/^models\//, '');
        const model = genAI.getGenerativeModel({ model: modelName });

        // Gọi API trực tiếp
        const result = await model.generateContent({
          contents: [{ role: 'user', parts: [{ text: finalPrompt }] }],
          generationConfig
        });

        // Kiểm tra lý do dừng của AI
        const candidate = result.response.candidates?.[0];
        let isMaxTokensError = false;
        if (candidate && candidate.finishReason !== 'STOP') {
          console.warn(`[LLM Cảnh báo] API ngắt giữa chừng vì lý do: ${candidate.finishReason}`);
          if (candidate.finishReason === 'MAX_TOKENS') {
            isMaxTokensError = true;
          }
        }

        let responseText = result.response.text();

        // Làm sạch các ký tự rác hoặc markdown nếu có
        responseText = responseText.replace(/^```json\s*/i, '').replace(/\s*```$/i, '').trim();
        responseText = responseText.replace(/[\u0000-\u001F]+/g, (match) => {
          if (match === '\n') return '\\n';
          if (match === '\r') return '\\r';
          if (match === '\t') return '\\t';
          return '';
        });

        if (isMaxTokensError) {
          // Nếu bị ngắt giữa chừng, quăng lỗi ngay để chạy lại vòng lặp
          throw new Error("AI trả về kết quả quá dài (MAX_TOKENS) khiến JSON bị rách.");
        }

        // Ép kiểu JSON - Nếu bị đứt đoạn, sẽ tự văng lỗi và nhảy vào catch để gọi lại AI
        parsedData = JSON.parse(responseText);
        
        // Bứt khỏi vòng lặp nếu parse thành công
        break; 

      } catch (err) {
        console.warn(`❌ [LLM Thất bại Lần ${attempt}]: ${err.message}`);
        
        if (attempt >= maxAttempts) {
          throw Object.assign(new Error(`LLM liên tục trả về JSON hỏng hoặc lỗi mạng sau ${maxAttempts} lần thử.`), { statusCode: 502 });
        }
        
        // Nghỉ một nhịp trước khi bắt LLM gọi lại
        const delay = Math.pow(2, attempt) * 1000 + Math.floor(Math.random() * 500);
        console.log(`⏳ Đang thử gọi lại AI sau ${delay}ms...`);
        await sleep(delay);
      }
    }

    // 5. Validate Schema Phase 1 (Double Check)
    const expectedSchema = options?.expectedSchema || 'phase1';
    if (expectedSchema === 'phase1' && !validatePhase1Schema(parsedData)) {
      throw Object.assign(new Error('Dữ liệu JSON từ LLM không khớp với Schema cấu trúc Phase 1'), { statusCode: 502 });
    }

    return parsedData;

  } catch (error) {
    // Bắt lỗi tổng và chuẩn hóa mã lỗi HTTP trước khi trả về Controller
    console.error('❌ [LLM Client Error]:', {
      message: error.message,
      status: error.status || error.statusCode || 500,
    });
    
    throw Object.assign(new Error(`Xử lý AI Stylist thất bại: ${error.message}`), { 
      statusCode: error.statusCode || 502 
    });
  }
}

module.exports = {
  generateChatResponse
};