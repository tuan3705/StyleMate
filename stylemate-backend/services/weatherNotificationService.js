const { fetchWeatherForecast } = require('./weatherService');

const normalizeText = (value = '') => value.toLowerCase();

const pickWeatherCondition = (snapshot) => {
  const conditionText = normalizeText(snapshot.condition?.text || '');
  const tempC = snapshot.tempC ?? snapshot.temp_c ?? 0;
  const windKph = snapshot.windKph ?? snapshot.wind_kph ?? 0;
  const uv = snapshot.uv ?? 0;

  if (conditionText.includes('thunder')) {
    return { code: 'THUNDER', label: 'Giông', tempC };
  }
  if (conditionText.includes('rain') || conditionText.includes('drizzle')) {
    return { code: 'RAIN', label: 'Mưa', tempC };
  }
  if (tempC <= 18) {
    return { code: 'COLD', label: 'Lạnh', tempC };
  }
  if (windKph >= 35) {
    return { code: 'WINDY', label: 'Gió mạnh', tempC };
  }
  if (uv >= 8 || tempC >= 33) {
    return { code: 'HOT', label: 'Nắng gắt', tempC };
  }
  return { code: 'NORMAL', label: 'Thời tiết', tempC };
};

const buildWeatherMessage = (condition) => {
  switch (condition.code) {
    case 'THUNDER':
      return {
        title: 'Cảnh báo giông',
        body: 'Hôm nay có giông. Nhớ mang áo mưa và hạn chế ra ngoài.'
      };
    case 'RAIN':
      return {
        title: 'Mưa rồi',
        body: 'Hôm nay có mưa. Mang theo ô/áo mưa nhé.'
      };
    case 'COLD':
      return {
        title: 'Trời lạnh',
        body: 'Nhiệt độ thấp. Ưu tiên áo khoác và giữ ấm.'
      };
    case 'WINDY':
      return {
        title: 'Gió mạnh',
        body: 'Gió lớn ngoài trời. Cẩn thận khi di chuyển.'
      };
    case 'HOT':
      return {
        title: 'Nắng gắt',
        body: 'Nắng gắt, hãy uống đủ nước và mặc đồ thoáng.'
      };
    default:
      return {
        title: 'Thời tiết hôm nay',
        body: 'Chúc bạn một ngày dễ chịu!'
      };
  }
};

const buildWeatherPayload = (condition, snapshot) => {
  const message = buildWeatherMessage(condition);
  const tempValue = snapshot.tempC ?? snapshot.temp_c ?? 0;

  return {
    notification: {
      title: message.title,
      body: message.body
    },
    data: {
      type: 'weather',
      weatherCode: condition.code,
      temp: `${Math.round(tempValue)}`
    }
  };
};

const fetchWeatherNotificationPayload = async (lat, lon) => {
  const weather = await fetchWeatherForecast(lat, lon);
  const current = weather.current || {};
  const snapshot = {
    condition: current.condition || {},
    tempC: current.temp_c,
    windKph: current.wind_kph,
    uv: current.uv
  };
  const condition = pickWeatherCondition(snapshot);
  return {
    payload: buildWeatherPayload(condition, snapshot),
    condition,
    weather
  };
};

module.exports = {
  fetchWeatherNotificationPayload,
  buildWeatherPayload,
  pickWeatherCondition
};

