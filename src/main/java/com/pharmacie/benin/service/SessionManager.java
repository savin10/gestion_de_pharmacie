package com.pharmacie.benin.service;

import com.pharmacie.benin.model.AdminUser;
import com.pharmacie.benin.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager component for managing user sessions in memory.
 * 
 * This component provides session management functionality including:
 * - Creating sessions with UUID v4 tokens (128 bits entropy minimum)
 * - Retrieving sessions from in-memory storage
 * - Invalidating sessions
 * - Scheduled cleanup of expired sessions every hour
 * 
 * Sessions are stored in a ConcurrentHashMap for thread-safe access.
 * Each session expires 8 hours after creation.
 * 
 * Validates Requirements: 4.6, 4.7, 5.1, 5.5
 */
@Component
public class SessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    
    /**
     * Session expiration duration in hours.
     */
    private static final int SESSION_EXPIRATION_HOURS = 8;
    
    /**
     * Thread-safe in-memory storage for sessions.
     * Key: session token (UUID v4)
     * Value: Session object
     */
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    
    /**
     * Creates a new session for the given user.
     * 
     * Generates a UUID v4 token with at least 128 bits of entropy.
     * Sets session expiration to 8 hours from creation time.
     * Stores the session in the ConcurrentHashMap.
     * 
     * @param user the authenticated admin user
     * @return the generated session token (UUID v4 format)
     * @throws IllegalArgumentException if user is null or user ID is null
     */
    public String createSession(AdminUser user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Generate UUID v4 token (128 bits of entropy)
        String token = UUID.randomUUID().toString();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(SESSION_EXPIRATION_HOURS);
        
        Session session = new Session(
            token,
            user.getId(),
            user.getUsername(),
            now,
            now,
            expiresAt
        );
        
        sessions.put(token, session);
        
        logger.info("Created session for user: {} (ID: {}), expires at: {}", 
                    user.getUsername(), user.getId(), expiresAt);
        
        return token;
    }
    
    /**
     * Retrieves a session by its token.
     * 
     * Performs a ConcurrentHashMap lookup for the given token.
     * If the session exists and is not expired, updates the lastAccessedAt timestamp.
     * If the session is expired, removes it from storage and returns empty.
     * 
     * @param token the session token to look up
     * @return Optional containing the Session if found and valid, empty otherwise
     */
    public Optional<Session> getSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        
        Session session = sessions.get(token);
        
        if (session == null) {
            logger.debug("Session not found for token: {}", token);
            return Optional.empty();
        }
        
        // Check if session is expired
        if (session.isExpired()) {
            logger.info("Session expired for user: {} (ID: {})", 
                       session.getUsername(), session.getUserId());
            sessions.remove(token);
            return Optional.empty();
        }
        
        // Update last accessed timestamp
        session.updateLastAccessed();
        
        logger.debug("Retrieved valid session for user: {} (ID: {})", 
                    session.getUsername(), session.getUserId());
        
        return Optional.of(session);
    }
    
    /**
     * Invalidates a session by removing it from storage.
     * 
     * This method is called during logout to immediately invalidate the session.
     * 
     * @param token the session token to invalidate
     */
    public void invalidateSession(String token) {
        if (token == null || token.isBlank()) {
            logger.warn("Attempted to invalidate null or blank token");
            return;
        }
        
        Session removedSession = sessions.remove(token);
        
        if (removedSession != null) {
            logger.info("Invalidated session for user: {} (ID: {})", 
                       removedSession.getUsername(), removedSession.getUserId());
        } else {
            logger.debug("Attempted to invalidate non-existent session: {}", token);
        }
    }
    
    /**
     * Scheduled task to clean expired sessions every hour.
     * 
     * This method runs automatically every hour (3600000 milliseconds) to remove
     * expired sessions from memory, preventing memory leaks.
     * 
     * The cleanup process:
     * 1. Iterates through all sessions
     * 2. Checks if each session is expired
     * 3. Removes expired sessions from the ConcurrentHashMap
     * 4. Logs the number of sessions cleaned
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms = 1 hour)
    public void cleanExpiredSessions() {
        logger.info("Starting scheduled cleanup of expired sessions");
        
        int initialSize = sessions.size();
        int removedCount = 0;
        
        // Iterate through all sessions and remove expired ones
        sessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            if (session.isExpired()) {
                logger.debug("Removing expired session for user: {} (ID: {})", 
                           session.getUsername(), session.getUserId());
                return true;
            }
            return false;
        });
        
        removedCount = initialSize - sessions.size();
        
        logger.info("Completed session cleanup: removed {} expired sessions, {} active sessions remaining", 
                   removedCount, sessions.size());
    }
    
    /**
     * Gets the current number of active sessions.
     * Useful for monitoring and testing purposes.
     * 
     * @return the number of sessions currently stored
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * Clears all sessions from storage.
     * This method should only be used for testing purposes.
     */
    public void clearAllSessions() {
        int count = sessions.size();
        sessions.clear();
        logger.warn("Cleared all sessions: {} sessions removed", count);
    }
}
