const axios = require('axios');
const FormData = require('form-data');

const DEFAULT_ENDPOINT = 'https://cloudapi.lykdat.com/v1/detection/tags';

const tagImage = async ({ buffer, filename, mimetype }) => {
  const apiKey = process.env.LYKDAT_TAGGING_API_KEY;
  if (!apiKey) {
    throw new Error('LYKDAT_TAGGING_API_KEY is not configured');
  }

  const formData = new FormData();
  formData.append('image', buffer, {
    filename: filename || 'image.jpg',
    contentType: mimetype || 'application/octet-stream'
  });

  const response = await axios.post(
    process.env.LYKDAT_TAGGING_ENDPOINT || DEFAULT_ENDPOINT,
    formData,
    {
      headers: {
        'x-api-key': apiKey,
        ...formData.getHeaders()
      },
      timeout: 15000
    }
  );

  return response.data;
};

module.exports = {
  tagImage
};

