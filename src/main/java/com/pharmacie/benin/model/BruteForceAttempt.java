package com.pharmacie.benin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * BruteForceAttempt model for tracking failed login attempts in memory.
 * This is a POJO (not a JPA entity) used for brute force protection.
 * 
 * Attempts are stored in a cache with the following characteristics:
 * - IpAddress: Primary key for tracking attempts by source
 * - AttemptCount: Number of failed login attempts
 * - BlockedUntil: Timestamp when the block expires
 * - Expiration: 15 minutes after last failed attempt
 * 
 * Validates Requirements: 4.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BruteForceAttempt {
    
    /**
     * IP address of the source making login attempts.
     * Used as the primary key for tracking attempts.
     */
    private String ipAddress;
    
    /**
     * Number of failed login attempts from this IP address.
     * Reset to 0 after successful login or after expiration.
     */
    private int attemptCount;
    
    /**
     * Timestamp of the first failed attempt in the current sequence.
     * Used to determine if attempts are within the 5-minute window.
     */
    private LocalDateTime firstAttemptAt;
    
    /**
     * Timestamp of the most recent failed attempt.
     * Used to calculate expiration time.
     */
    private LocalDateTime lastAttemptAt;
    
    /**
     * Timestamp when the IP address will be unblocked.
     * Set to lastAttemptAt + 15 minutes after 3 failed attempts.
     * Null if the IP is not currently blocked.
     */
    private LocalDateTime blockedUntil;
    
    /**
     * Checks if the IP address is currently blocked.
     * 
     * @return true if blockedUntil is set and is in the future, false otherwise
     */
    public boolean isBlocked() {
        return blockedUntil != null && LocalDateTime.now().isBefore(blockedUntil);
    }
    
    /**
     * Checks if the attempt tracking has expired (15 minutes since last attempt).
     * 
     * @return true if more than 15 minutes have passed since lastAttemptAt, false otherwise
     */
    public boolean isExpired() {
        if (lastAttemptAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lastAttemptAt.plusMinutes(15));
    }
    
    /**
     * Increments the attempt count and updates the lastAttemptAt timestamp.
     * If this is the first attempt, also sets firstAttemptAt.
     * If attempt count reaches 3, sets blockedUntil to 15 minutes from now.
     */
    public void registerFailedAttempt() {
        LocalDateTime now = LocalDateTime.now();
        
        if (attemptCount == 0) {
            this.firstAttemptAt = now;
        }
        
        this.attemptCount++;
        this.lastAttemptAt = now;
        
        // Block after 3 failed attempts
        if (attemptCount >= 3) {
            this.blockedUntil = now.plusMinutes(15);
        }
    }
    
    /**
     * Resets the attempt tracking for this IP address.
     * Called after a successful login or when the tracking expires.
     */
    public void reset() {
        this.attemptCount = 0;
        this.firstAttemptAt = null;
        this.lastAttemptAt = null;
        this.blockedUntil = null;
    }
}
