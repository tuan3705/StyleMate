const normalize = (value) => (typeof value === 'string' ? value.trim().toLowerCase() : '');

const normalizeTagToken = (text) => {
  if (typeof text !== 'string') {
    return '';
  }
  return normalize(text)
    .replace(/^label\s+/, '')
    .replace(/^confidence\s+/, '')
    .replace(/^reason\s+/, '')
    .replace(/^tags\s+/, '')
    .replace(/^json\s+/, '')
    .replace(/^url\s+/, '')
    .replace(/^meta\s+/, '')
    .trim();
};

const extractTagText = (tag) => {
  if (typeof tag === 'string') {
    return normalizeTagToken(tag);
  }
  if (!tag || typeof tag !== 'object') {
    return '';
  }
  const fields = [
    tag.label,
    tag.name,
    tag.tag,
    tag.value,
    tag.class,
    tag.category,
    tag.description,
    tag.reason
  ];
  return fields
    .filter((field) => typeof field === 'string' && field.trim().length > 0)
    .map(normalizeTagToken)
    .filter((value) => value.length > 0)
    .join(' ');
};

const parseTagsFromRaw = (raw) => {
  if (typeof raw !== 'string') {
    return [];
  }
  const blocks = [];
  const regex = /```(?:json)?\s*([\s\S]*?)```/gi;
  let match;
  while ((match = regex.exec(raw)) !== null) {
    blocks.push(match[1]);
  }
  for (const block of blocks) {
    const trimmed = block.trim();
    if (!trimmed) continue;
    try {
      const payload = JSON.parse(trimmed);
      if (Array.isArray(payload?.tags)) {
        return payload.tags;
      }
    } catch (error) {
      const start = trimmed.indexOf('{');
      const end = trimmed.lastIndexOf('}');
      if (start >= 0 && end > start) {
        const snippet = trimmed.slice(start, end + 1);
        try {
          const payload = JSON.parse(snippet);
          if (Array.isArray(payload?.tags)) {
            return payload.tags;
          }
        } catch (parseError) {
          continue;
        }
      }
    }
  }
  return [];
};

const expandCompositeTags = (texts) => {
  const expanded = [];
  for (const text of texts) {
    if (!text) continue;
    expanded.push(text);
    const parts = text.split(/[\/&]/g).map((part) => normalize(part)).filter(Boolean);
    expanded.push(...parts);
  }
  return Array.from(new Set(expanded));
};

const matchByRules = (texts, rules, key) => {
  const expandedTexts = expandCompositeTags(texts);
  for (const text of expandedTexts) {
    for (const rule of rules) {
      if (rule.keywords.some((keyword) => text.includes(keyword))) {
        return rule[key];
      }
    }
  }
  return null;
};

const SEASON_RULES = [
  { season: 'Spring', keywords: ['spring', 'floral', 'pastel', 'light jacket', 'breeze', 'trench', 'cardigan'] },
  { season: 'Summer', keywords: ['summer', 'hot', 'beach', 'sunny', 'tank', 'shorts', 'sleeveless', 'linen', 'short sleeve', 't-shirt'] },
  { season: 'Autumn', keywords: ['autumn', 'fall', 'earth tone', 'layer', 'knit', 'cardigan', 'hoodie', 'sweater'] },
  { season: 'Winter', keywords: ['winter', 'snow', 'coat', 'wool', 'heavy', 'puffer', 'jacket', 'long sleeve', 'parka'] }
];

const OCCASION_RULES = [
  { occasion: 'Casual', keywords: ['casual', 'everyday', 'street', 'relaxed', 'weekend', 'denim', 'tee', 'casual wear'] },
  { occasion: 'Work', keywords: ['work', 'office', 'business', 'smart', 'formal office', 'blazer', 'suit', 'shirt', 'work wear'] },
  { occasion: 'Sports', keywords: ['sports', 'gym', 'athletic', 'running', 'training', 'yoga', 'tennis', 'sports wear', 'workout'] },
  { occasion: 'Formal', keywords: ['formal', 'party', 'evening', 'dressy', 'gala', 'wedding', 'formal wear'] }
];

const mapAutoTaggingToSuggestion = (apiResponse) => {
  const fallbackTags = Array.isArray(apiResponse?.tags)
    ? apiResponse.tags
    : Array.isArray(apiResponse?.data?.tags)
      ? apiResponse.data.tags
      : Array.isArray(apiResponse?.result?.tags)
        ? apiResponse.result.tags
        : [];
  const rawTags = parseTagsFromRaw(apiResponse?.raw);
  const tags = rawTags.length > 0 ? rawTags : fallbackTags;
  const sortedTags = [...tags].sort((a, b) => (b.confidence || 0) - (a.confidence || 0));
  const tagTexts = sortedTags.map(extractTagText).filter((text) => text.length > 0);

  const directSeasonText = normalize(apiResponse?.season || apiResponse?.data?.season || apiResponse?.result?.season);
  const directOccasionText = normalize(apiResponse?.occasion || apiResponse?.data?.occasion || apiResponse?.result?.occasion);

  const season = matchByRules(
    directSeasonText ? [directSeasonText, ...tagTexts] : tagTexts,
    SEASON_RULES,
    'season'
  );
  const occasion = matchByRules(
    directOccasionText ? [directOccasionText, ...tagTexts] : tagTexts,
    OCCASION_RULES,
    'occasion'
  );

  return {
    season,
    occasion,
    tags: sortedTags
  };
};

module.exports = {
  mapAutoTaggingToSuggestion
};
