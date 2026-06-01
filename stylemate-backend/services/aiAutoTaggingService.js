const axios = require('axios');
const FormData = require('form-data');
const { AppError } = require('../middleware/errorHandler');

const DEFAULT_ENDPOINT = 'https://aiautotagging.com/api/tag/image';
const REQUEST_TIMEOUT_MS = 20000;

const tagImage = async ({ buffer, filename, additionalContext }) => {
  const endpoint = process.env.AI_AUTOTAGGING_ENDPOINT || DEFAULT_ENDPOINT;
  const apiKey = process.env.AI_AUTOTAGGING_API_KEY;

  const formData = new FormData();
  formData.append('file', buffer, {
    filename: filename || 'upload.jpg'
  });
  formData.append('options', JSON.stringify({
    maxTags: 12,
    includeConfidence: true,
    additionalContext
  }));

  try {
    const response = await axios.post(endpoint, formData, {
      headers: {
        ...formData.getHeaders(),
        ...(apiKey ? { Authorization: apiKey } : {})
      },
      timeout: REQUEST_TIMEOUT_MS
    });

    return response.data;
  } catch (error) {
    if (!error.response) {
      throw new AppError('Không thể kết nối tới AI auto tagging', 502);
    }

    const status = error.response.status || 502;
    throw new AppError('AI auto tagging xử lý thất bại', status);
  }
};

module.exports = {
  tagImage
};

