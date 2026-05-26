/**
 * models/ProcessingJob.js
 * Generic processing job stored in MongoDB for async pipelines.
 */
const mongoose = require('mongoose');

const processingJobSchema = new mongoose.Schema({
  jobId: { type: String, required: true, unique: true },
  type: { type: String, required: true },
  userId: { type: String },
  status: { type: String, enum: ['queued', 'processing', 'completed', 'failed', 'cancelled'], default: 'queued' },
  progress: { type: Number, default: 0 },
  params: { type: Object, default: {} },
  result: { type: Object, default: {} },
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now },
  expiresAt: { type: Date }
});

// TTL index will remove docs when expiresAt passes
processingJobSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

processingJobSchema.pre('save', function (next) {
  this.updatedAt = Date.now();
  next();
});

module.exports = mongoose.model('ProcessingJob', processingJobSchema);
