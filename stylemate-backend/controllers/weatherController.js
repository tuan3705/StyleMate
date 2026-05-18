/**
 * 🌤️ Weather Controller
 * 
 * Đóng vai trò Proxy: Client Android gọi vào Backend,
 * Backend gọi tiếp sang WeatherAPI.com.
 * 
 * Mục đích:
 *   1. Giấu API Key (không expose trên Client).
 *   2. Có thể cache kết quả sau này.
 *   3. Có thể transform/xử lý dữ liệu trước khi trả về.
 */
const axios = require('axios');
const asyncHandler = require('../middleware/asyncHandler');
const { AppError } = require('../middleware/errorHandler');

/**
 * 🌤️ GET /api/weather/forecast?lat={lat}&lon={lon}
 * 
 * Proxy gọi WeatherAPI.com forecast API.
 * Trả nguyên JSON từ WeatherAPI về cho Client.
 * 
 * Query params:
 *   - lat: Number (vĩ độ, bắt buộc)
 *   - lon: Number (kinh độ, bắt buộc)
 * 
 * Response: JSON nguyên gốc từ WeatherAPI.com
 * Error 400: nếu thiếu lat/lon
 * Error 502: nếu không thể gọi WeatherAPI
 */
const getWeatherForecast = asyncHandler(async (req, res, next) => {
  const { lat, lon } = req.query;

  // Validation
  if (!lat || !lon) {
    return next(new AppError('Thiếu tham số lat và/hoặc lon', 400));
  }

  const latNum = Number(lat);
  const lonNum = Number(lon);

  if (isNaN(latNum) || isNaN(lonNum)) {
    return next(new AppError('lat và lon phải là số hợp lệ', 400));
  }

  if (latNum < -90 || latNum > 90) {
    return next(new AppError('lat phải nằm trong [-90, 90]', 400));
  }

  if (lonNum < -180 || lonNum > 180) {
    return next(new AppError('lon phải nằm trong [-180, 180]', 400));
  }

  // Lấy API key từ biến môi trường (bắt buộc phải có trong .env)
  // Không dùng fallback hardcode — nếu thiếu, API sẽ báo lỗi rõ ràng
  const apiKey = process.env.WEATHER_API_KEY;

  // URL gọi sang WeatherAPI.com
  const weatherUrl = `https://api.weatherapi.com/v1/forecast.json`;
  const queryParams = {
    key: apiKey,
    q: `${latNum},${lonNum}`,
    days: 3,
    aqi: 'no',
    alerts: 'no'
  };

  try {
    console.log(`🌤️ Đang gọi WeatherAPI.com cho lat=${latNum}, lon=${lonNum}`);

    const response = await axios.get(weatherUrl, { params: queryParams, timeout: 10000 });

    console.log(`✅ Nhận dữ liệu thời tiết thành công: ${response.data.location?.name}`);

    // Trả nguyên JSON về cho Client
    res.status(200).json(response.data);
  } catch (error) {
    console.error('❌ Lỗi gọi WeatherAPI:', error.message);

    if (error.response) {
      // WeatherAPI trả về lỗi (vd: 403 key sai, 400 query sai)
      const status = error.response.status;
      const weatherError = error.response.data?.error?.message || 'Lỗi từ WeatherAPI';

      if (status === 403) {
        return next(new AppError('API Key WeatherAPI không hợp lệ', 502));
      }

      return res.status(status).json({
        success: false,
        message: `WeatherAPI error: ${weatherError}`,
        weatherApiStatus: status
      });
    } else if (error.code === 'ECONNABORTED') {
      return next(new AppError('WeatherAPI timeout sau 10 giây', 504));
    } else {
      return next(new AppError(`Không thể gọi WeatherAPI: ${error.message}`, 502));
    }
  }
});

module.exports = {
  getWeatherForecast
};
