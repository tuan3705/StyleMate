const axios = require('axios');
const FormData = require('form-data');
const sharp = require('sharp');

const DEFAULT_ENDPOINT = 'https://cloudapi.lykdat.com/v1/detection/tags';
const MAX_FILE_SIZE = 2.5 * 1024 * 1024; // 2.5MB (under Lykdat's 3MB limit)

/**
 * Resize image if > 2.5MB so Lykdat doesn't reject it.
 */
async function compressImage(buffer) {
  if (buffer.length <= MAX_FILE_SIZE) return buffer;

  console.log(`[Lykdat] Image too large (${(buffer.length / 1024 / 1024).toFixed(2)}MB), compressing...`);

  // Try resizing to 1200px wide, quality 80%
  let compressed = await sharp(buffer)
    .resize(1200, undefined, { fit: 'inside', withoutEnlargement: true })
    .jpeg({ quality: 80 })
    .toBuffer();

  // If still > 2.5MB, reduce quality further
  let quality = 70;
  while (compressed.length > MAX_FILE_SIZE && quality > 30) {
    compressed = await sharp(buffer)
      .resize(800, undefined, { fit: 'inside', withoutEnlargement: true })
      .jpeg({ quality })
      .toBuffer();
    quality -= 10;
  }

  console.log(`[Lykdat] Compressed to ${(compressed.length / 1024 / 1024).toFixed(2)}MB (quality=${quality + 10})`);
  return compressed;
}

const tagImage = async ({ buffer, filename, mimetype }) => {
  const apiKey = process.env.LYKDAT_TAGGING_API_KEY;
  if (!apiKey) {
    throw new Error('LYKDAT_TAGGING_API_KEY is not configured');
  }

  // ⚡ Compress image if too large
  const resizedBuffer = await compressImage(buffer);

  const formData = new FormData();
  formData.append('image', resizedBuffer, {
    filename: filename || 'image.jpg',
    contentType: mimetype || 'application/octet-stream'
  });

  console.log(`[Lykdat] Calling API with key=${apiKey.slice(0,8)}..., filename=${filename}`);

  try {
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
  } catch (err) {
    if (err.response) {
      console.error('[Lykdat] HTTP error:', err.response.status, JSON.stringify(err.response.data));
    } else {
      console.error('[Lykdat] Network error:', err.message);
    }
    throw err;
  }
};

module.exports = {
  tagImage
};

