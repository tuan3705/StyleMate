const UserDevice = require('../models/UserDevice');
const { fetchWeatherNotificationPayload } = require('../services/weatherNotificationService');
const { sendMulticast } = require('../services/fcmService');

const DEFAULT_LAT = Number(process.env.WEATHER_DEFAULT_LAT || 21.0285);
const DEFAULT_LON = Number(process.env.WEATHER_DEFAULT_LON || 105.8542);

const chunkArray = (array, size) => {
  const chunks = [];
  for (let i = 0; i < array.length; i += size) {
    chunks.push(array.slice(i, i + size));
  }
  return chunks;
};

const collectDevices = async (userId) => {
  const query = userId ? { userId } : {};
  return UserDevice.find(query).select('fcmToken latitude longitude -_id');
};

const roundCoord = (value) => {
  return Math.round(value * 100) / 100;
};

const buildLocationKey = (lat, lon) => `${lat.toFixed(2)},${lon.toFixed(2)}`;

const runWeatherNotificationJob = async ({ userId } = {}) => {
  const devices = await collectDevices(userId);
  const tokens = devices.map((device) => device.fcmToken).filter(Boolean);
  if (!tokens.length) {
    return {
      successCount: 0,
      failureCount: 0,
      tokens: 0,
      reason: 'NO_TOKENS'
    };
  }

  const locationGroups = new Map();
  devices.forEach((device) => {
    if (!device.fcmToken) return;
    const lat = device.latitude ?? DEFAULT_LAT;
    const lon = device.longitude ?? DEFAULT_LON;
    const roundedLat = roundCoord(lat);
    const roundedLon = roundCoord(lon);
    const key = buildLocationKey(roundedLat, roundedLon);
    if (!locationGroups.has(key)) {
      locationGroups.set(key, { lat: roundedLat, lon: roundedLon, tokens: [] });
    }
    locationGroups.get(key).tokens.push(device.fcmToken);
  });

  let successCount = 0;
  let failureCount = 0;
  let lastWeatherCode = null;

  for (const group of locationGroups.values()) {
    const { payload, condition } = await fetchWeatherNotificationPayload(
      group.lat,
      group.lon
    );
    lastWeatherCode = condition.code;

    const batches = chunkArray(group.tokens, 500);
    for (const batch of batches) {
      const response = await sendMulticast(batch, payload);
      successCount += response.successCount || 0;
      failureCount += response.failureCount || 0;
    }
  }

  return {
    successCount,
    failureCount,
    tokens: tokens.length,
    weatherCode: lastWeatherCode
  };
};

module.exports = {
  runWeatherNotificationJob
};
