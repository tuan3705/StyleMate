const fs = require('fs');
const path = require('path');
const axios = require('axios');
const jwt = require('jsonwebtoken');
const ProcessingJob = require('../models/ProcessingJob');

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
  // Persist job into MongoDB ProcessingJob collection so it survives restarts
  const jobId = `tryon_${Date.now().toString(36)}_${Math.random().toString(36).slice(2,8)}`;
  const doc = new ProcessingJob({
    jobId,
    type: 'tryon',
    userId: userId || null,
    status: 'queued',
    progress: 0,
    params: { bodyImagePath: bodyImagePath || null, bodyImageUrl: bodyImageUrl || null, selectedItemIds, itemImagePaths, options },
    result: null,
    // Default expiry for try-on artifacts
    expiresAt: new Date(Date.now() + Number(process.env.TEMP_ASSET_TTL_MS || 24 * 3600 * 1000))
  });

  // Save and schedule processing
  return doc.save().then(saved => {
    // Kick off processing asynchronously
    process.nextTick(() => processJob(saved.jobId));
    // Keep a short-lived in-memory reference for fast reads during this process
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
  // Load from DB (canonical source)
  let doc = await ProcessingJob.findOne({ jobId });
  if (!doc) return;
  // Check if cancelled
  if (doc.status === 'cancelled') return;

  doc.status = 'processing';
  doc.progress = 0;
  await doc.save();

  // Keep in-memory mirror for quick reads
  jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, params: doc.params });

  // Decide provider: explicit option > env TRYON_PROVIDER > auto-detect
  const providedProvider = (doc.params.options && doc.params.options.provider) || process.env.TRYON_PROVIDER || 'AUTO';
  let provider = providedProvider;
  if (provider === 'AUTO') {
    if (KLAI_ACCESS_KEY && KLAI_SECRET_KEY) provider = 'KLAI';
    else if (process.env.STABLE_API_URL) provider = 'STABLE';
    else provider = 'SIMULATED';
  }

  // If KlingAI configured, call it; otherwise simulate
  if (provider === 'KLAI' && KLAI_ACCESS_KEY && KLAI_SECRET_KEY) {
    try {
      // Build base64s
      const params = doc.params || {};
      const humanB64 = params.bodyImageBase64 || (params.bodyImagePath ? await fileToBase64DataUri(params.bodyImagePath) : (params.bodyImageUrl ? await urlToBase64DataUri(params.bodyImageUrl) : null));
      const clothB64 = (params.itemImageBase64 && params.itemImageBase64[0]) || (params.itemImagePaths && params.itemImagePaths[0]) ? (params.itemImageBase64 && params.itemImageBase64[0]) || await fileToBase64DataUri(params.itemImagePaths[0]) : (params.itemImageUrls && params.itemImageUrls[0] ? await urlToBase64DataUri(params.itemImageUrls[0]) : null);

      if (!humanB64 || !clothB64) {
        throw new Error('Missing images for KlingAI try-on');
      }

      doc.progress = 10; await doc.save(); jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, params: doc.params });

      const taskId = await createKlingATask(humanB64, clothB64);
      doc.progress = 20; await doc.save(); jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, params: doc.params });

      // Poll
      let attempts = 0;
      while (attempts < KLAI_MAX_POLL) {
        const data = await queryKlingATask(taskId);
        const status = data.data?.task_status;
        if (status === 'succeed') {
          const imageUrl = data.data?.task_result?.images?.[0]?.url;
          if (imageUrl) {
            // download image
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

      // timeout
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
  // If provider is STABLE and a stable endpoint is configured, call it
  if (provider === 'STABLE' && process.env.STABLE_API_URL) {
    try {
      const params = doc.params || {};
      const humanB64 = params.bodyImageBase64 || (params.bodyImagePath ? await fileToBase64DataUri(params.bodyImagePath) : (params.bodyImageUrl ? await urlToBase64DataUri(params.bodyImageUrl) : null));
      const clothB64 = (params.itemImageBase64 && params.itemImageBase64[0]) || (params.itemImagePaths && params.itemImagePaths[0]) ? (params.itemImageBase64 && params.itemImageBase64[0]) || await fileToBase64DataUri(params.itemImagePaths[0]) : (params.itemImageUrls && params.itemImageUrls[0] ? await urlToBase64DataUri(params.itemImageUrls[0]) : null);

      if (!humanB64 || !clothB64) {
        throw new Error('Missing images for Stable provider try-on');
      }

      doc.progress = 10; await doc.save(); jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress });

      // Call Stable provider (provider X) - generic scaffold
      const stableUrl = process.env.STABLE_API_URL.replace(/\/$/, '') + (process.env.STABLE_TRYON_PATH || '/v1/tryon');
      const stableBody = { model: process.env.STABLE_MODEL || 'sd-tryon-v1', human_image: humanB64, cloth_image: clothB64, options: doc.params.options || {} };
      const stableHeaders = { 'Content-Type': 'application/json' };
      if (process.env.STABLE_API_KEY) stableHeaders['Authorization'] = `Bearer ${process.env.STABLE_API_KEY}`;

      const resp = await axios.post(stableUrl, stableBody, { headers: stableHeaders, timeout: 60000 });

      // Accept either image_base64 or URL in response
      const imageB64 = resp.data?.image_base64 || resp.data?.data?.image_base64 || null;
      const imageUrl = resp.data?.url || resp.data?.data?.url || null;

      const resultFileName = `tryon_result_${jobId}.png`;
      const destPath = path.join(UPLOAD_DIR, resultFileName);

      if (imageB64) {
        // remove data URI prefix if present
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

  // Fallback simulation when no provider configured
  for (let p = 5; p <= 95; p += 10) {
    doc.progress = p; await doc.save(); jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress });
    await sleep(400);
  }

  // Simulate generating a result image by copying the bodyImage if available
  const resultFileName = `tryon_result_${jobId}.png`;
  const destPath = path.join(UPLOAD_DIR, resultFileName);
  try {
    const params = doc.params || {};
    if (params.bodyImagePath && fs.existsSync(params.bodyImagePath)) {
      fs.copyFileSync(params.bodyImagePath, destPath);
    } else {
      // try to copy global placeholder if exists
      const placeholder = path.join(__dirname, '..', 'uploads', 'placeholder.png');
      if (fs.existsSync(placeholder)) fs.copyFileSync(placeholder, destPath);
      else fs.writeFileSync(destPath, '');
    }
  } catch (err) {
    console.warn('tryOnImageService: copy error', err.message);
  }

  doc.progress = 100;
  doc.status = 'completed';
  doc.result = {
    jobId,
    status: 'completed',
    generatedImageUrl: `/uploads/tryon/${resultFileName}`,
    message: 'Simulated try-on image generated.',
    suggestions: [
      { type: 'accessory', text: 'Add a slim brown belt', relatedItemIds: [] }
    ],
    llmSummary: { score: 7.0, comment: 'Simulated result — replace with real pipeline.' }
  };

  await doc.save();
  jobs.set(jobId, { jobId: doc.jobId, status: doc.status, progress: doc.progress, result: doc.result });
}

async function getJobStatus(jobId) {
  // Prefer DB canonical record
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
