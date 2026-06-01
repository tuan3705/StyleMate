/**
 * services/chatSessionService.js
 * 
 * In-memory session management for AI Stylist chat.
 * Sessions are stored in memory with TTL and auto-cleanup.
 */

const crypto = require('crypto');

class ChatSessionService {
  constructor(ttlMs = 30 * 60 * 1000) {
    this.sessions = new Map();
    this.ttlMs = ttlMs;
    this.cleanupInterval = setInterval(() => this.cleanup(), 5 * 60 * 1000); // Cleanup every 5 min
  }

  /**
   * Create a new chat session
   * @param {string} userId - User ID
   * @param {object} context - Initial context (weather, closet summary, etc.)
   * @returns {object} Session object with sessionId
   */
  createSession(userId, context = {}) {
    const sessionId = `session_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
    
    const session = {
      sessionId,
      userId,
      createdAt: new Date(),
      lastAccessedAt: new Date(),
      expiresAt: new Date(Date.now() + this.ttlMs),
      messages: [],
      context: {
        weather: context.weather || null,
        location: context.location || null,
        userProfile: context.userProfile || null,
        selectedItems: context.selectedItems || [],
        occasion: context.occasion || null,
        ...context
      },
      suggestedOutfits: [],
      selectedOutfit: null
    };

    this.sessions.set(sessionId, session);
    return session;
  }

  /**
   * Get a session by ID
   */
  getSession(sessionId) {
    const session = this.sessions.get(sessionId);
    if (!session) return null;
    
    // Check if expired
    if (new Date() > session.expiresAt) {
      this.sessions.delete(sessionId);
      return null;
    }

    // Update last accessed time
    session.lastAccessedAt = new Date();
    session.expiresAt = new Date(Date.now() + this.ttlMs);
    return session;
  }

  /**
   * Add a user message to session
   */
  addUserMessage(sessionId, message, metadata = {}) {
    const session = this.getSession(sessionId);
    if (!session) return null;

    const userMsg = {
      role: 'user',
      content: message,
      timestamp: new Date(),
      metadata: metadata
    };

    session.messages.push(userMsg);
    return session;
  }

  /**
   * Add an assistant message to session
   */
  addAssistantMessage(sessionId, response, metadata = {}) {
    const session = this.getSession(sessionId);
    if (!session) return null;

    const assistantMsg = {
      role: 'assistant',
      content: response,
      timestamp: new Date(),
      metadata: metadata
    };

    session.messages.push(assistantMsg);
    
    // Also store suggested outfits if provided
    if (metadata.suggestedOutfits && Array.isArray(metadata.suggestedOutfits)) {
      session.suggestedOutfits.push({
        outfits: metadata.suggestedOutfits,
        timestamp: new Date(),
        messageIndex: session.messages.length - 1
      });
    }

    return session;
  }

  /**
   * Update session context
   */
  updateContext(sessionId, contextUpdates) {
    const session = this.getSession(sessionId);
    if (!session) return null;

    session.context = {
      ...session.context,
      ...contextUpdates
    };

    return session;
  }

  /**
   * Get chat history for a session
   */
  getChatHistory(sessionId) {
    const session = this.getSession(sessionId);
    if (!session) return null;

    return {
      sessionId,
      messages: session.messages,
      context: session.context,
      suggestedOutfits: session.suggestedOutfits
    };
  }

  /**
   * Delete a session
   */
  deleteSession(sessionId) {
    return this.sessions.delete(sessionId);
  }

  /**
   * Cleanup expired sessions
   */
  cleanup() {
    const now = new Date();
    let cleanedCount = 0;

    for (const [sessionId, session] of this.sessions.entries()) {
      if (now > session.expiresAt) {
        this.sessions.delete(sessionId);
        cleanedCount++;
      }
    }

    if (cleanedCount > 0) {
      console.log(`[ChatSessionService] Cleaned up ${cleanedCount} expired sessions`);
    }
  }

  /**
   * Get session stats (for monitoring)
   */
  getStats() {
    return {
      activeSessions: this.sessions.size,
      sessionIds: Array.from(this.sessions.keys()),
      ttlMs: this.ttlMs
    };
  }

  /**
   * Destroy service (cleanup interval)
   */
  destroy() {
    clearInterval(this.cleanupInterval);
  }
}

// Create singleton instance
const chatSessionService = new ChatSessionService();

module.exports = chatSessionService;
