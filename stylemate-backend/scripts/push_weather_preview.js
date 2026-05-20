const { buildWeatherPayload, pickWeatherCondition } = require('../services/weatherNotificationService');

const mockSnapshot = {
  condition: { text: 'Heavy rain' },
  tempC: 24,
  windKph: 12,
  uv: 5
};

const condition = pickWeatherCondition(mockSnapshot);
const payload = buildWeatherPayload(condition, mockSnapshot);

console.log('Preview condition:', condition);
console.log('Preview payload:', payload);

