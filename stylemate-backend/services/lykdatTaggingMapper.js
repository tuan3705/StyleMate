const CATEGORY_VALUES = {
  TOPS: 'Tops',
  BOTTOMS: 'Bottoms',
  DRESSES: 'Dresses',
  FOOTWEAR: 'Footwear',
  BAGS: 'Bags',
  ACCESSORIES: 'Accessories'
};

const TOPS = new Set([
  'top',
  'shirt',
  'blouse',
  'tshirt',
  't-shirt',
  'sweater',
  'hoodie',
  'jacket',
  'coat',
  'outerwear'
]);
const BOTTOMS = new Set(['pants', 'trousers', 'jeans', 'skirt', 'shorts']);
const DRESSES = new Set(['dress', 'gown']);
const FOOTWEAR = new Set(['shoe', 'shoes', 'sneaker', 'boot', 'boots', 'sandals', 'heels']);
const BAGS = new Set(['bag', 'backpack', 'handbag']);

const normalize = (value) => (typeof value === 'string' ? value.trim().toLowerCase() : '');

const buildCategoryCandidates = ({ labels = [], items = [] }) => {
  const candidates = [];

  labels.forEach((label) => {
    const classification = normalize(label.classification);
    const name = normalize(label.name);
    const confidence = Number(label.confidence) || 0;

    if (classification === 'accessories') {
      if (BAGS.has(name)) {
        candidates.push({ category: CATEGORY_VALUES.BAGS, confidence, source: 'labels' });
      } else if (name) {
        candidates.push({ category: CATEGORY_VALUES.ACCESSORIES, confidence, source: 'labels' });
      }
      return;
    }

    if (classification === 'apparel') {
      if (FOOTWEAR.has(name)) {
        candidates.push({ category: CATEGORY_VALUES.FOOTWEAR, confidence, source: 'labels' });
      } else if (BOTTOMS.has(name)) {
        candidates.push({ category: CATEGORY_VALUES.BOTTOMS, confidence, source: 'labels' });
      } else if (DRESSES.has(name)) {
        candidates.push({ category: CATEGORY_VALUES.DRESSES, confidence, source: 'labels' });
      } else if (TOPS.has(name)) {
        candidates.push({ category: CATEGORY_VALUES.TOPS, confidence, source: 'labels' });
      }
    }
  });

  items.forEach((item) => {
    const name = normalize(item.name);
    const confidence = Number(item.confidence) || 0;

    if (TOPS.has(name)) {
      candidates.push({ category: CATEGORY_VALUES.TOPS, confidence, source: 'items' });
    } else if (BOTTOMS.has(name)) {
      candidates.push({ category: CATEGORY_VALUES.BOTTOMS, confidence, source: 'items' });
    } else if (DRESSES.has(name)) {
      candidates.push({ category: CATEGORY_VALUES.DRESSES, confidence, source: 'items' });
    } else if (FOOTWEAR.has(name)) {
      candidates.push({ category: CATEGORY_VALUES.FOOTWEAR, confidence, source: 'items' });
    } else if (BAGS.has(name)) {
      candidates.push({ category: CATEGORY_VALUES.BAGS, confidence, source: 'items' });
    }
  });

  candidates.sort((a, b) => b.confidence - a.confidence);
  return candidates;
};

const pickBestColor = (colors = []) => {
  if (!Array.isArray(colors) || colors.length === 0) {
    return null;
  }

  const best = [...colors].sort((a, b) => (b.confidence || 0) - (a.confidence || 0))[0];
  return best
    ? {
        value: best.name || null,
        confidence: Number(best.confidence) || 0
      }
    : null;
};

const pickBestName = (labels = []) => {
  if (!Array.isArray(labels) || labels.length === 0) {
    return null;
  }

  const nicknameLabels = labels.filter((label) => normalize(label.classification) === 'nickname');
  const apparelLabels = labels.filter((label) => normalize(label.classification) === 'apparel');

  const pickFrom = (list) =>
    list
      .map((label) => ({
        value: label.name || null,
        confidence: Number(label.confidence) || 0
      }))
      .sort((a, b) => b.confidence - a.confidence)[0] || null;

  return pickFrom(nicknameLabels) || pickFrom(apparelLabels);
};

const mapLykdatToItemFields = (apiResponse) => {
  const data = apiResponse && apiResponse.data ? apiResponse.data : {};
  const labels = Array.isArray(data.labels) ? data.labels : [];
  const items = Array.isArray(data.items) ? data.items : [];
  const colors = Array.isArray(data.colors) ? data.colors : [];

  const categoryCandidates = buildCategoryCandidates({ labels, items });
  const bestCategory = categoryCandidates[0] || null;
  const bestColor = pickBestColor(colors);
  const bestName = pickBestName(labels);

  return {
    category: bestCategory ? bestCategory.category : null,
    categoryConfidence: bestCategory ? bestCategory.confidence : 0,
    categorySource: bestCategory ? bestCategory.source : null,
    color: bestColor ? bestColor.value : null,
    colorConfidence: bestColor ? bestColor.confidence : 0,
    name: bestName ? bestName.value : null,
    nameConfidence: bestName ? bestName.confidence : 0,
    candidates: {
      categories: categoryCandidates.slice(0, 5)
    }
  };
};

module.exports = {
  mapLykdatToItemFields
};

