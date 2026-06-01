const axios = require('axios');
const FormData = require('form-data');
const { AppError } = require('../middleware/errorHandler');

const REMOVE_BG_API_URL = 'https://api.remove.bg/v1.0/removebg';
const REMOVE_BG_TIMEOUT_MS = 30000;

const parseRemoveBgError = (error) => {
  if (!error.response) {
    return new AppError('Không thể kết nối tới remove.bg', 502);
  }

  const status = error.response.status || 502;
  const contentType = error.response.headers?.['content-type'] || '';

  if (contentType.includes('application/json')) {
    try {
      const payload = JSON.parse(Buffer.from(error.response.data).toString('utf8'));
      const message = payload?.errors?.[0]?.title || payload?.errors?.[0]?.detail || 'Remove.bg lỗi không xác định';
      return new AppError(message, status);
    } catch (parseError) {
      return new AppError('Remove.bg trả về lỗi không hợp lệ', status);
    }
  }

  return new AppError('Remove.bg xử lý thất bại', status);
};

const removeBackground = async ({ buffer, filename }) => {
  const apiKey = process.env.REMOVE_BG_API_KEY;
  if (!apiKey) {
    throw new AppError('REMOVE_BG_API_KEY chưa được cấu hình', 500);
  }

  const formData = new FormData();
  formData.append('image_file', buffer, {
    filename: filename || 'upload.jpg'
  });
  formData.append('size', 'auto');
  formData.append('format', 'png');

  try {
    const response = await axios.post(REMOVE_BG_API_URL, formData, {
      headers: {
        ...formData.getHeaders(),
        'X-Api-Key': apiKey
      },
      responseType: 'arraybuffer',
      timeout: REMOVE_BG_TIMEOUT_MS,
      maxContentLength: 20 * 1024 * 1024,
      maxBodyLength: 20 * 1024 * 1024
    });

    return response.data;
  } catch (error) {
    throw parseRemoveBgError(error);
  }
};

module.exports = {
  removeBackground
};

