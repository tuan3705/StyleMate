const axios = require('axios');
const { AppError } = require('../middleware/errorHandler');

const DEFAULT_DAYS = 3;

// ⚡ Bộ nhớ đệm cho WeatherAPI: tránh gọi API liên tục cùng 1 tọa độ
const weatherCache = new Map();
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 phút

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
    throw new AppError('Thiếu WEATHER_API_KEY trong môi trường', 500);
  }

  const latNum = Number(lat);
  const lonNum = Number(lon);
  if (Number.isNaN(latNum) || Number.isNaN(lonNum)) {
    throw new AppError('lat/lon phải là số hợp lệ', 400);
  }

  // ⚡ Kiểm tra cache trước
  const cacheKey = `${latNum.toFixed(4)},${lonNum.toFixed(4)}`;
  const cached = weatherCache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
    console.log(`⚡ Dùng cache WeatherAPI cho ${cacheKey} (còn ${Math.round((CACHE_TTL_MS - (Date.now() - cached.timestamp)) / 1000)}s)`);
    return cached.data;
  }

  const weatherUrl = 'https://api.weatherapi.com/v1/forecast.json';
  const params = buildWeatherParams(latNum, lonNum);

  // ⚡ Tăng timeout lên 15s + thêm retry 1 lần nếu timeout
  const MAX_RETRIES = 1;
  let lastError = null;

  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    try {
      const response = await axios.get(weatherUrl, {
        params,
        timeout: 15000 // Tăng từ 10s lên 15s
      });

      // Lưu cache
      weatherCache.set(cacheKey, {
        data: response.data,
        timestamp: Date.now()
      });

      // Dọn cache cũ (nếu có nhiều hơn 20 entry)
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
        console.log(`⚠️ WeatherAPI timeout lần ${attempt + 1}, thử lại...`);
        // Chờ 1s trước khi retry
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