/**
 * 🔐 Token Utils
 *
 * Tạo và xác thực JWT cho access/refresh token.
 */
const crypto = require('crypto');
const jwt = require('jsonwebtoken');

const getJwtConfig = () => {
  return {
    accessSecret: process.env.JWT_ACCESS_SECRET || 'dev_access_secret',
    refreshSecret: process.env.JWT_REFRESH_SECRET || 'dev_refresh_secret',
    accessExpiresIn: process.env.JWT_ACCESS_EXPIRES || '15m',
    refreshExpiresIn: process.env.JWT_REFRESH_EXPIRES || '30d'
  };
};

const signAccessToken = (user) => {
  const { accessSecret, accessExpiresIn } = getJwtConfig();
  return jwt.sign({ sub: user._id, tokenVersion: user.tokenVersion }, accessSecret, {
    expiresIn: accessExpiresIn
  });
};

const signRefreshToken = (user) => {
  const { refreshSecret, refreshExpiresIn } = getJwtConfig();
  return jwt.sign({ sub: user._id, tokenVersion: user.tokenVersion }, refreshSecret, {
    expiresIn: refreshExpiresIn
  });
};

const verifyAccessToken = (token) => {
  const { accessSecret } = getJwtConfig();
  return jwt.verify(token, accessSecret);
};

const verifyRefreshToken = (token) => {
  const { refreshSecret } = getJwtConfig();
  return jwt.verify(token, refreshSecret);
};

const hashToken = (token) => {
  return crypto.createHash('sha256').update(token).digest('hex');
};

module.exports = {
  signAccessToken,
  signRefreshToken,
  verifyAccessToken,
  verifyRefreshToken,
  hashToken
};

