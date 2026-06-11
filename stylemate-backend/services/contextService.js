const axios = require('axios');
const ClothingItem = require('../models/ClothingItem');
const CalendarEvent = require('../models/CalendarEvent');
const User = require('../models/User');

const WEATHER_API_URL = 'https://api.weatherapi.com/v1/forecast.json';
const WEATHER_API_KEY = process.env.WEATHER_API_KEY;

// Simple in-memory per-user context cache to reduce repeated token usage
const contextCache = new Map();
const CONTEXT_CACHE_TTL_MS = Number(process.env.CONTEXT_CACHE_TTL_MS || 5 * 60 * 1000); // 5 minutes

async function fetchWeatherSummary(lat, lon, dateMillis) {
  if (!lat || !lon || !WEATHER_API_KEY) {
    return null;
  }

  try {
    // WeatherAPI allows 'dt' param for future dates in forecast (up to 14 days for paid, but free/pro vary)
    // If dateMillis is provided and within range, we can use it.
    const params = {
      key: WEATHER_API_KEY,
      q: `${Number(lat)},${Number(lon)}`,
      days: 3,
      aqi: 'no',
      alerts: 'no'
    };

    if (dateMillis) {
      const dt = new Date(dateMillis).toISOString().split('T')[0];
      params.dt = dt;
    }

    const resp = await axios.get(WEATHER_API_URL, {
      params,
      timeout: 10000
    });

    const data = resp.data;
    const loc = `${data.location?.name || ''}, ${data.location?.region || ''}`.trim();

    // If we requested a specific date, find it in forecast
    let condition = '';
    let temp_c = null;

    if (dateMillis) {
        const targetDate = new Date(dateMillis).toISOString().split('T')[0];
        const dayForecast = data.forecast?.forecastday?.find(d => d.date === targetDate);
        if (dayForecast) {
            condition = dayForecast.day?.condition?.text || '';
            temp_c = dayForecast.day?.avgtemp_c;
        } else {
            // Fallback to current if not found in forecast list (though 'dt' should have returned it)
            condition = data.current?.condition?.text || '';
            temp_c = data.current?.temp_c;
        }
    } else {
        condition = data.current?.condition?.text || '';
        temp_c = data.current?.temp_c;
    }

    return {
      location: loc,
      condition,
      temp_c,
      raw: data
    };
  } catch (err) {
    console.warn('contextService.fetchWeatherSummary error:', err.message);
    return null;
  }
}

async function fetchClosetSummary(selectedItemIds = []) {
  try {
    let items = [];
    if (Array.isArray(selectedItemIds) && selectedItemIds.length > 0) {
      items = await ClothingItem.find({ _id: { $in: selectedItemIds } }).lean().limit(50).exec();
    }

    if (!items || items.length === 0) {
      // Fallback: return latest 6 items
      items = await ClothingItem.find({}).sort({ createdAt: -1 }).limit(6).lean().exec();
    }

    const summary = items.map(it => ({
      id: it._id,
      name: it.name || '',
      category: it.category || '',
      color: it.color || '',
      season: it.season || '',
      occasion: it.occasion || '',
      image: it.imageNoBg || it.imageOriginal || ''
    }));

    return { items: summary };
  } catch (err) {
    console.warn('contextService.fetchClosetSummary error:', err.message);
    return { items: [] };
  }
}

async function fetchCalendarSummary(days = 3) {
  try {
    const now = new Date();
    // epoch midnight UTC for today
    const utcMidnight = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
    const from = utcMidnight;
    const to = utcMidnight + (days - 1) * 24 * 3600 * 1000;

    const events = await CalendarEvent.find({ date: { $gte: from, $lte: to } }).sort({ date: 1 }).lean().exec();
    const summary = events.map(e => ({ date: e.date, outfitId: e.outfitId }));
    return { days, events: summary };
  } catch (err) {
    console.warn('contextService.fetchCalendarSummary error:', err.message);
    return { days, events: [] };
  }
}

async function fetchUserProfile(userId) {
  if (!userId) return null;
  try {
    const user = await User.findOne({ userId }).lean().exec();
    if (!user) {
      try {
        const byId = await User.findById(userId).lean().exec();
        if (byId) return {
          userId: byId.userId || String(byId._id),
          name: byId.name || null,
          preferences: byId.preferences || null,
          colorProfile: byId.colorProfile || null,
          sizes: byId.sizes || null,
          raw: byId
        };
      } catch (e) {
        // ignore
      }
      return { userId, preferences: null };
    }
    return {
      userId: user.userId || String(user._id),
      name: user.name || null,
      preferences: user.preferences || null,
      colorProfile: user.colorProfile || null,
      sizes: user.sizes || null,
      raw: user
    };
  } catch (err) {
    console.warn('contextService.fetchUserProfile error:', err.message);
    return { userId, preferences: null };
  }
}

async function buildContext({ userId, lat, lon, dateMillis, selectedItemIds = [], days = 3, forceRefresh = false } = {}) {
  // Compose a cache key using user + location + selected items + date
  const keyParts = [userId || 'anon', lat || '', lon || '', dateMillis || '', (Array.isArray(selectedItemIds) ? selectedItemIds.join(',') : '')];
  const cacheKey = keyParts.join('|');
  if (!forceRefresh) {
    const cached = contextCache.get(cacheKey);
    if (cached && cached.expiresAt > Date.now()) {
      return cached.value;
    }
  }

  const weather = await fetchWeatherSummary(lat, lon, dateMillis);
  const closet = await fetchClosetSummary(selectedItemIds);
  const calendar = await fetchCalendarSummary(days);
  const profile = await fetchUserProfile(userId);

  // Build a concise human-readable summary for RAG insertion
  const parts = [];
  if (weather) {
    const dateLabel = dateMillis ? `on ${new Date(dateMillis).toISOString().split('T')[0]}` : 'currently';
    parts.push(`Weather (${dateLabel}): ${weather.location || ''} — ${weather.condition || ''}, ${weather.temp_c != null ? weather.temp_c + '°C' : ''}`.trim());
  }

  if (Array.isArray(closet.items) && closet.items.length > 0) {
    const itemsText = closet.items.slice(0, 6).map(it => `${it.name || ''}(${it.id}) ${it.category || ''} ${it.color || ''}`.trim()).join('; ');
    parts.push(`Closet (sample): ${itemsText}`);
  } else {
    parts.push('Closet: no items available');
  }

  if (Array.isArray(calendar.events) && calendar.events.length > 0) {
    const eventsText = calendar.events.map(ev => `${new Date(ev.date).toISOString().slice(0,10)} -> outfit:${ev.outfitId}`).join('; ');
    parts.push(`Calendar upcoming: ${eventsText}`);
  }

  if (profile && profile.preferences) {
    parts.push(`Profile preferences: ${JSON.stringify(profile.preferences)}`);
  }

  const summaryText = parts.join(' | ');

  const ctx = {
    userId,
    weather,
    closet,
    calendar,
    profile,
    summaryText
  };

  // Cache and return
  contextCache.set(cacheKey, { value: ctx, expiresAt: Date.now() + CONTEXT_CACHE_TTL_MS });
  return ctx;
}

module.exports = {
  buildContext,
  fetchWeatherSummary,
  fetchClosetSummary,
  fetchUserProfile
};
