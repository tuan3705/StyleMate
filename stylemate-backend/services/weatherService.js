const axios = require('axios');
const { AppError } = require('../middleware/errorHandler');

const DEFAULT_DAYS = 3;

// ⚡ WeatherAPI cache: avoid calling the API repeatedly for the same coordinates
const weatherCache = new Map();
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

const buildWeatherParams = (lat, lon) => {
  return {
    key: process.env.WEATHER_API_KEY,
    q: `${lat},${lon}`,
    days: DEFAULT_DAYS,
    aqi: 'no',
    alerts: 'no'
  };
};

const fetchWeatherForecast = async (lat, lon) => {
  if (!process.env.WEATHER_API_KEY) {
    throw new AppError('Missing WEATHER_API_KEY in environment', 500);
  }

  const latNum = Number(lat);
  const lonNum = Number(lon);
  if (Number.isNaN(latNum) || Number.isNaN(lonNum)) {
    throw new AppError('lat/lon must be valid numbers', 400);
  }

  // ⚡ Check cache first
  const cacheKey = `${latNum.toFixed(4)},${lonNum.toFixed(4)}`;
  const cached = weatherCache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
    console.log(`⚡ Using WeatherAPI cache for ${cacheKey} (${Math.round((CACHE_TTL_MS - (Date.now() - cached.timestamp)) / 1000)}s remaining)`);
    return cached.data;
  }

  const weatherUrl = 'https://api.weatherapi.com/v1/forecast.json';
  const params = buildWeatherParams(latNum, lonNum);

  // ⚡ Increase timeout to 15s + add 1 retry on timeout
  const MAX_RETRIES = 1;
  let lastError = null;

  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    try {
      const response = await axios.get(weatherUrl, {
        params,
        timeout: 15000 // Increased from 10s to 15s
      });

      // Save to cache
      weatherCache.set(cacheKey, {
        data: response.data,
        timestamp: Date.now()
      });

      // Clean old cache (if more than 20 entries)
      if (weatherCache.size > 20) {
        const keys = [...weatherCache.keys()];
        const now = Date.now();
        keys.forEach(key => {
          if (now - weatherCache.get(key).timestamp > CACHE_TTL_MS) {
            weatherCache.delete(key);
          }
        });
      }

      return response.data;
    } catch (error) {
      lastError = error;
      if (error.code === 'ECONNABORTED' && attempt < MAX_RETRIES) {
        console.log(`⚠️ WeatherAPI timeout attempt ${attempt + 1}, retrying...`);
        // Wait 1s before retry
        await new Promise(resolve => setTimeout(resolve, 1000));
        continue;
      }
      throw error;
    }
  }

  throw lastError;
};

module.exports = {
  fetchWeatherForecast
};