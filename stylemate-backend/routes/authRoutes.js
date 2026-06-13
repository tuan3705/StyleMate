/**
 * 🔐 Auth Routes
 *
 * Base path: /api/auth
 */
const express = require('express');
const router = express.Router();
const { register, login, refresh, logout, changePassword } = require('../controllers/authController');
const { requireAuth } = require('../middleware/authMiddleware');

// POST /api/auth/register
router.post('/register', register);

// POST /api/auth/login
router.post('/login', login);

// POST /api/auth/refresh
router.post('/refresh', refresh);

// POST /api/auth/logout
router.post('/logout', requireAuth, logout);

// POST /api/auth/change-password
router.post('/change-password', requireAuth, changePassword);

module.exports = router;
