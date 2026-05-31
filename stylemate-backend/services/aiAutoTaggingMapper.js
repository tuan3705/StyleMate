const normalize = (value) => (typeof value === 'string' ? value.trim().toLowerCase() : '');

const SEASON_RULES = [
  { season: 'Spring', keywords: ['spring', 'floral', 'pastel', 'light jacket', 'breeze'] },
  { season: 'Summer', keywords: ['summer', 'hot', 'beach', 'sunny', 'tank', 'shorts', 'sleeveless'] },
  { season: 'Autumn', keywords: ['autumn', 'fall', 'earth tone', 'layer', 'knit', 'cardigan'] },
  { season: 'Winter', keywords: ['winter', 'snow', 'coat', 'wool', 'heavy', 'puffer'] }
];

const OCCASION_RULES = [
  { occasion: 'Casual', keywords: ['casual', 'everyday', 'street', 'relaxed', 'weekend'] },
  { occasion: 'Work', keywords: ['work', 'office', 'business', 'smart', 'formal office'] },
  { occasion: 'Sports', keywords: ['sports', 'gym', 'athletic', 'running', 'training'] },
  { occasion: 'Formal', keywords: ['formal', 'party', 'evening', 'dressy', 'gala'] }
];

const matchByRules = (tags, rules, key) => {
  for (const tag of tags) {
    const label = normalize(tag.label);
    for (const rule of rules) {
      if (rule.keywords.some((keyword) => label.includes(keyword))) {
        return rule[key];
      }
    }
  }
  return null;
};

const mapAutoTaggingToSuggestion = (apiResponse) => {
  const tags = Array.isArray(apiResponse?.tags) ? apiResponse.tags : [];
  const sortedTags = [...tags].sort((a, b) => (b.confidence || 0) - (a.confidence || 0));

  const season = matchByRules(sortedTags, SEASON_RULES, 'season');
  const occasion = matchByRules(sortedTags, OCCASION_RULES, 'occasion');

  return {
    season,
    occasion,
    tags: sortedTags
  };
};

module.exports = {
  mapAutoTaggingToSuggestion
};

