/**
 * 🔗 Routes Index
 * 
 * Tổng hợp tất cả các route và mount vào app.
 * Giúp server.js sạch sẽ, chỉ gọi 1 hàm mountRoutes.
 */
const clothesRoutes = require('./clothesRoutes');
const outfitsRoutes = require('./outfitsRoutes');
const calendarRoutes = require('./calendarRoutes');
const weatherRoutes = require('./weatherRoutes');
const userRoutes = require('./userRoutes');
const authRoutes = require('./authRoutes');
const { requireAuth } = require('../middleware/authMiddleware');

/**
 * Mount tất cả routes vào Express app.
 * 
 * @param {import('express').Application} app - Express app instance
 */
const mountRoutes = (app) => {
  // 👕 Quản lý tủ đồ
  app.use('/api/clothes', requireAuth, clothesRoutes);

  // 👔 Quản lý phối đồ
  app.use('/api/outfits', requireAuth, outfitsRoutes);

  // 📅 Quản lý lịch
  app.use('/api/calendar', requireAuth, calendarRoutes);

  // 🌤️ Proxy thời tiết
  app.use('/api/weather', requireAuth, weatherRoutes);

  // 🔐 Auth (login/logout/refresh)
  app.use('/api/auth', authRoutes);

  // 📱 Quản lý FCM Token
  app.use('/api/user', requireAuth, userRoutes);

  // 🏠 Route kiểm tra health
  app.get('/api/health', (req, res) => {
    res.status(200).json({
      success: true,
      message: 'Stylemate Backend đang hoạt động!',
      timestamp: Date.now(),
      uptime: process.uptime()
    });
  });
};

module.exports = mountRoutes;
