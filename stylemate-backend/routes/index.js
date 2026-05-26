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
const aiStylistRoutes = require('./aiStylistRoutes');
const virtualTryOnRoutes = require('./virtualTryOnRoutes');
const itemRoutes = require('./itemRoutes');

/**
 * Mount tất cả routes vào Express app.
 * 
 * @param {import('express').Application} app - Express app instance
 */
const mountRoutes = (app) => {
  // 👕 Quản lý tủ đồ
  app.use('/api/clothes', clothesRoutes);

  // 👔 Quản lý phối đồ
  app.use('/api/outfits', outfitsRoutes);

  // 📅 Quản lý lịch
  app.use('/api/calendar', calendarRoutes);

  // 🌤️ Proxy thời tiết
  app.use('/api/weather', weatherRoutes);

  // 📱 Quản lý FCM Token
  app.use('/api/user', userRoutes);

  // 🤖 AI Stylist endpoints (Phase 1)
  app.use('/api/ai-stylist', aiStylistRoutes);
  // Virtual Try-on async endpoints
  app.use('/api/ai-stylist/virtual-tryon', virtualTryOnRoutes);
  // Items (upload + metadata extraction)
  app.use('/api/items', itemRoutes);

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
