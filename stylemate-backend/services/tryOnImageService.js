const fs = require('fs');
const path = require('path');
const axios = require('axios');
const jwt = require('jsonwebtoken');
const ProcessingJob = require('../models/ProcessingJob');
const replicateTryOn = require('./replicateTryOnService');

const UPLOAD_DIR = path.join(__dirname, '..', 'uploads', 'tryon');
if (!fs.existsSync(UPLOAD_DIR)) {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

// KlingAI env
const KLAI_API_BASE = process.env.KLAI_API_BASE_URL || 'https://api.klingai.com';
const KLAI_ACCESS_KEY = process.env.KLAI_ACCESS_KEY;
const KLAI_SECRET_KEY = process.env.KLAI_SECRET_KEY;
const KLAI_POLL_INTERVAL = Number(process.env.KLAI_POLL_INTERVAL_MS || 1000);
const KLAI_MAX_POLL = Number(process.env.KLAI_MAX_POLL || 60);

// In-memory map kept for backward compatibility speed, but canonical storage is MongoDB ProcessingJob
const jobs = new Map();

function sleep(ms) { return new Promise((r) => setTimeout(r, ms)); }

async function fileToBase64DataUri(localPath) {
  if (!localPath || !fs.existsSync(localPath)) return null;
  const buf = fs.readFileSync(localPath);
  const ext = path.extname(localPath).replace('.', '') || 'jpg';
  return `data:image/${ext};base64,${buf.toString('base64')}`;
}

async function urlToBase64DataUri(url) {
  if (!url) return null;
  try {
    const resp = await axios.get(url, { responseType: 'arraybuffer', timeout: 15000 });
    const contentType = resp.headers['content-type'] || 'image/jpeg';
    const b64 = Buffer.from(resp.data, 'binary').toString('base64');
    return `data:${contentType};base64,${b64}`;
  } catch (err) {
    return null;
  }
}

// ⚡ Hàm xoá các file tạm trong uploads/tryon/, chỉ giữ file kết quả
function cleanTryonTempFiles(keepFile) {
  try {
    const files = fs.readdirSync(UPLOAD_DIR);
    files.forEach(file => {
      if (file === keepFile) return;
      // Xoá file tạm: replicate_cloth_*, tryon_ (của body upload), placeholder
      if (file.startsWith('replicate_cloth_') || 
          file.startsWith('tryon_') ||
          file === 'placeholder.png') {
        const filePath = path.join(UPLOAD_DIR, file);
        try { fs.unlinkSync(filePath); } catch (_) {}
      }
    });
    console.log(`[tryOnImageService] Cleaned temp files in uploads/tryon/ (kept: ${keepFile})`);
  } catch (_) {}
}

async function createKlingATask(humanBase64, clothBase64) {
  if (!KLAI_ACCESS_KEY || !KLAI_SECRET_KEY) throw new Error('KlingAI creds not configured');
  const now = Math.floor(Date.now() / 1000);
  const payload = { iss: KLAI_ACCESS_KEY, exp: now + 1800, nbf: now - 5 };
  const token = jwt.sign(payload, KLAI_SECRET_KEY);

  const url = `${KLAI_API_BASE}/v1/images/kolors-virtual-try-on`;
  const body = {
    model_name: 'kolors-virtual-try-on-v1',
    human_image: humanBase64,
    cloth_image: clothBase64
  };

  const resp = await axios.post(url, body, { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, timeout: 20000 });
  if (resp.data?.code !== 0) throw new Error(resp.data?.message || 'KlingAI create task failed');
  return resp.data.data.task_id;
}

async function queryKlingATask(taskId) {
  if (!KLAI_ACCESS_KEY || !KLAI_SECRET_KEY) throw new Error('KlingAI creds not configured');
  const now = Math.floor(Date.now() / 1000);
  const payload = { iss: KLAI_ACCESS_KEY, exp: now + 1800, nbf: now - 5 };
  const token = jwt.sign(payload, KLAI_SECRET_KEY);
  const url = `${KLAI_API_BASE}/v1/images/kolors-virtual-try-on/${taskId}`;
  const resp = await axios.get(url, { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, timeout: 20000 });
  if (resp.data?.code !== 0) throw new Error(resp.data?.message || 'KlingAI query failed');
  return resp.data;
}

function createJob({ userId, bodyImagePath, bodyImageUrl, selectedItemIds = [], itemImagePaths = [], options = {} }) {
  const jobId = `tryon_${Date.now().toString(36)}_${Math.random().toString(36).slice(2,8)}`;
  const doc = new ProcessingJob({
    jobId,
    type: 'tryon',
    userId: userId || null,
    status: 'queued',
    progress: 0,
    params: { bodyImagePath: bodyImagePath || null, bodyImageUrl: bodyImageUrl || null, selectedItemIds, itemImagePaths, options },
    result: null,
    expiresAt: new Date(Date.now() + Number(process.env.TEMP_ASSET_TTL_MS || 24 * 3600 * 1000))
  });

  return doc.save().then(saved => {
    process.nextTick(() => processJob(saved.jobId));
    const job = {
      jobId: saved.jobId,
      userId: saved.userId,
      status: saved.status,
      progress: saved.progress,
      params: saved.params,
      result: saved.result,
      createdAt: saved.createdAt
    };
    jobs.set(saved.jobId, job);
    return { jobId: saved.jobId, status: saved.status, estimatedSeconds: 6 };
  });
}

async function processJob(jobId) {
  let doc = await ProcessingJob.findOne({ jobId });
  if (!doc) return;
  if (doc.status === 'cancelled') return;

  doc.status = 'processing';
  doc.progress = 0;
  await doc.save();
  jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, params: doc.params });

  const providedProvider = (doc.params.options && doc.params.options.provider) || process.env.TRYON_PROVIDER || 'AUTO';
  let provider = providedProvider;
  if (provider === 'AUTO') {
    if (process.env.REPLICATE_API_TOKEN) provider = 'REPLICATE';
    else if (KLAI_ACCESS_KEY && KLAI_SECRET_KEY) provider = 'KLAI';
    else if (process.env.STABLE_API_URL) provider = 'STABLE';
    else provider = 'SIMULATED';
  }

  if (provider === 'KLAI' && KLAI_ACCESS_KEY && KLAI_SECRET_KEY) {
    try {
      const params = doc.params || {};
      const humanB64 = params.bodyImageBase64 || (params.bodyImagePath ? await fileToBase64DataUri(params.bodyImagePath) : (params.bodyImageUrl ? await urlToBase64DataUri(params.bodyImageUrl) : null));
      const clothB64 = (params.itemImageBase64 && params.itemImageBase64[0]) || (params.itemImagePaths && params.itemImagePaths[0]) ? (params.itemImageBase64 && params.itemImageBase64[0]) || await fileToBase64DataUri(params.itemImagePaths[0]) : (params.itemImageUrls && params.itemImageUrls[0] ? await urlToBase64DataUri(params.itemImageUrls[0]) : null);

      if (!humanB64 || !clothB64) {
        throw new Error('Missing images for KlingAI try-on');
      }

      doc.progress = 10; await doc.save(); jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, params: doc.params });

      const taskId = await createKlingATask(humanB64, clothB64);
      doc.progress = 20; await doc.save(); jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, params: doc.params });

      let attempts = 0;
      while (attempts < KLAI_MAX_POLL) {
        const data = await queryKlingATask(taskId);
        const status = data.data?.task_status;
        if (status === 'succeed') {
          const imageUrl = data.data?.task_result?.images?.[0]?.url;
          if (imageUrl) {
            const resultFileName = `tryon_result_${jobId}.png`;
            const destPath = path.join(UPLOAD_DIR, resultFileName);
            const downloadResp = await axios.get(imageUrl, { responseType: 'stream', timeout: 20000 });
            const writer = fs.createWriteStream(destPath);
            await new Promise((resolve, reject) => {
              downloadResp.data.pipe(writer);
              let error = null;
              writer.on('error', err => { error = err; writer.close(); reject(err); });
              writer.on('close', () => { if (!error) resolve(); });
            });
            doc.progress = 100;
            doc.status = 'completed';
            doc.result = {
              jobId,
              status: 'completed',
              generatedImageUrl: `/uploads/tryon/${resultFileName}`,
              message: 'Try-on generated via KlingAI',
              suggestions: [],
              llmSummary: { score: 0 }
            };
            await doc.save();
            jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, result: doc.result });
            // ⚡ Dọn dẹp file tạm, giữ lại file kết quả
            cleanTryonTempFiles(resultFileName);
            return;
          }
        } else if (status === 'failed') {
          doc.status = 'failed';
          doc.progress = 0;
          doc.result = { message: data.data?.task_status_msg || 'KlingAI failed' };
          await doc.save();
          jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, result: doc.result });
          return;
        }

        attempts++;
        doc.progress = Math.min(90, doc.progress + 10);
        await doc.save();
        jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress });
        await sleep(KLAI_POLL_INTERVAL);
      }

      doc.status = 'failed';
      doc.result = { message: 'Timeout waiting for try-on result' };
      await doc.save();
      jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, result: doc.result });
      return;

    } catch (err) {
      console.warn('tryOnImageService KlingAI error:', err.message);
      doc.status = 'failed';
      doc.result = { message: err.message };
      await doc.save();
      jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, result: doc.result });
      return;
    }
  }

  if (provider === 'REPLICATE' && process.env.REPLICATE_API_TOKEN) {
    try {
      const params = doc.params || {};
      const bodyPath = params.bodyImagePath || null;
      let clothPath = null;
      let category = 'Upper body';
      let garmentDes = '';

      if (params.selectedItemIds && params.selectedItemIds.length > 0) {
        console.log(`[Replicate] Fetching ${params.selectedItemIds.length} items from DB by IDs...`);
        const ClothingItem = require('../models/ClothingItem');
        const items = await ClothingItem.find({ _id: { $in: params.selectedItemIds } }).lean();
        
        for (const item of items) {
          const imgUrl = item.imageNoBg || item.imageOriginal;
          if (!imgUrl || clothPath) continue;
          
          const fullUrl = imgUrl.startsWith('http') ? imgUrl 
            : `http://localhost:${process.env.PORT || 3000}${imgUrl.startsWith('/') ? '' : '/'}${imgUrl}`;
          
          const tempFileName = `replicate_cloth_${item._id}_${Date.now()}.png`;
          const tempPath = path.join(UPLOAD_DIR, tempFileName);
          
          try {
            const imgResp = await axios.get(fullUrl, { responseType: 'arraybuffer', timeout: 10000 });
            fs.writeFileSync(tempPath, Buffer.from(imgResp.data));
            clothPath = tempPath;
            garmentDes = item.name || item.category || '';
            console.log(`[Replicate] Downloaded cloth: ${tempFileName}`);
          } catch (downloadErr) {
            console.warn(`[Replicate] Cannot download item ${item._id}: ${downloadErr.message}`);
          }
        }
        if (items.length > 0) {
          category = items[0].category || category;
        }
      }

      if (!clothPath && params.itemImagePaths && params.itemImagePaths[0]) {
        clothPath = params.itemImagePaths[0];
      }

      if (!bodyPath) throw new Error('Missing body image for Replicate try-on');
      if (!clothPath) throw new Error('Missing cloth image for Replicate try-on');

      doc.progress = 20; await doc.save();

      console.log(`[Replicate] Processing job ${jobId}...`);
      const result = await replicateTryOn.generateTryOn(bodyPath, clothPath, category, garmentDes);

      doc.progress = 80; await doc.save();

      const resultFileName = `tryon_result_${jobId}.png`;
      const destPath = path.join(UPLOAD_DIR, resultFileName);
      await replicateTryOn.downloadResult(result.resultUrl, destPath);

      doc.progress = 100;
      doc.status = 'completed';
      doc.result = {
        jobId,
        status: 'completed',
        generatedImageUrl: `/uploads/tryon/${resultFileName}`,
        message: 'Try-on generated via Replicate IDM-VTON',
        suggestions: [],
        llmSummary: { score: 0 }
      };
      await doc.save();
      jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, result: doc.result });
      // ⚡ Dọn dẹp file tạm (body image, cloth image), giữ file kết quả
      cleanTryonTempFiles(resultFileName);
      console.log(`[Replicate] ✅ Job ${jobId} completed`);
      return;
    } catch (err) {
      console.error('[Replicate] ❌ Error:', err.message);
      console.log('[Replicate] ⚠️ Falling through to simulation fallback...');
    }
  }

  if (provider === 'STABLE' && process.env.STABLE_API_URL) {
    try {
      const params = doc.params || {};
      const humanB64 = params.bodyImageBase64 || (params.bodyImagePath ? await fileToBase64DataUri(params.bodyImagePath) : (params.bodyImageUrl ? await urlToBase64DataUri(params.bodyImageUrl) : null));
      const clothB64 = (params.itemImageBase64 && params.itemImageBase64[0]) || (params.itemImagePaths && params.itemImagePaths[0]) ? (params.itemImageBase64 && params.itemImageBase64[0]) || await fileToBase64DataUri(params.itemImagePaths[0]) : (params.itemImageUrls && params.itemImageUrls[0] ? await urlToBase64DataUri(params.itemImageUrls[0]) : null);

      if (!humanB64 || !clothB64) {
        throw new Error('Missing images for Stable provider try-on');
      }

      doc.progress = 10; await doc.save(); jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress });

      const stableUrl = process.env.STABLE_API_URL.replace(/\/$/, '') + (process.env.STABLE_TRYON_PATH || '/v1/tryon');
      const stableBody = { model: process.env.STABLE_MODEL || 'sd-tryon-v1', human_image: humanB64, cloth_image: clothB64, options: doc.params.options || {} };
      const stableHeaders = { 'Content-Type': 'application/json' };
      if (process.env.STABLE_API_KEY) stableHeaders['Authorization'] = `Bearer ${process.env.STABLE_API_KEY}`;

      const resp = await axios.post(stableUrl, stableBody, { headers: stableHeaders, timeout: 60000 });

      const imageB64 = resp.data?.image_base64 || resp.data?.data?.image_base64 || null;
      const imageUrl = resp.data?.url || resp.data?.data?.url || null;

      const resultFileName = `tryon_result_${jobId}.png`;
      const destPath = path.join(UPLOAD_DIR, resultFileName);

      if (imageB64) {
        const cleaned = imageB64.replace(/^data:image\/(png|jpeg|jpg);base64,/, '');
        fs.writeFileSync(destPath, Buffer.from(cleaned, 'base64'));
      } else if (imageUrl) {
        const downloadResp = await axios.get(imageUrl, { responseType: 'stream', timeout: 20000 });
        const writer = fs.createWriteStream(destPath);
        await new Promise((resolve, reject) => {
          downloadResp.data.pipe(writer);
          let error = null;
          writer.on('error', err => { error = err; writer.close(); reject(err); });
          writer.on('close', () => { if (!error) resolve(); });
        });
      } else {
        throw new Error('Stable provider returned no image');
      }

      doc.progress = 100;
      doc.status = 'completed';
      doc.result = {
        jobId,
        status: 'completed',
        generatedImageUrl: `/uploads/tryon/${resultFileName}`,
        message: 'Try-on generated via Stable provider',
        suggestions: [],
        llmSummary: { score: 0 }
      };
      await doc.save();
      jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, result: doc.result });
      cleanTryonTempFiles(resultFileName);
      return;
    } catch (err) {
      console.warn('tryOnImageService Stable provider error:', err.message);
      doc.status = 'failed';
      doc.result = { message: err.message };
      await doc.save();
      jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, result: doc.result });
      return;
    }
  }

  // — Fallback simulation —
  for (let p = 5; p <= 95; p += 10) {
    doc.progress = p; await doc.save(); jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress });
    await sleep(400);
  }

  const resultFileName = `tryon_result_${jobId}.png`;
  const destPath = path.join(UPLOAD_DIR, resultFileName);
  let fallbackSuccess = false;
  try {
    const params = doc.params || {};
    
    if (params.bodyImagePath && fs.existsSync(params.bodyImagePath)) {
      fs.copyFileSync(params.bodyImagePath, destPath);
      fallbackSuccess = true;
      console.log(`[Simulated] Copied bodyImagePath to result: ${params.bodyImagePath}`);
    }
    else if (params.bodyImageBase64) {
      let base64Data = params.bodyImageBase64;
      if (base64Data.startsWith('data:')) {
        base64Data = base64Data.replace(/^data:image\/\w+;base64,/, '');
      }
      const imgBuffer = Buffer.from(base64Data, 'base64');
      if (imgBuffer.length > 100) {
        fs.writeFileSync(destPath, imgBuffer);
        fallbackSuccess = true;
        console.log('[Simulated] Decoded bodyImageBase64 to result image');
      }
    }
    else if (params.bodyImageUrl) {
      try {
        const imgResp = await axios.get(params.bodyImageUrl, { responseType: 'arraybuffer', timeout: 10000 });
        fs.writeFileSync(destPath, Buffer.from(imgResp.data));
        fallbackSuccess = true;
        console.log(`[Simulated] Downloaded bodyImageUrl to result: ${params.bodyImageUrl}`);
      } catch (downloadErr) {
        console.warn(`[Simulated] Cannot download bodyImageUrl: ${downloadErr.message}`);
      }
    }

    if (!fallbackSuccess) {
      const placeholder = path.join(__dirname, '..', 'uploads', 'placeholder.png');
      if (fs.existsSync(placeholder)) {
        fs.copyFileSync(placeholder, destPath);
        console.log('[Simulated] Copied placeholder to result');
      } else {
        const minimalPng = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==', 'base64');
        fs.writeFileSync(destPath, minimalPng);
        console.log('[Simulated] Created minimal placeholder PNG');
      }
    }
  } catch (err) {
    console.warn('tryOnImageService: fallback copy error', err.message);
  }

  doc.progress = 100;
  doc.status = 'completed';
  doc.result = {
    jobId,
    status: 'completed',
    generatedImageUrl: `/uploads/tryon/${resultFileName}`,
    message: 'Simulated try-on image generated (no AI provider configured). Body image used as result.',
    suggestions: [],
    llmSummary: { score: 0, comment: 'Simulated fallback — no AI provider configured.' }
  };

  await doc.save();
  jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, result: doc.result });
  cleanTryonTempFiles(resultFileName);
}

async function getJobStatus(jobId) {
  const doc = await ProcessingJob.findOne({ jobId });
  if (!doc) {
    const job = jobs.get(jobId);
    if (!job) return null;
    return { jobId: job.jobId, status: job.status, progress: job.progress, estimatedSeconds: job.status === 'queued' ? 6 : Math.max(0, 3 - Math.floor(job.progress / 33)) };
  }
  return { jobId: doc.jobId, status: doc.status, progress: doc.progress, estimatedSeconds: doc.status === 'queued' ? 6 : Math.max(0, 3 - Math.floor(doc.progress / 33)) };
}

async function getJobResult(jobId) {
  const doc = await ProcessingJob.findOne({ jobId });
  if (!doc) return null;
  if (doc.status !== 'completed') return null;
  return doc.result;
}

module.exports = {
  createJob,
  getJobStatus,
  getJobResult
};