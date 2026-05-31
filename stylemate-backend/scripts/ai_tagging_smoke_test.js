const { mapLykdatToItemFields } = require('../services/lykdatTaggingMapper');

const sampleResponse = {
  data: {
    colors: [
      { confidence: 0.44, name: 'silver' },
      { confidence: 0.19, name: 'gainsboro' }
    ],
    items: [
      { category: 'clothing', confidence: 0.96, name: 'outerwear' }
    ],
    labels: [
      { classification: 'apparel', confidence: 0.95, name: 'pants' },
      { classification: 'accessories', confidence: 0.74, name: 'bag' },
      { classification: 'nickname', confidence: 0.56, name: 'set-in sleeve' }
    ]
  }
};

const result = mapLykdatToItemFields(sampleResponse);
console.log('AI fill mapping result:', result);

