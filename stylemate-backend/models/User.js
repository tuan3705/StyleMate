/**
 * User model — stores lightweight profile, preferences and measurements
 */
const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  userId: { type: String, required: true, index: true, unique: true }, // client-provided id
  name: { type: String },
  email: { type: String },
  preferences: { type: mongoose.Schema.Types.Mixed }, // e.g., style preferences
  colorProfile: { type: mongoose.Schema.Types.Mixed },
  sizes: { type: mongoose.Schema.Types.Mixed },
  createdAt: { type: Number, default: () => Date.now() },
  updatedAt: { type: Number, default: () => Date.now() }
}, { timestamps: false });

userSchema.pre('findOneAndUpdate', function (next) {
  this.set({ updatedAt: Date.now() });
  next();
});

module.exports = mongoose.model('User', userSchema);
