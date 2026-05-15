package com.pharmacie.benin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Session model for managing user sessions in memory.
 * This is a POJO (not a JPA entity) used for session management.
 * 
 * Sessions are stored in a ConcurrentHashMap with the following characteristics:
 * - Token: UUID v4 (128 bits minimum entropy)
 * - ExpiresAt: createdAt + 8 hours
 * - LastAccessedAt: updated on each request
 * 
 * Validates Requirements: 4.6, 4.7, 5.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    
    /**
     * Unique session token (UUID v4 format).
     * Provides at least 128 bits of entropy for security.
     */
    private String token;
    
    /**
     * ID of the authenticated user.
     */
    private Long userId;
    
    /**
     * Username of the authenticated user.
     */
    private String username;
    
    /**
     * Timestamp when the session was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp of the last access to the session.
     * Updated on each request to track activity.
     */
    private LocalDateTime lastAccessedAt;
    
    /**
     * Timestamp when the session expires.
     * Set to createdAt + 8 hours.
     */
    private LocalDateTime expiresAt;
    
    /**
     * Checks if the session is expired.
     * 
     * @return true if the current time is after expiresAt, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Updates the lastAccessedAt timestamp to the current time.
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
