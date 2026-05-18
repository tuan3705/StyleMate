                                                                                                                                                                                                                            /**
 * 🌤️ Weather Routes
 * 
 * Định tuyến cho API Proxy thời tiết.
 * 
 * Base path: /api/weather
 */
const express = require('express');
const router = express.Router();
const {
  getWeatherForecast
} = require('../controllers/weatherController');

// GET /api/weather/forecast?lat=...&lon=... — Proxy gọi WeatherAPI.com
router.get('/forecast', getWeatherForecast);

module.exports = router;
