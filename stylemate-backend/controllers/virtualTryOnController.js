const tryOnImageService = require('../services/tryOnImageService');
const llmClient = require('../services/llmClient');
const asyncHandler = require('../middleware/asyncHandler');

/**
 * POST /api/ai-stylist/virtual-tryon
 * Accepts multipart or JSON. Returns jobId immediately.
 */
const kickoffTryOn = asyncHandler(async (req, res, next) => {
  const userId = req.body.userId || null;

  // Files handled by multer in route
  const bodyImagePath = req.files?.bodyImage?.[0]?.path || null;
  const itemImagePaths = (req.files?.itemImages || []).map(f => f.path);
  const bodyImageUrl = req.body.bodyImageUrl || null;
  const bodyImageBase64 = req.body.bodyImageBase64 || null;
  const itemImageBase64 = req.body.itemImageBase64 ? (typeof req.body.itemImageBase64 === 'string' ? JSON.parse(req.body.itemImageBase64) : req.body.itemImageBase64) : null;
  const selectedItemIds = req.body.selectedItemIds ? (Array.isArray(req.body.selectedItemIds) ? req.body.selectedItemIds : JSON.parse(req.body.selectedItemIds)) : [];
  const options = req.body.options ? (typeof req.body.options === 'string' ? JSON.parse(req.body.options) : req.body.options) : {};

  const jobMeta = await tryOnImageService.createJob({ userId, bodyImagePath, bodyImageUrl, bodyImageBase64, selectedItemIds, itemImagePaths, itemImageBase64, options });

  res.status(202).json({ success: true, ...jobMeta });
});

/**
 * GET /api/ai-stylist/virtual-tryon/:jobId/status
 */
const getStatus = asyncHandler(async (req, res) => {
  const { jobId } = req.params;
  const status = await tryOnImageService.getJobStatus(jobId);
  if (!status) return res.status(404).json({ success: false, message: 'Job not found' });
  return res.status(200).json({ success: true, ...status });
});

/**
 * GET /api/ai-stylist/virtual-tryon/:jobId/result
 */
const getResult = asyncHandler(async (req, res) => {
  const { jobId } = req.params;
  const result = await tryOnImageService.getJobResult(jobId);
  if (!result) return res.status(202).json({ success: false, message: 'Result not ready', status: await tryOnImageService.getJobStatus(jobId) });
  return res.status(200).json({ success: true, ...result });
});

/**
 * POST /api/ai-stylist/virtual-tryon/:jobId/followup
 * Send follow-up question referencing the try-on result.
 */
const postFollowup = asyncHandler(async (req, res) => {
  const { jobId } = req.params;
  const question = req.body.question;
  const userId = req.body.userId || null;

  if (!question) return res.status(400).json({ success: false, message: 'Missing question' });

  const jobResult = await tryOnImageService.getJobResult(jobId);
  if (!jobResult) return res.status(404).json({ success: false, message: 'Job result not found or not completed' });

  const context = {
    job: jobResult
  };

  const llmResp = await llmClient.generateChatResponse({ userId, message: question, context });
  return res.status(200).json({ success: true, ...llmResp });
});

module.exports = {
  kickoffTryOn,
  getStatus,
  getResult,
  postFollowup
};
