const { mapAutoTaggingToSuggestion } = require('../services/aiAutoTaggingMapper');

const sampleResponse = {
  tags: [
    { label: 'summer', confidence: 0.91 },
    { label: 'casual', confidence: 0.72 },
    { label: 'street style', confidence: 0.6 }
  ]
};

const result = mapAutoTaggingToSuggestion(sampleResponse);
console.log('Auto tagging mapping result:', result);

