/**
 * ═══════════════════════════════════════════════════════════════
 * 🧪 AI HYBRID SMOKE TEST (Improved)
 * ═══════════════════════════════════════════════════════════════
 */

require('dotenv').config();
const axios = require('axios');

const BASE_URL = `http://localhost:${process.env.PORT || 3000}/api/ai-stylist`;
// Sử dụng một ID hợp lệ hoặc để hệ thống tự xử lý chuỗi "HungBu" đã được fix
const MOCK_USER_ID = '654321098765432109876543';

async function runTests() {
  console.log('🚀 Bắt đầu kiểm thử Hybrid AI APIs...\n');

  // 1. Test Chat API
  try {
    console.log('--- [1/3] Testing Chat API (RAG + DeepSeek) ---');
    const chatRes = await axios.post(`${BASE_URL}/chat`, {
      userId: MOCK_USER_ID,
      message: 'Tôi muốn phối một bộ đồ đi dự tiệc cưới, phong cách sang trọng.'
    });
    console.log('✅ Chat Success!');
    console.log('AI Message:', chatRes.data.message);
    console.log('-----------------------------------------------\n');
  } catch (err) {
    console.error('❌ Chat API Failed:', err.response?.data || err.message);
  }

  // 2. Test Home Suggestions
  try {
    console.log('--- [2/3] Testing Home Suggestions (DeepSeek) ---');
    const homeRes = await axios.get(`${BASE_URL}/home-suggestions`, {
      params: { userId: MOCK_USER_ID, location: '10.8231,106.6297' }
    });
    console.log('✅ Home Suggestions Success!');
    console.log('-----------------------------------------------\n');
  } catch (err) {
    console.error('❌ Home Suggestions Failed:', err.response?.data || err.message);
  }

  // 3. Test Style Assessment
  try {
    console.log('--- [3/3] Testing Style Assessment (Gemini/DeepSeek Fallback) ---');
    const mockImage = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';
    const assessRes = await axios.post(`${BASE_URL}/style-assess`, {
      userId: MOCK_USER_ID,
      userPhoto: mockImage,
      message: 'Đánh giá bộ đồ này giúp tôi.'
    });
    console.log('✅ Style Assessment Success!');
    console.log('Score:', assessRes.data.result?.score);
    console.log('-----------------------------------------------\n');
  } catch (err) {
    console.error('❌ Style Assessment Failed:', err.response?.data || err.message);
  }

  console.log('🏁 Kết thúc kiểm thử.');
}

runTests();
