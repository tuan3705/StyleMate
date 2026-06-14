const { fetchWeatherForecast } = require('./weatherService');

const normalizeText = (value = '') => value.toLowerCase();

const pickWeatherCondition = (snapshot) => {
  const conditionText = normalizeText(snapshot.condition?.text || '');
  const tempC = snapshot.tempC ?? snapshot.temp_c ?? 0;
  const windKph = snapshot.windKph ?? snapshot.wind_kph ?? 0;
  const uv = snapshot.uv ?? 0;

  if (conditionText.includes('thunder')) {
    return { code: 'THUNDER', label: 'Thunderstorm', tempC };
  }
  if (conditionText.includes('rain') || conditionText.includes('drizzle')) {
    return { code: 'RAIN', label: 'Rainy', tempC };
  }
  if (tempC <= 18) {
    return { code: 'COLD', label: 'Cold', tempC };
  }
  if (windKph >= 35) {
    return { code: 'WINDY', label: 'Windy', tempC };
  }
  if (uv >= 8 || tempC >= 33) {
    return { code: 'HOT', label: 'Hot & Sunny', tempC };
  }
  return { code: 'NORMAL', label: 'Weather', tempC };
};

const buildWeatherMessage = (condition) => {
  switch (condition.code) {
    case 'THUNDER':
      return {
        title: 'Thunderstorm Alert',
        body: 'Thunderstorms expected today. Bring an umbrella and stay indoors if possible.'
      };
    case 'RAIN':
      return {
        title: 'Rainy Day Ahead',
        body: 'Expect rain today. Don\'t forget your umbrella or raincoat!'
      };
    case 'COLD':
      return {
        title: 'Cold Weather',
        body: 'Temperatures are low. Layer up with a jacket and stay warm.'
      };
    case 'WINDY':
      return {
        title: 'Strong Winds',
        body: 'Strong winds outside. Be careful when traveling.'
      };
    case 'HOT':
      return {
        title: 'Hot & Sunny',
        body: 'It\'s hot out there! Stay hydrated and wear breathable clothing.'
      };
    default:
      return {
        title: 'Today\'s Weather',
        body: 'Have a great day! The weather is looking pleasant.'
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