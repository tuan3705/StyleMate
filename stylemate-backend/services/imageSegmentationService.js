const axios = require('axios');
const fs = require('fs');
const path = require('path');

const FAL_API_KEY = process.env.FAL_API_KEY;
const FAL_API_ENDPOINT = process.env.FAL_API_ENDPOINT || 'https://queue.fal.run/fal-ai/birefnet/v2';

const UPLOAD_DIR = path.join(__dirname, '..', 'uploads', 'items');
if (!fs.existsSync(UPLOAD_DIR)) fs.mkdirSync(UPLOAD_DIR, { recursive: true });

async function readFileAsBase64(filePath) {
  const buf = fs.readFileSync(filePath);
  const ext = path.extname(filePath).replace('.', '') || 'jpg';
  return `data:image/${ext};base64,${buf.toString('base64')}`;
}

async function removeBackground(localFilePath) {
  // If no FAL key configured, return null to indicate not processed
  if (!FAL_API_KEY) {
    return null;
  }

  try {
    const base64 = await readFileAsBase64(localFilePath);

    const resp = await axios.post(FAL_API_ENDPOINT, {
      image_url: base64,
      model: 'General Use (Light)',
      operating_resolution: '1024x1024',
      output_format: 'png',
      refine_foreground: true
    }, {
      headers: { Authorization: `Key ${FAL_API_KEY}`, 'Content-Type': 'application/json' },
      timeout: 10000
    });

    const data = resp.data;
    const statusUrl = data.status_url;
    const responseUrl = data.response_url;
    if (!statusUrl || !responseUrl) {
      throw new Error('Invalid response from background removal provider');
    }

    // Poll status
    let status = '';
    const start = Date.now();
    while (status !== 'COMPLETED') {
      const statusResp = await axios.get(statusUrl, { headers: { Authorization: `Key ${FAL_API_KEY}` }, timeout: 10000 });
      status = statusResp.data.status;
      if (status === 'FAILED' || status === 'CANCELLED') throw new Error(`Background removal ${status}`);
      if (Date.now() - start > 60_000) throw new Error('Background removal timeout');
      if (status !== 'COMPLETED') await new Promise(r => setTimeout(r, 500));
    }

    const resultResp = await axios.get(responseUrl, { headers: { Authorization: `Key ${FAL_API_KEY}` }, timeout: 10000 });
    const imageUrl = resultResp.data.image?.url;
    if (!imageUrl) throw new Error('No image returned from background removal');

    // Download the result to local uploads
    const filename = `nobg_${Date.now().toString(36)}.png`;
    const localPath = path.join(UPLOAD_DIR, filename);
    const writer = fs.createWriteStream(localPath);

    const downloadResp = await axios.get(imageUrl, { responseType: 'stream', timeout: 20000 });
    await new Promise((resolve, reject) => {
      downloadResp.data.pipe(writer);
      let error = null;
      writer.on('error', err => { error = err; writer.close(); reject(err); });
      writer.on('close', () => { if (!error) resolve(); });
    });

    return { localPath, publicUrl: `/uploads/items/${filename}` };
  } catch (err) {
    console.warn('imageSegmentationService.removeBackground error:', err.message);
    return null;
  }
}

module.exports = {
  removeBackground
};
