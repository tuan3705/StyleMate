/**
 * 🚀 Stylemate Backend Server
 * 
 * Node.js + Express + MongoDB (Mongoose) Backend Application
 * for the Stylemate Android application.
 * 
 * 📌 How to run:
 *   1. Install dependencies: npm install
 *   2. Run server:           npm start
 *   3. Dev mode:             npm run dev (requires nodemon)
 * 
 * 📌 Environment variables (.env):
 *   - PORT: Server port (default 3000)
 *   - MONGODB_URI: MongoDB connection URI
 *   - WEATHER_API_KEY: WeatherAPI.com API Key
 * 
 * 📌 Endpoints:
 *   - GET  /api/health                    → Health check
 *   - CRUD /api/clothes                   → Clothing management
 *   - CRUD /api/outfits                   → Outfit management
 *   - CRUD /api/calendar                  → Calendar management
 *   - GET  /api/weather/forecast          → Weather proxy
 *   - POST /api/user/fcm-token            → Save FCM Token
 */

// ═══════════════════════════════════════════════════════════════
// 📦 Initialization
// ═══════════════════════════════════════════════════════════════
const path = require('path');
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const dotenv = require('dotenv');

// Load environment variables from .env file
dotenv.config({ path: path.join(__dirname, '.env'), override: true });

// ═══════════════════════════════════════════════════════════════
// 📁 Auto-create uploads/ directory if not exists
// ═══════════════════════════════════════════════════════════════
const fs = require('fs');
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
  console.log('📁 Created uploads/ directory');
}

// Initialize Express app
const app = express();

// Connect database
const connectDatabase = require('./config/database');

// Routes
const mountRoutes = require('./routes/index');
const { startScheduler } = require('./cron/scheduler');

// Error handling middleware
const {
  notFoundHandler,
  globalErrorHandler
} = require('./middleware/errorHandler');

// ═══════════════════════════════════════════════════════════════
// 🛡️ Middleware (order matters)
// ═══════════════════════════════════════════════════════════════

// 1. CORS — Allow Android (or web) client to call API from different domains
app.use(cors());

// 2. Request logging — Log each request (dev mode)
if (process.env.NODE_ENV !== 'production') {
  app.use(morgan('dev'));
} else {
  app.use(morgan('combined'));
}

// 3. Serve static files — Allow Android to access uploaded images
//    Android loads images via URL: http://YOUR_IP:3000/uploads/abc.jpg
//    Supports files in uploads/, uploads/items/, uploads/tryon/
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// 4. Parse JSON body — Limit size to 10MB (for base64 images later)
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// ═══════════════════════════════════════════════════════════════
// 🏠 Basic Route
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
// 🔗 Mount all API Routes
// ═══════════════════════════════════════════════════════════════

mountRoutes(app);

// Debug: list mounted routes for troubleshooting
console.log('[server] mounted routes (debug):');
if (app && app._router && app._router.stack) {
  app._router.stack.forEach(layer => {
    if (layer.route && layer.route.path) {
      console.log('Route:', Object.keys(layer.route.methods).join(','), layer.route.path);
    } else if (layer.name === 'router' && layer.handle && layer.handle.stack) {
      layer.handle.stack.forEach(l => {
        if (l.route && l.route.path) {
          console.log('  Subroute:', Object.keys(l.route.methods).join(','), l.route.path);
        }
      });
    }
  });
} else {
  console.log('[server] No router found on app yet.');
}

// ═══════════════════════════════════════════════════════════════
// 🚨 Error handling middleware (must be placed AFTER routes)
// ═══════════════════════════════════════════════════════════════

// 404 handler — route not found
app.use(notFoundHandler);

// Global error handler — catch all errors
app.use(globalErrorHandler);

// ═══════════════════════════════════════════════════════════════
// 🚀 Start server
// ═══════════════════════════════════════════════════════════════

const PORT = process.env.PORT || 3000;

const startServer = async () => {
  try {
    // Connect to MongoDB first
    await connectDatabase();

    // Then start HTTP server
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

    startScheduler();
  } catch (error) {
    console.error('❌ Server startup error:', error.message);
    process.exit(1);
  }
};

// 🔥 Go!
startServer();
