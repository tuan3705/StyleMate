const axios = require('axios');
const fs = require('fs');
const path = require('path');
const FormData = require('form-data');

const BASE_URL = 'http://localhost:3000';
let ACCESS_TOKEN = '';
let USER_ID = '';

// Hình ảnh dùng để test (Lấy 1 ảnh từ UI Stylemate)
const TEST_IMAGE_PATH = path.join(__dirname, '../../UI Stylemate/01_home_features.jpg');

async function authenticate() {
  console.log('\n=========================================');
  console.log('🔑 Authenticating (Register/Login)');
  console.log('=========================================');
  try {
    const randomSuffix = Math.floor(Math.random() * 100000);
    const payload = {
      email: `test_ai_${randomSuffix}@example.com`,
      password: 'Password123!',
      fullName: 'AI Tester'
    };
    
    // Register
    console.log('📤 Registering user:', payload.email);
    const res = await axios.post(`${BASE_URL}/api/auth/register`, payload);
    ACCESS_TOKEN = res.data.data.accessToken;
    USER_ID = res.data.data.user.id;
    console.log('✅ Registered and got token:', ACCESS_TOKEN.substring(0, 20) + '...', 'UserID:', USER_ID);
  } catch (error) {
    console.error('❌ Error in auth:', error.response?.data || error.message);
    process.exit(1);
  }
}

function getHeaders() {
  return {
    'Authorization': `Bearer ${ACCESS_TOKEN}`
  };
}

async function testAIChat() {
  console.log('\n=========================================');
  console.log('🧪 Bắt đầu test AI Chat (/api/ai-stylist/chat)');
  console.log('=========================================');
  try {
    const payload = {
      userId: USER_ID,
      message: 'Tôi nên mặc gì cho buổi phỏng vấn xin việc IT vào mùa hè?',
    };
    const response = await axios.post(`${BASE_URL}/api/ai-stylist/chat`, payload, { headers: getHeaders() });
    console.log('✅ Response:', JSON.stringify(response.data, null, 2));
    return response.data.sessionId;
  } catch (error) {
    console.error('❌ Error testing AI Chat:', error.response?.data || error.message);
  }
}

async function testStyleAssess() {
  console.log('\n=========================================');
  console.log('🧪 Bắt đầu test Style Assess (/api/ai-stylist/style-assess)');
  console.log('=========================================');
  try {
    const payload = {
      userId: USER_ID,
      message: 'Đánh giá phong cách hiện tại của tôi',
    };
    const response = await axios.post(`${BASE_URL}/api/ai-stylist/style-assess`, payload, { headers: getHeaders() });
    console.log('✅ Response:', JSON.stringify(response.data, null, 2));
  } catch (error) {
    console.error('❌ Error testing Style Assess:', error.response?.data || error.message);
  }
}

async function testColorAnalyze() {
  console.log('\n=========================================');
  console.log('🧪 Bắt đầu test Color Analyze (/api/ai-stylist/color-analyze)');
  console.log('=========================================');
  try {
    const payload = {
      userId: USER_ID,
    };
    const response = await axios.post(`${BASE_URL}/api/ai-stylist/color-analyze`, payload, { headers: getHeaders() });
    console.log('✅ Response:', JSON.stringify(response.data, null, 2));
  } catch (error) {
    console.error('❌ Error testing Color Analyze:', error.response?.data || error.message);
  }
}

async function testAIFill() {
  console.log('\n=========================================');
  console.log('🧪 Bắt đầu test AI Fill (/api/images/ai-fill)');
  console.log('=========================================');
  try {
    if (!fs.existsSync(TEST_IMAGE_PATH)) {
      console.log('⚠️ Không tìm thấy ảnh test tại:', TEST_IMAGE_PATH);
      return;
    }
    const formData = new FormData();
    formData.append('image', fs.createReadStream(TEST_IMAGE_PATH));

    const response = await axios.post(`${BASE_URL}/api/images/ai-fill`, formData, {
      headers: {
        ...getHeaders(),
        ...formData.getHeaders()
      }
    });
    console.log('✅ Response:', JSON.stringify(response.data, null, 2));
  } catch (error) {
    console.error('❌ Error testing AI Fill:', error.response?.data || error.message);
  }
}

async function testAutoTagging() {
  console.log('\n=========================================');
  console.log('🧪 Bắt đầu test Auto Tagging (/api/images/auto-tagging)');
  console.log('=========================================');
  try {
    if (!fs.existsSync(TEST_IMAGE_PATH)) {
      console.log('⚠️ Không tìm thấy ảnh test tại:', TEST_IMAGE_PATH);
      return;
    }
    const formData = new FormData();
    formData.append('image', fs.createReadStream(TEST_IMAGE_PATH));

    const response = await axios.post(`${BASE_URL}/api/images/auto-tagging`, formData, {
      headers: {
        ...getHeaders(),
        ...formData.getHeaders()
      }
    });
    console.log('✅ Response:', JSON.stringify(response.data, null, 2));
  } catch (error) {
    console.error('❌ Error testing Auto Tagging:', error.response?.data || error.message);
  }
}

async function testVirtualTryOn() {
  console.log('\n=========================================');
  console.log('🧪 Bắt đầu test Virtual Try-On (/api/virtual-tryon)');
  console.log('=========================================');
  try {
    if (!fs.existsSync(TEST_IMAGE_PATH)) {
      console.log('⚠️ Không tìm thấy ảnh test tại:', TEST_IMAGE_PATH);
      return;
    }
    const formData = new FormData();
    formData.append('bodyImage', fs.createReadStream(TEST_IMAGE_PATH));
    formData.append('itemImages', fs.createReadStream(TEST_IMAGE_PATH));

    const response = await axios.post(`${BASE_URL}/api/virtual-tryon`, formData, {
      headers: {
        ...getHeaders(),
        ...formData.getHeaders()
      }
    });
    console.log('✅ Response:', JSON.stringify(response.data, null, 2));
    return response.data.jobId;
  } catch (error) {
    console.error('❌ Error testing Virtual Try-On:', error.response?.data || error.message);
  }
}

async function runAllTests() {
  console.log('🚀 Bắt đầu chuỗi test tất cả AI Endpoints...');
  
  try {
    await axios.get(`${BASE_URL}/api/health`);
    console.log('🟢 Server đang hoạt động!');
  } catch (error) {
    console.error('🔴 Không thể kết nối tới server. Vui lòng đảm bảo backend đang chạy ở', BASE_URL);
    process.exit(1);
  }

  await authenticate();

  await testAIChat();
  await testStyleAssess();
  await testColorAnalyze();
  await testAIFill();
  await testAutoTagging();
  await testVirtualTryOn();
  
  console.log('\n🎉 Hoàn thành chuỗi test!');
}

runAllTests();
