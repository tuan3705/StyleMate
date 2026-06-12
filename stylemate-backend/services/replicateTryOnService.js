/**
 * Replicate Try-On Service (cuuupid/idm-vton)
 * 
 * Input: local file paths for body image and garment image
 * Output: URL ảnh kết quả
 * 
 * Lưu ý: Không dùng URL localhost vì Replicate cloud không truy cập được.
 * Phải dùng Buffer/File object để Replicate Node.js client tự động upload.
 */
const Replicate = require('replicate');
const fs = require('fs');
const path = require('path');
const axios = require('axios');

const REPLICATE_API_TOKEN = process.env.REPLICATE_API_TOKEN;
const MODEL_VERSION = 'cuuupid/idm-vton:0513734a452173b8173e907e3a59d19a36266e55b48528559432bd21c7d7e985';

/**
 * Map category string to Replicate's expected category values
 * IDM-VTON supports: "upper_body", "lower_body", "dress"
 */
function mapCategory(category) {
  const cat = (category || '').toLowerCase();
  if (cat.includes('lower') || cat.includes('bottom') || cat.includes('pant') || cat.includes('skirt')) return 'lower_body';
  if (cat.includes('dress') || cat.includes('jumpsuit')) return 'dress';
  return 'upper_body';
}

async function generateTryOn(bodyImagePath, clothImagePath, category = 'upper_body', garmentDes) {
  if (!REPLICATE_API_TOKEN) throw new Error('REPLICATE_API_TOKEN chưa được cấu hình');
  if (!bodyImagePath || !fs.existsSync(bodyImagePath)) throw new Error(`Body image not found: ${bodyImagePath}`);
  if (!clothImagePath || !fs.existsSync(clothImagePath)) throw new Error(`Cloth image not found: ${clothImagePath}`);

  const replicate = new Replicate({ auth: REPLICATE_API_TOKEN });
  const mappedCategory = mapCategory(category);

  // Đọc file thành Buffer - Replicate Node.js client sẽ tự động upload lên server của họ
  const humanBuffer = fs.readFileSync(bodyImagePath);
  const garmBuffer = fs.readFileSync(clothImagePath);

  console.log(`[Replicate] Running IDM-VTON try-on...`);
  console.log(`[Replicate] human_img: ${path.basename(bodyImagePath)} (${(humanBuffer.length / 1024).toFixed(1)} KB)`);
  console.log(`[Replicate] garm_img: ${path.basename(clothImagePath)} (${(garmBuffer.length / 1024).toFixed(1)} KB)`);
  console.log(`[Replicate] category: ${mappedCategory}, garment_des: ${garmentDes || 'auto'}`);

  const input = {
    garm_img: garmBuffer,
    human_img: humanBuffer,
    category: mappedCategory,
    ...(garmentDes ? { garment_des: garmentDes } : {})
  };

  const output = await replicate.run(MODEL_VERSION, { input });

  console.log(`[Replicate] Output type: ${typeof output}`, Array.isArray(output) ? `(array[${output.length}])` : '');

  // Output là Readable Stream (File object từ replicate) có method .url()
  let resultUrl = null;
  if (typeof output === 'string') {
    resultUrl = output;
  } else if (output && typeof output.url === 'function') {
    resultUrl = output.url();
  } else if (output && output.url) {
    resultUrl = output.url;
  } else if (Array.isArray(output)) {
    for (const item of output) {
      if (typeof item === 'string') { resultUrl = item; break; }
      if (item && item.url) { resultUrl = typeof item.url === 'function' ? item.url() : item.url; break; }
    }
  }

  if (!resultUrl) {
    // Fallback: thử convert output toString
    try { resultUrl = output.toString(); } catch(e) {}
  }

  if (!resultUrl) {
    console.error('[Replicate] ❌ Cannot extract result URL. Raw output:', JSON.stringify(output).slice(0, 500));
    throw new Error('Cannot extract result URL from Replicate output');
  }

  console.log(`[Replicate] ✅ Result URL: ${resultUrl}`);
  return { resultUrl };
}

/**
 * Download result image từ Replicate về local server
 */
async function downloadResult(resultUrl, destPath) {
  console.log(`[Replicate] Downloading result to ${destPath}...`);
  const resp = await axios.get(resultUrl, { responseType: 'stream', timeout: 120000 });
  const writer = fs.createWriteStream(destPath);
  await new Promise((resolve, reject) => {
    resp.data.pipe(writer);
    let error = null;
    writer.on('error', err => { error = err; writer.close(); reject(err); });
    writer.on('close', () => {
      if (!error) {
        const stats = fs.statSync(destPath);
        console.log(`[Replicate] ✅ Downloaded: ${destPath} (${(stats.size / 1024).toFixed(1)} KB)`);
        resolve();
      }
    });
  });
}

module.exports = { generateTryOn, downloadResult };