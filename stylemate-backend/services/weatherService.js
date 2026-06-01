const axios = require('axios');
const { AppError } = require('../middleware/errorHandler');

const DEFAULT_DAYS = 3;

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

  const weatherUrl = 'https://api.weatherapi.com/v1/forecast.json';
  const params = buildWeatherParams(latNum, lonNum);
  const response = await axios.get(weatherUrl, { params, timeout: 10000 });
  return response.data;
};

module.exports = {
  fetchWeatherForecast
};

