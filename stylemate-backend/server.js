/**
 * 🚀 Stylemate Backend Server
 * 
 * Ứng dụng Backend Node.js + Express + MongoDB (Mongoose)
 * cho ứng dụng Android Stylemate.
 * 
 * 📌 Cách chạy:
 *   1. Cài dependencies: npm install
 *   2. Chạy server:      npm start
 *   3. Dev mode:         npm run dev (cần nodemon)
 * 
 * 📌 Biến môi trường (.env):
 *   - PORT: Cổng chạy server (mặc định 3000)
 *   - MONGODB_URI: URI kết nối MongoDB
 *   - WEATHER_API_KEY: API Key WeatherAPI.com
 * 
 * 📌 Endpoints:
 *   - GET  /api/health                    → Kiểm tra server
 *   - CRUD /api/clothes                   → Quản lý quần áo
 *   - CRUD /api/outfits                   → Quản lý phối đồ
 *   - CRUD /api/calendar                  → Quản lý lịch
 *   - GET  /api/weather/forecast          → Proxy thời tiết
 *   - POST /api/user/fcm-token            → Lưu FCM Token
 */

// ═══════════════════════════════════════════════════════════════
// 📦 Khởi tạo
// ═══════════════════════════════════════════════════════════════
const path = require('path');
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const dotenv = require('dotenv');

// Load biến môi trường từ file .env
dotenv.config();

// Khởi tạo Express app
const app = express();

// Kết nối database
const connectDatabase = require('./config/database');

// Routes
const mountRoutes = require('./routes/index');

// Middleware xử lý lỗi
const {
  notFoundHandler,
  globalErrorHandler
} = require('./middleware/errorHandler');

// ═══════════════════════════════════════════════════════════════
// 🛡️ Middleware (thứ tự quan trọng)
// ═══════════════════════════════════════════════════════════════

// 1. CORS — Cho phép Client Android (hoặc web) gọi API từ domain khác
app.use(cors());

// 2. Request logging — In log mỗi request (dev mode)
if (process.env.NODE_ENV !== 'production') {
  app.use(morgan('dev'));
} else {
  app.use(morgan('combined'));
}

// 3. Serve static files — Cho phép Android truy cập ảnh đã upload
//    Android sẽ load ảnh qua URL: http://YOUR_IP:3000/uploads/abc.jpg
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// 4. Parse JSON body — Giới hạn kích thước 10MB (cho ảnh base64 sau này)
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// ═══════════════════════════════════════════════════════════════
// 🏠 Route cơ bản
// ═══════════════════════════════════════════════════════════════

app.get('/', (req, res) => {
  res.status(200).json({
    success: true,
    message: '🎯 Stylemate Backend API',
    version: '1.0.0',
    docs: '/api/health'
  });
});

// ═══════════════════════════════════════════════════════════════
// 🔗 Mount tất cả API Routes
// ═══════════════════════════════════════════════════════════════

mountRoutes(app);

// ═══════════════════════════════════════════════════════════════
// 🚨 Middleware xử lý lỗi (phải đặt SAU routes)
// ═══════════════════════════════════════════════════════════════

// 404 handler — route không tồn tại
app.use(notFoundHandler);

// Global error handler — bắt tất cả lỗi
app.use(globalErrorHandler);

// ═══════════════════════════════════════════════════════════════
// 🚀 Khởi động server
// ═══════════════════════════════════════════════════════════════

const PORT = process.env.PORT || 3000;

const startServer = async () => {
  try {
    // Kết nối MongoDB trước
    await connectDatabase();

    // Sau đó mới start HTTP server
    app.listen(PORT, '0.0.0.0', () => {
      console.log('╔═══════════════════════════════════════════════╗');
      console.log('║         🚀 STYLEMATE BACKEND SERVER          ║');
      console.log('╠═══════════════════════════════════════════════╣');
      console.log(`║  Port:       ${PORT}`);
      console.log(`║  Env:        ${process.env.NODE_ENV || 'development'}`);
      console.log(`║  Network:    http://0.0.0.0:${PORT}`);
      console.log(`║  Health:     http://localhost:${PORT}/api/health`);
      console.log(`║  Clothes:    http://localhost:${PORT}/api/clothes`);
      console.log(`║  Outfits:    http://localhost:${PORT}/api/outfits`);
      console.log(`║  Calendar:   http://localhost:${PORT}/api/calendar`);
      console.log(`║  Weather:    http://localhost:${PORT}/api/weather/forecast`);
      console.log(`║  User/FCM:   http://localhost:${PORT}/api/user/fcm-token`);
      console.log('╚═══════════════════════════════════════════════╝');
    });
  } catch (error) {
    console.error('❌ Lỗi khởi động server:', error.message);
    process.exit(1);
  }
};

// 🔥 Go!
startServer();
